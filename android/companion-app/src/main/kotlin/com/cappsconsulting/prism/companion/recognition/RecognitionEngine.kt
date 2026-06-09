package com.cappsconsulting.prism.companion.recognition

import com.cappsconsulting.prism.companion.hal.CameraFrame

/**
 * Direct port of `prism/modules/recognition.py::RecognitionResult`. [mode] keeps the
 * original's bare wire string (`"face" | "voice" | "mock"`) rather than becoming an
 * enum — it is logged and persisted but never branched on by name anywhere in the
 * pipeline (`orchestrator.py` only ever reads `.is_enrolled_child`/`.confidence`);
 * inventing a closed type for a value nothing closes over would be precision theater.
 */
data class RecognitionResult(
    val isEnrolledChild: Boolean,
    val confidence: Double,
    val mode: String,
)

/**
 * Redesign of `RecognitionModule`'s *contract*. The Python docstring is the actual
 * spec, and it survives unchanged: **"On-device only. Never calls cloud. Raw
 * frames/audio are discarded immediately after embedding extraction. Templates stored
 * in [a] separate recognition database (never in the main db)."** That is a privacy
 * guarantee, not an implementation detail — Doc 3.0 §3.1's "keep this list short, it's
 * the privacy surface" instinct applies just as hard on-device as across the wire.
 *
 * A faithful concrete implementation — `MlKitRecognitionEngine`, say — needs Google's
 * on-device ML Kit Face Detection (or an equivalent embedding model bundled as an
 * asset) plus a Room table living in its *own* database file, mirroring the original's
 * deliberate `recognition.db` separation from the main store. That is real engineering
 * work gated on real model assets and real enrollment UX (a guided "look at the
 * camera" flow this redesign hasn't drawn yet) — exactly the kind of gap this port
 * names rather than papers over with a confidence number that would *look* like a
 * working face-recognition system and *be* a fabrication wearing its clothes. Unlike
 * [com.cappsconsulting.prism.companion.vision.VisionClassifier], there is no mock
 * implementation here: a fake "yes, this is your enrolled child" is never an honest
 * placeholder for a biometric safety gate, even one labeled `mode = "mock"`.
 */
interface RecognitionEngine {
    /** Direct port of `is_enrolled` — `True` once at least one template has been captured. */
    fun isEnrolled(): Boolean

    /** Direct port of `recognize` — on-device only, returns yes/no plus confidence, retains nothing. */
    suspend fun recognize(frame: CameraFrame): RecognitionResult

    /** Direct port of `enroll` — extracts embeddings from the given frames; the frames themselves must not outlive this call. */
    suspend fun enroll(frames: List<CameraFrame>): Boolean

    /** Direct port of `delete_templates` — parent-callable, wipes every stored template unconditionally. */
    fun deleteTemplates()
}
