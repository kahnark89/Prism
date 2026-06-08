// :engine — the shared engine (Doc 3.0 §2's "private library/SDK").
//
// Pure Kotlin/JVM, no Android dependency: this module carries the
// platform-agnostic IP that ports "verbatim" or "as algorithm/spec"
// (Epigenome 025) — inner_life today; memory, mood_line, grounding,
// personas, safety, learning_log follow the same pattern established here.
//
// The Python stack at /prism is the reference spec for this module's
// translation, not a runtime dependency (Epigenome 025, sign-off: native
// Kotlin/Java rewrite over a CPython bridge).

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
