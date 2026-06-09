// Root build file — declares plugin versions for subprojects without applying them here.
// Android Gradle Plugin requires the Android SDK (compileSdk platform + build-tools);
// the :engine and :sync modules are pure Kotlin/JVM and build without it (see README.md
// "What can/can't be verified in CI without an SDK").
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
