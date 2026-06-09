package com.cappsconsulting.prism.companion

import android.app.Application

/**
 * Referenced by `AndroidManifest.xml` (`android:name=".PrismCompanionApp"`) —
 * exists to satisfy that reference honestly rather than leave the manifest
 * pointing at a name Android would silently paper over with a default
 * [Application] instance. Deliberately minimal: this app's actual composition
 * root — wiring [com.cappsconsulting.prism.companion.hal.CompanionHal],
 * [com.cappsconsulting.prism.companion.recognition.RecognitionEngine], and
 * the rest of [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator]'s
 * dependency graph into a running session — is real platform-API engineering
 * work gated on pieces this port names rather than fabricates (concrete Android
 * implementations of `CameraSource`, `SpeakerOutput`, `HapticOutput`, and a
 * biometrically-honest `RecognitionEngine`). Building that root before those
 * pieces exist would mean wiring real [Application] lifecycle to a graph that
 * can't actually run — the "looks like it's working" trap this port has named
 * and routed around since [com.cappsconsulting.prism.companion.vision.VisionClassifier]'s
 * kdoc first drew the line.
 */
class PrismCompanionApp : Application()
