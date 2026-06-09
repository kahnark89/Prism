package com.cappsconsulting.prism.companion.orchestrator

import android.app.Application
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.cappsconsulting.prism.companion.awakening.AwakeningChoreographer
import com.cappsconsulting.prism.companion.awakening.AwakeningMachine
import com.cappsconsulting.prism.companion.data.RecognitionDatabase
import com.cappsconsulting.prism.companion.hal.CompanionHal
import com.cappsconsulting.prism.companion.hal.android.AndroidHaptics
import com.cappsconsulting.prism.companion.hal.android.AndroidMicrophone
import com.cappsconsulting.prism.companion.hal.android.AndroidSpeaker
import com.cappsconsulting.prism.companion.hal.android.CameraXSource
import com.cappsconsulting.prism.companion.llm.AnthropicLlmClient
import com.cappsconsulting.prism.companion.llm.ApiKeyStore
import com.cappsconsulting.prism.companion.recognition.MlKitRecognitionEngine
import com.cappsconsulting.prism.companion.vision.TfliteVisionClassifier
import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.grounding.GroundingAccumulator
import com.cappsconsulting.prism.engine.innerlife.InnerLifeEngine
import com.cappsconsulting.prism.engine.learninglog.LearningLog
import com.cappsconsulting.prism.engine.memory.MemoryEngine
import com.cappsconsulting.prism.engine.moodline.MoodLineCompiler
import com.cappsconsulting.prism.engine.personas.Personas
import com.cappsconsulting.prism.engine.perspective.PerspectiveEngine
import com.cappsconsulting.prism.engine.safety.SafetyGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "CompanionViewModel"

/**
 * Wires the complete Companion session — the assembly layer that [MainActivity]
 * calls into once, so the orchestrator's dependency graph is constructed exactly
 * once per app process and survives configuration changes (portrait-lock + the
 * `configChanges` attribute in the manifest mean this ViewModel is never recreated
 * mid-session, but the ViewModel lifecycle guarantees it anyway).
 *
 * No DI framework — every dependency is constructed here, inline. The ordering
 * follows the dependency graph: engine-level objects first (no Android dependencies),
 * HAL second (need [Context] and [LifecycleOwner]), orchestrator last (needs both).
 *
 * [initialize] is called from [MainActivity.onCreate] before `setContent` — it's
 * synchronous up to `viewModelScope.launch`, so [orchestrator] and [cameraPreviewView]
 * are non-null by the time Compose first renders, and the re-entrancy guard prevents
 * double-initialization on activity restarts.
 *
 * What's wired:
 * - [TfliteVisionClassifier]: reads `assets/mobilenet_v1.tflite` if present; falls back
 *   to [MockVisionClassifier] gracefully when model asset hasn't been bundled yet.
 * - [MlKitRecognitionEngine]: ML Kit face detection + pixel-similarity template matching
 *   in `recognition.db`. Upgrade path to TFLite embedding model documented in that class.
 * - [AnthropicLlmClient]: HTTP POST to `/v1/messages`; API key from [ApiKeyStore]
 *   (SharedPreferences). Throws if no key configured → [PerspectiveEngine] falls back
 *   to offline templates.
 */
class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    private var _orchestrator: CompanionOrchestrator? = null
    val orchestrator: CompanionOrchestrator? get() = _orchestrator

    private var _cameraSource: CameraXSource? = null
    val cameraPreviewView: PreviewView? get() = _cameraSource?.previewView

    private var _hal: CompanionHal? = null
    private var _recognitionEngine: MlKitRecognitionEngine? = null
    private var _config: PrismConfig? = null

    private val _apiKeyStore = ApiKeyStore(application)
    val apiKeyStore: ApiKeyStore get() = _apiKeyStore

    /** Child name from config — shown in enrollment and other parent-facing UI. */
    val childName: String get() = _config?.childName ?: "your child"

    /** Whether the child has been enrolled — delegates to the recognition engine's in-memory cache. */
    fun isEnrolled(): Boolean = _recognitionEngine?.isEnrolled() ?: false

    /**
     * Captures [frameCount] frames from the running camera and passes them to
     * [MlKitRecognitionEngine.enroll]. Returns true if at least one frame contained a
     * detectable face and the template was stored. Called from [CompanionEnrollmentScreen].
     */
    suspend fun enrollChild(frameCount: Int = 5): Boolean {
        val hal = _hal ?: return false
        val engine = _recognitionEngine ?: return false
        val frames = (1..frameCount).map { hal.camera.captureFrame() }
        return engine.enroll(frames)
    }

    fun initialize(lifecycleOwner: LifecycleOwner) {
        if (_orchestrator != null) return

        val app = getApplication<Application>()
        val config = PrismConfig()
        _config = config

        // Recognition database — separate from the main session DB per Hard Line 3.
        val recognitionDb = RecognitionDatabase.getDatabase(app)
        val recognitionEngine = MlKitRecognitionEngine(recognitionDb.faceTemplateDao())
        _recognitionEngine = recognitionEngine

        // HAL — Android implementations of the four hardware interfaces
        val cameraSource = CameraXSource(app, lifecycleOwner)
        _cameraSource = cameraSource
        val hal = CompanionHal(
            camera = cameraSource,
            microphone = AndroidMicrophone(),
            speaker = AndroidSpeaker(app),
            haptics = AndroidHaptics(app),
        )
        _hal = hal

        // Engine layer — pure Kotlin, no Android deps
        val persona = Personas.PIP
        val innerLife = InnerLifeEngine(
            config = config,
            baseM = persona.baseM,
            baseE = persona.baseE,
            baseC = persona.baseC,
            baseA = persona.baseA,
            baseS = persona.baseS,
        )
        val memory = MemoryEngine(config)
        val moodLine = MoodLineCompiler(config)
        val grounding = GroundingAccumulator(config)
        val safety = SafetyGate(config)
        val learningLog = LearningLog(grounding)
        val perspective = PerspectiveEngine(
            config = config,
            safety = safety,
            llmClient = AnthropicLlmClient(_apiKeyStore),
        )

        // Awakening layer
        val awakening = AwakeningMachine()
        val choreographer = AwakeningChoreographer(hal.speaker, hal.haptics)

        // Orchestrator
        val orchestrator = CompanionOrchestrator(
            config = config,
            hal = hal,
            innerLife = innerLife,
            memory = memory,
            moodLine = moodLine,
            grounding = grounding,
            safety = safety,
            learningLog = learningLog,
            perspective = perspective,
            vision = TfliteVisionClassifier(app),
            recognition = recognitionEngine,
            awakening = awakening,
            choreographer = choreographer,
            scope = viewModelScope,
        )
        _orchestrator = orchestrator

        // Start the session — startAll() binds the camera to the lifecycle, then
        // run() enters the four background loops (inner life tick, memory decay,
        // save, idle watch). The coroutine completes only on shutdown or cancellation.
        viewModelScope.launch {
            try {
                hal.startAll()
                orchestrator.run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Session failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                _orchestrator?.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Shutdown failed", e)
            }
        }
    }
}
