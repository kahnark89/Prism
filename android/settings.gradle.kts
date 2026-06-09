@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "prism"

// ── The shared engine (the differentiating IP — Epigenome 025) ──
// Pure Kotlin/JVM: no Android dependency, runs and tests on any JVM.
// This is the native-rewrite target for prism/engines + prism/modules
// (the pure-logic core that "survives as the algorithm spec").
include(":engine")

// ── Pairing & sync protocol (Doc 3.0 §3 — net-new, doesn't exist in the Python spec) ──
// Pure Kotlin/JVM: javax.crypto (ECDH + AES-GCM) is identical on JVM and Android.
include(":sync")

// ── Companion app (child-facing, Doc 3.0 §2 + Doc 2.3) ──
include(":companion-app")

// ── Parent Suite app (parent-facing, Doc 2.2) ──
include(":parent-suite-app")
