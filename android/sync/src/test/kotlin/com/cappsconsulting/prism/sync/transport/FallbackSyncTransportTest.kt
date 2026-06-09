package com.cappsconsulting.prism.sync.transport

import com.cappsconsulting.prism.sync.crypto.SealedEnvelope
import com.cappsconsulting.prism.sync.pairing.LinkedDevice
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [FallbackSyncTransport] is the testable expression of Doc 3.0 §3.3's policy —
 * "local-first... relay fallback" — kept independent of what either transport
 * actually does on the wire (which this sandbox can't run anyway: no Android
 * platform, no live network). A recording fake stands in for each transport so
 * these tests prove *routing*, not networking.
 */
class FallbackSyncTransportTest {

    private class RecordingTransport : SyncTransport {
        val sent = mutableListOf<Pair<LinkedDevice, SealedEnvelope>>()
        var toReceive: SealedEnvelope? = null

        override suspend fun send(to: LinkedDevice, envelope: SealedEnvelope) {
            sent.add(to to envelope)
        }

        override suspend fun receive(): SealedEnvelope? = toReceive
    }

    private fun deviceOf(id: String) =
        LinkedDevice(deviceId = id, label = "label", keyAlias = "alias-$id", pairedAtEpochSeconds = 0.0)

    private fun envelopeOf(tag: Byte) = SealedEnvelope(nonce = byteArrayOf(tag), ciphertext = byteArrayOf(tag, tag))

    @Test
    fun `sends over the peer link when the device is reachable — the local-first case`() = runTest {
        val peerToPeer = RecordingTransport()
        val relay = RecordingTransport()
        val transport = FallbackSyncTransport(peerToPeer, relay, isPeerReachable = { true })
        val device = deviceOf("companion-phone")
        val envelope = envelopeOf(1)

        transport.send(device, envelope)

        assertThat(peerToPeer.sent).containsExactly(device to envelope)
        assertThat(relay.sent).isEmpty()
    }

    @Test
    fun `falls through to the relay when the peer is unreachable — the away-from-home case`() = runTest {
        val peerToPeer = RecordingTransport()
        val relay = RecordingTransport()
        val transport = FallbackSyncTransport(peerToPeer, relay, isPeerReachable = { false })
        val device = deviceOf("companion-phone")
        val envelope = envelopeOf(2)

        transport.send(device, envelope)

        assertThat(relay.sent).containsExactly(device to envelope)
        assertThat(peerToPeer.sent).isEmpty()
    }

    @Test
    fun `reachability is decided per-send, never cached — being home for one message doesn't fix the route`() = runTest {
        val peerToPeer = RecordingTransport()
        val relay = RecordingTransport()
        var homeNow = true
        val transport = FallbackSyncTransport(peerToPeer, relay, isPeerReachable = { homeNow })
        val device = deviceOf("companion-phone")

        transport.send(device, envelopeOf(1)) // at home
        homeNow = false
        transport.send(device, envelopeOf(2)) // parent leaves; same session, same registry

        assertThat(peerToPeer.sent.map { it.second }).containsExactly(envelopeOf(1))
        assertThat(relay.sent.map { it.second }).containsExactly(envelopeOf(2))
    }

    @Test
    fun `receiving prefers whatever the local link already has, falling back only when it is silent`() = runTest {
        val peerToPeer = RecordingTransport()
        val relay = RecordingTransport()
        val transport = FallbackSyncTransport(peerToPeer, relay, isPeerReachable = { true })

        peerToPeer.toReceive = envelopeOf(9)
        relay.toReceive = envelopeOf(8)
        assertThat(transport.receive()).isEqualTo(envelopeOf(9))

        peerToPeer.toReceive = null
        assertThat(transport.receive()).isEqualTo(envelopeOf(8))

        relay.toReceive = null
        assertThat(transport.receive()).isNull()
    }
}
