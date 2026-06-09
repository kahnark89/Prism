// Pairing & sync protocol — Doc 3.0 §3, "a proposal... flagged for technical sign-off."
// Net-new: nothing in prism/ ports here, because the single-process Python design
// never had a link to sync across (Epigenome 024 — the platform pivot is what
// *creates* this module's reason to exist).
//
// Pure Kotlin/JVM on purpose, same as :engine: ECDH key agreement (java.security)
// and AES-GCM sealing (javax.crypto) are identical on the JVM and on Android — the
// "zero-knowledge relay" claim (§3.3/§3.4) has to be checkable, and a protocol that
// only "works" inside the Android Runtime would be a weaker, less auditable claim
// than one that runs and proves itself in any JVM, this sandbox's included.
//
// What's deliberately NOT here (kept as interfaces for the apps to implement):
// Android Keystore storage (KeyStorage — "AndroidKeyStore" doesn't exist on a plain
// JVM) and real network transports (SyncTransport — NSD/Wi-Fi Direct for P2P, HTTPS
// to the relay). Both need the platform and a live network this sandbox doesn't have.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21) // see :engine/build.gradle.kts — same reasoning, same pin.
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
