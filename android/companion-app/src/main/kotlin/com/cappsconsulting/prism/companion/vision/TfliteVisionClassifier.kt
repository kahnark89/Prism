package com.cappsconsulting.prism.companion.vision

import android.content.Context
import android.util.Log
import com.cappsconsulting.prism.companion.hal.CameraFrame
import com.cappsconsulting.prism.companion.hal.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

private const val TAG = "TfliteVisionClassifier"
private const val MODEL_ASSET = "mobilenet_v1.tflite"

/**
 * [VisionClassifier] backed by a bundled MobileNet V1 `.tflite` model via the TFLite Task
 * Vision [ImageClassifier].
 *
 * **To activate real inference:** place `mobilenet_v1.tflite` and its companion `labels.txt`
 * in `android/companion-app/src/main/assets/`. The Task Vision library reads the model from
 * the APK asset bundle and handles input preprocessing (resizing, pixel normalization) and
 * output postprocessing (label lookup, score sorting) internally — [TensorImage.fromBitmap]
 * is the only caller-side step needed.
 *
 * **Graceful fallback:** if the model file is absent (first build, before assets land) the
 * constructor catches the [Exception] from [ImageClassifier.createFromFileAndOptions] and
 * sets [classifier] to null. Every [infer] call then routes to [MockVisionClassifier], which
 * is the documented working default ([VisionClassifier]'s kdoc: "the other half of that
 * dispatch — faithfully ported... not a stub but the documented, working default").
 */
class TfliteVisionClassifier(context: Context) : VisionClassifier {

    private val classifier: ImageClassifier? = runCatching {
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.05f)
            .build()
        ImageClassifier.createFromFileAndOptions(context, MODEL_ASSET, options)
    }.onFailure { e ->
        Log.i(TAG, "Model asset '$MODEL_ASSET' not found — using MockVisionClassifier: ${e.message}")
    }.getOrNull()

    private val mockFallback = MockVisionClassifier()

    override suspend fun infer(frame: CameraFrame): VisionResult {
        val cls = classifier ?: return mockFallback.infer(frame)
        return withContext(Dispatchers.Default) {
            val bitmap = frame.toBitmap()
            val tensorImage = TensorImage.fromBitmap(bitmap)
            bitmap.recycle()
            val results = cls.classify(tensorImage)
            val category = results.firstOrNull()?.categories?.firstOrNull()
                ?: return@withContext mockFallback.infer(frame)
            VisionResult(
                label = category.label,
                confidence = category.score.toDouble(),
                box = BoundingBox(0, 0, frame.widthPx, frame.heightPx),
                rawFrame = frame,
            )
        }
    }
}
