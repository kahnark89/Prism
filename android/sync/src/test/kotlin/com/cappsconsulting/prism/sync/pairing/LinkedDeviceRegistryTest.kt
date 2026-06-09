package com.cappsconsulting.prism.sync.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Doc 3.0 §3.2 step 4 names two contracts for the "linked devices" list this
 * registry backs: it must be a *plain, visible* list (the glass-box instinct
 * extended to the system's own connections), and unlinking one device must
 * "revoke[] only that key, instantly, on both ends" — independence between
 * parents, not a shared fate.
 */
class LinkedDeviceRegistryTest {

    private fun deviceOf(id: String, alias: String = "alias-$id") =
        LinkedDevice(deviceId = id, label = "Naomi's mom's phone", keyAlias = alias, pairedAtEpochSeconds = 1_000.0)

    @Test
    fun `linking makes a device visible in the plain list and individually retrievable`() {
        val registry = LinkedDeviceRegistry()
        registry.link(deviceOf("device-a"))

        assertThat(registry.all().map { it.deviceId }).containsExactly("device-a")
        assertThat(registry.get("device-a")).isNotNull()
        assertThat(registry.get("device-a")!!.keyAlias).isEqualTo("alias-device-a")
    }

    @Test
    fun `unlinking one device revokes only that one — the others remain linked, untouched`() {
        val registry = LinkedDeviceRegistry()
        registry.link(deviceOf("mom-phone"))
        registry.link(deviceOf("dad-tablet"))
        registry.link(deviceOf("grandma-phone"))

        val removed = registry.unlink("dad-tablet")

        assertThat(removed).isTrue()
        assertThat(registry.get("dad-tablet")).isNull()
        assertThat(registry.all().map { it.deviceId }).containsExactly("mom-phone", "grandma-phone")
    }

    @Test
    fun `unlinking a device that was never linked is a harmless no-op, reported honestly`() {
        val registry = LinkedDeviceRegistry()
        registry.link(deviceOf("mom-phone"))

        assertThat(registry.unlink("never-paired")).isFalse()
        assertThat(registry.all()).hasSize(1)
    }

    @Test
    fun `re-linking the same device id replaces its record rather than duplicating it`() {
        val registry = LinkedDeviceRegistry()
        registry.link(deviceOf("mom-phone", alias = "old-alias"))
        registry.link(deviceOf("mom-phone", alias = "new-alias"))

        assertThat(registry.all()).hasSize(1)
        assertThat(registry.get("mom-phone")!!.keyAlias).isEqualTo("new-alias")
    }
}
