// Prism — Android platform root.
//
// Doc 3.0 §2: both apps are thin shells around one shared engine (a private
// library/SDK). `:engine` is that library — pure Kotlin/JVM where the logic
// is platform-agnostic (inner_life, memory, mood_line, grounding, personas,
// safety, learning_log — Epigenome 025's "survives as algorithm/spec" list).
//
// `:companion` and `:parentSuite` (Doc 3.0 §2 module-to-app mapping) are
// Android application modules added once Android-SDK tooling is available in
// the build environment — they cannot configure without `android.jar`.
// Adding them later is `include(":companion", ":parentSuite")` plus their
// module directories; this root is shaped so that's the only change needed.

rootProject.name = "prism-platform"

include(":engine")
