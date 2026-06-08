// Prism — Android platform root build file.
// Plugin versions pinned here, applied `false` at the root and per-module
// in subprojects — standard multi-module convention so :companion and
// :parentSuite can later apply com.android.application without redeclaring
// the Kotlin version.

plugins {
    kotlin("jvm") version "2.0.21" apply false
}
