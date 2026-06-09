package com.cappsconsulting.prism.companion.vision

import com.cappsconsulting.prism.companion.hal.CameraFrame
import kotlin.math.round

/** Pixel rectangle `(x, y, width, height)` — the named replacement for the Python `tuple[int,int,int,int]` `box`. */
data class BoundingBox(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * Direct port of `prism/modules/vision_engine.py::VisionResult`. [rawFrame] keeps the
 * same role the original's `raw_frame: np.ndarray` did — available to whatever needs
 * pixels (e.g. [com.cappsconsulting.prism.companion.recognition.RecognitionEngine],
 * exactly as `orchestrator.py` reused the same captured `frame` for both `vision.infer`
 * and `recognition.recognize`), discarded once the pipeline step holding it returns.
 */
data class VisionResult(
    val label: String,
    val confidence: Double,
    val box: BoundingBox,
    val rawFrame: CameraFrame,
)

/**
 * Redesign of `VisionEngine`'s *contract* — `infer(frame) -> VisionResult`, unchanged,
 * because nothing about "what comes out the other end of object recognition" is
 * platform-shaped. What's platform-shaped is *how the inference runs*: the Python
 * dispatched between `_tflite_infer` (real model, via `tflite_runtime`) and
 * `_mock_infer` depending on whether `load_model` found a `.tflite` file on disk.
 *
 * The Android equivalent of `_tflite_infer` — call it `TfliteVisionClassifier` — is
 * named here, not written: it needs a bundled `mobilenet_v1.tflite` + `labels.txt`
 * (model assets this port doesn't carry) and the `org.tensorflow:tensorflow-lite`
 * runtime wired through `Interpreter`/`ByteBuffer` preprocessing — real engineering
 * work, not a translation, and dishonest to fake here. [MockVisionClassifier] is
 * the *other* half of that dispatch — faithfully ported below — which the original
 * also used as its fallback whenever no model loaded. It is therefore not a stub but
 * the documented, working default this app ships with until model assets land.
 */
interface VisionClassifier {
    suspend fun infer(frame: CameraFrame): VisionResult
}

private val MOCK_LABELS = listOf(
    "apple", "banana", "orange", "cat", "dog", "flower",
    "car", "book", "cup", "chair", "tree", "bird",
)

/**
 * Direct port of `VisionEngine._mock_infer` — same cycling label set, same
 * `0.75 + (idx % 4) * 0.05` confidence formula (rounded to 2dp, matching the
 * original's `round(confidence, 2)`), same fixed `(100, 100, 200, 200)` box, same
 * raw-frame pass-through. Stateful by design — `_mock_idx` was an instance counter
 * in the original too, advancing once per call so a session cycles through every
 * label rather than repeating one.
 */
class MockVisionClassifier : VisionClassifier {
    private var index = 0

    override suspend fun infer(frame: CameraFrame): VisionResult {
        val label = MOCK_LABELS[index % MOCK_LABELS.size]
        val confidence = round((0.75 + (index % 4) * 0.05) * 100.0) / 100.0
        index += 1
        return VisionResult(
            label = label,
            confidence = confidence,
            box = BoundingBox(x = 100, y = 100, width = 200, height = 200),
            rawFrame = frame,
        )
    }
}
