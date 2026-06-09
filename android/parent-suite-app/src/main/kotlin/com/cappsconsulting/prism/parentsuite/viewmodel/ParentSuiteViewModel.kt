package com.cappsconsulting.prism.parentsuite.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappsconsulting.prism.engine.grounding.GroundingStatus
import com.cappsconsulting.prism.parentsuite.data.ChildProfile
import com.cappsconsulting.prism.parentsuite.data.ConceptRecord
import com.cappsconsulting.prism.parentsuite.data.ConceptTileState
import com.cappsconsulting.prism.parentsuite.data.Pacing
import com.cappsconsulting.prism.sync.pairing.LinkedDevice
import com.cappsconsulting.prism.sync.pairing.LinkedDeviceRegistry
import com.cappsconsulting.prism.sync.pairing.PairingHandshake
import com.cappsconsulting.prism.sync.pairing.PairingToken
import com.cappsconsulting.prism.sync.payload.SessionSummary
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * The Parent Suite's single shared ViewModel — Doc 2.2's full feature surface
 * in one place: Map (concepts, grounding bands, counter-balance, pacing, boundaries),
 * Trajectory (session history), Pairing (QR display, linked devices), and Settings
 * (child profile).
 *
 * No DI framework — passed directly from [com.cappsconsulting.prism.parentsuite.MainActivity]
 * to [com.cappsconsulting.prism.parentsuite.ui.ParentNavHost] and composables as a parameter.
 * No [GroundingStatus] redefinition — uses `GroundingStatus` from `:engine` directly
 * (Hard Line 6: "no visible numeric score anywhere, to anyone" — the shared enum
 * and its wire values are the canonical source of truth for banded language).
 *
 * [seedConcepts] and [seedSessions] populate the Map and Trajectory for a fresh
 * device, illustrating all four [ConceptTileState]s and all three [GroundingStatus]
 * bands so those screens are non-empty and reviewable without a live sync connection.
 * They are demo records, labeled as such in their companion-object factory, not
 * real session data.
 */
class ParentSuiteViewModel : ViewModel() {

    private val _concepts = MutableStateFlow(seedConcepts())
    val concepts: StateFlow<List<ConceptRecord>> = _concepts.asStateFlow()

    private val _recentSessions = MutableStateFlow(seedSessions())
    val recentSessions: StateFlow<List<SessionSummary>> = _recentSessions.asStateFlow()

    private val _activePairingToken = MutableStateFlow<PairingToken?>(null)
    val activePairingToken: StateFlow<PairingToken?> = _activePairingToken.asStateFlow()

    private val _pairingQr = MutableStateFlow<Bitmap?>(null)
    val pairingQr: StateFlow<Bitmap?> = _pairingQr.asStateFlow()

    private val deviceRegistry = LinkedDeviceRegistry()

    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(emptyList())
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    private val _childProfile = MutableStateFlow(ChildProfile())
    val childProfile: StateFlow<ChildProfile> = _childProfile.asStateFlow()

    fun setPacing(conceptId: String, pacing: Pacing) {
        _concepts.update { list ->
            list.map { if (it.id == conceptId) it.copy(pacing = pacing) else it }
        }
    }

    fun setAbsentByParent(conceptId: String, absent: Boolean) {
        _concepts.update { list ->
            list.map { concept ->
                if (concept.id != conceptId) return@map concept
                concept.copy(tileState = if (absent) ConceptTileState.ABSENT_BY_PARENT else ConceptTileState.ACTIVE)
            }
        }
    }

    fun setDormant(conceptId: String, dormant: Boolean) {
        _concepts.update { list ->
            list.map { concept ->
                if (concept.id != conceptId) return@map concept
                // ABSENT_BY_PARENT wins — a hard stop can't be softened to DORMANT
                if (concept.tileState == ConceptTileState.ABSENT_BY_PARENT) return@map concept
                concept.copy(tileState = if (dormant) ConceptTileState.DORMANT else ConceptTileState.ACTIVE)
            }
        }
    }

    fun setCounterBalanceWeight(conceptId: String, weight: Double) {
        _concepts.update { list ->
            list.map { if (it.id == conceptId) it.copy(counterBalanceWeight = weight.coerceIn(0.0, 5.0)) else it }
        }
    }

    /**
     * Generates a fresh [PairingToken] and its QR [Bitmap] — Doc 3.0 §3.2 step 1.
     * Key generation and QR encoding run on [Dispatchers.Default] to keep the main
     * thread free; the resulting bitmap is written to [pairingQr] for the UI to display.
     */
    fun generatePairingToken() {
        viewModelScope.launch {
            val keyPair = withContext(Dispatchers.Default) {
                PairingHandshake.generateEphemeralKeyPair()
            }
            val nowSeconds = System.currentTimeMillis() / 1000.0
            val token = PairingToken.issue(
                tokenId = UUID.randomUUID().toString(),
                publicKey = keyPair.public,
                nowEpochSeconds = nowSeconds,
            )
            _activePairingToken.value = token
            val json = Json.encodeToString(PairingToken.serializer(), token)
            val bitmap = withContext(Dispatchers.Default) {
                BarcodeEncoder().encodeBitmap(json, BarcodeFormat.QR_CODE, 512, 512)
            }
            _pairingQr.value = bitmap
        }
    }

    fun clearPairingToken() {
        _activePairingToken.value = null
        _pairingQr.value = null
    }

    fun unlinkDevice(deviceId: String) {
        deviceRegistry.unlink(deviceId)
        _linkedDevices.value = deviceRegistry.all()
    }

    /**
     * Called when a [SessionSummary] arrives over the sync link. Updates the
     * matching concept's grounding band and timestamps; adds a new concept record
     * (as [ConceptTileState.ACTIVE]) if the concept hasn't been seen before.
     */
    fun onSessionSummaryReceived(summary: SessionSummary) {
        _recentSessions.update { existing ->
            (listOf(summary) + existing).take(50)
        }
        _concepts.update { list ->
            val knownLabels = list.map { it.label }.toSet()
            val updated = list.map { concept ->
                if (concept.label != summary.concept) return@map concept
                concept.copy(
                    groundingStatus = GroundingStatus.fromWireValue(summary.groundingStatus),
                    lastSeenAtEpochSeconds = summary.endedAtEpochSeconds,
                    sessionCount = concept.sessionCount + 1,
                    tileState = if (concept.tileState == ConceptTileState.NOT_YET_REACHED) {
                        ConceptTileState.ACTIVE
                    } else {
                        concept.tileState
                    },
                    firstExposedAtEpochSeconds = if (summary.isNewExposure) {
                        summary.startedAtEpochSeconds
                    } else {
                        concept.firstExposedAtEpochSeconds
                    },
                )
            }
            if (summary.concept !in knownLabels) {
                updated + ConceptRecord(
                    id = UUID.randomUUID().toString(),
                    label = summary.concept,
                    tileState = ConceptTileState.ACTIVE,
                    groundingStatus = GroundingStatus.fromWireValue(summary.groundingStatus),
                    firstExposedAtEpochSeconds = summary.startedAtEpochSeconds,
                    lastSeenAtEpochSeconds = summary.endedAtEpochSeconds,
                    sessionCount = 1,
                )
            } else {
                updated
            }
        }
    }

    fun updateChildProfile(profile: ChildProfile) {
        _childProfile.value = profile
    }

    companion object {
        // Demo seed records — illustrate all four ConceptTileStates and all three
        // GroundingStatus bands so Map and Trajectory are non-empty on first launch.
        private fun seedConcepts(): List<ConceptRecord> = listOf(
            ConceptRecord(
                id = "seed-photosynthesis",
                label = "Photosynthesis",
                tileState = ConceptTileState.ACTIVE,
                groundingStatus = GroundingStatus.GETTING_IT,
                pacing = Pacing.BALANCED,
                counterBalanceWeight = 1.5,
                firstExposedAtEpochSeconds = 1_700_000_000.0,
                lastSeenAtEpochSeconds = 1_700_100_000.0,
                sessionCount = 4,
            ),
            ConceptRecord(
                id = "seed-volcanoes",
                label = "Volcanoes",
                tileState = ConceptTileState.ACTIVE,
                groundingStatus = GroundingStatus.OWNS_IT,
                pacing = Pacing.FAST,
                counterBalanceWeight = 0.0,
                firstExposedAtEpochSeconds = 1_699_000_000.0,
                lastSeenAtEpochSeconds = 1_700_050_000.0,
                sessionCount = 7,
            ),
            ConceptRecord(
                id = "seed-ocean-animals",
                label = "Ocean Animals",
                tileState = ConceptTileState.ACTIVE,
                groundingStatus = GroundingStatus.EXPLORING,
                pacing = Pacing.SLOW,
                counterBalanceWeight = 2.0,
                firstExposedAtEpochSeconds = 1_700_080_000.0,
                lastSeenAtEpochSeconds = 1_700_090_000.0,
                sessionCount = 1,
            ),
            ConceptRecord(
                id = "seed-dinosaurs",
                label = "Dinosaurs",
                tileState = ConceptTileState.DORMANT,
                groundingStatus = GroundingStatus.GETTING_IT,
                pacing = Pacing.BALANCED,
                counterBalanceWeight = 0.0,
                firstExposedAtEpochSeconds = 1_698_000_000.0,
                lastSeenAtEpochSeconds = 1_699_500_000.0,
                sessionCount = 5,
            ),
            ConceptRecord(
                id = "seed-rainforest",
                label = "Rainforest",
                tileState = ConceptTileState.ABSENT_BY_PARENT,
                groundingStatus = GroundingStatus.EXPLORING,
                sessionCount = 0,
            ),
            ConceptRecord(
                id = "seed-counting",
                label = "Counting & Numbers",
                tileState = ConceptTileState.NOT_YET_REACHED,
                sessionCount = 0,
            ),
        )

        // Demo seed sessions matching the seed concepts above.
        private fun seedSessions(): List<SessionSummary> = listOf(
            SessionSummary(
                sessionId = "seed-session-1",
                startedAtEpochSeconds = 1_700_100_000.0,
                endedAtEpochSeconds = 1_700_100_900.0,
                concept = "Photosynthesis",
                groundingStatus = GroundingStatus.GETTING_IT.wireValue,
                isNewExposure = false,
                isReappearance = true,
                companionMoodBand = "curious",
            ),
            SessionSummary(
                sessionId = "seed-session-2",
                startedAtEpochSeconds = 1_700_050_000.0,
                endedAtEpochSeconds = 1_700_050_600.0,
                concept = "Volcanoes",
                groundingStatus = GroundingStatus.OWNS_IT.wireValue,
                isNewExposure = false,
                isReappearance = false,
                companionMoodBand = "delighted",
            ),
            SessionSummary(
                sessionId = "seed-session-3",
                startedAtEpochSeconds = 1_700_090_000.0,
                endedAtEpochSeconds = 1_700_090_480.0,
                concept = "Ocean Animals",
                groundingStatus = GroundingStatus.EXPLORING.wireValue,
                isNewExposure = true,
                isReappearance = false,
                companionMoodBand = "playful",
            ),
        )
    }
}
