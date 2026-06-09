package com.cappsconsulting.prism.sync.pairing

import kotlinx.serialization.Serializable

/**
 * One paired device — Doc 3.0 §3.2 step 4: "Multiple parents, independently
 * revocable. Each parent device that pairs gets its *own* derived key."
 *
 * [keyAlias] is a reference *into* platform secure storage (see [KeyStorage]),
 * never the key material itself — the same boundary Hard Line 3 draws around
 * recognition templates, reused here so raw key bytes never sit in an ordinary
 * object that could be logged, serialized to disk in the open, or swept into a
 * crash report.
 */
@Serializable
data class LinkedDevice(
    val deviceId: String,
    val label: String,
    val keyAlias: String,
    val pairedAtEpochSeconds: Double,
)

/**
 * The "linked devices" list — Doc 3.0 §3.2 step 4: "The Parent Suite shows a plain
 * 'linked devices' list (mirroring Doc 2.2's glass-box instinct — nothing about the
 * *system's own connections* is hidden from the parent either); unlinking one
 * revokes only that key, instantly, on both ends."
 *
 * That last clause is the contract this registry exists to keep provable: [unlink]
 * touches exactly one entry. There is no bulk-revoke, no cascade — independence
 * between parents' links is structural, not a side effect of how the list happens
 * to be stored.
 */
class LinkedDeviceRegistry {
    private val devices = LinkedHashMap<String, LinkedDevice>()

    fun link(device: LinkedDevice) {
        devices[device.deviceId] = device
    }

    /** @return true if a device was actually removed (false if [deviceId] wasn't linked). */
    fun unlink(deviceId: String): Boolean = devices.remove(deviceId) != null

    fun get(deviceId: String): LinkedDevice? = devices[deviceId]

    fun all(): List<LinkedDevice> = devices.values.toList()
}
