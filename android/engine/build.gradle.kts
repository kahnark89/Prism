// The shared engine — "the actual differentiating IP" (Epigenome 025).
//
// Pure Kotlin/JVM on purpose: inner_life, memory, mood_line, grounding, personas,
// safety, and learning_log are platform-agnostic dynamical-system / state-machine
// logic with zero hardware coupling in the Python reference (prism/engines/*,
// prism/modules/{safety,learning_log}.py, prism/personas/*). Epigenome 025 is
// explicit that this is the part that "ports verbatim" — so it gets to be the one
// part of this replatform that actually compiles and runs its tests in any JVM,
// this sandbox included, with no Android SDK in the loop.
//
// Both Android apps depend on this as a regular library; on-device it runs inside
// the Android Runtime exactly as it runs here on OpenJDK — same bytecode target.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // 21, not 17: it's the only JDK present in this sandbox (and AGP 8.7.2 / Gradle 8.14.3
    // both run fine on it for a pure-JVM module like this one — no Android SDK involved here).
    // The two Android app modules still target Java 17 bytecode via compileOptions, per
    // AGP's recommended baseline; this toolchain choice is local to :engine's own build.
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

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
