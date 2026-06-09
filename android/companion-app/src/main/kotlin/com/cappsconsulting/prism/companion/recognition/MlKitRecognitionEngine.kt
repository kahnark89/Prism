package com.cappsconsulting.prism.companion.recognition

import android.graphics.Bitmap
import android.graphics.Rect
import com.cappsconsulting.prism.companion.data.FaceTemplateDao
import com.cappsconsulting.prism.companion.data.FaceTemplateEntity
import com.cappsconsulting.prism.companion.hal.CameraFrame
import com.cappsconsulting.prism.companion.hal.toBitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

/**
 * [RecognitionEngine] backed by ML Kit Face Detection for face localization and a
 * pixel-similarity template store in [RecognitionDatabase].
 *
 * **How recognition works:** enroll() detects the face bounding box, crops and scales
 * the region to [TEMPLATE_SIZE]×[TEMPLATE_SIZE], converts to normalized grayscale floats,
 * and stores the resulting [ByteArray] in Room. recognize() performs the same extraction
 * on the incoming frame and returns the cosine similarity against the stored template —
 * a similarity ≥ [RECOGNITION_THRESHOLD] is treated as the enrolled child.
 *
 * **Baseline approach, named honestly:** pixel-similarity works under consistent lighting
 * and similar pose, which is the expected deployment (fixed device, child looking at it).
 * It is not rotation- or large-lighting-change-invariant. The upgrade path — TFLite
 * FaceNet or MobileNetV2 face-embedding model run via [org.tensorflow.lite.Interpreter] —
 * uses the same seam: store the model-output embedding vector rather than the pixel array.
 *
 * **Privacy guarantees maintained:**
 * - On-device only — no cloud calls, no network path anywhere in this class.
 * - Raw frames discarded immediately after template extraction — the Bitmap never
 *   outlives [extractFaceTemplate], and neither does the face crop.
 * - Templates stored in `recognition.db`, not the main session database (Hard Line 3:
 *   "recognition templates stay on-device, separate, parent-deletable").
 * - [deleteTemplates] wipes every stored template unconditionally.
 */
class MlKitRecognitionEngine(
    private val dao: FaceTemplateDao,
) : RecognitionEngine {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.15f)
            .build()
    )

    private val bgScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cached enrollment state — initialized from DB asynchronously at construction.
    // Default is false (not enrolled) — conservative until the DB query returns.
    @Volatile private var enrolledCache: Boolean = false

    init {
        bgScope.launch { enrolledCache = dao.count() > 0 }
    }

    override fun isEnrolled(): Boolean = enrolledCache

    override suspend fun enroll(frames: List<CameraFrame>): Boolean {
        val templates = withContext(Dispatchers.Default) {
            frames.mapNotNull { extractFaceTemplate(it) }
        }
        if (templates.isEmpty()) return false
        val now = System.currentTimeMillis() / 1000.0
        dao.deleteAll()
        dao.insert(FaceTemplateEntity(templateBytes = templates.first(), capturedAtEpochSeconds = now))
        enrolledCache = true
        return true
    }

    override suspend fun recognize(frame: CameraFrame): RecognitionResult {
        val stored = dao.getFirst() ?: return RecognitionResult(false, 0.0, "face")
        val current = withContext(Dispatchers.Default) { extractFaceTemplate(frame) }
            ?: return RecognitionResult(false, 0.0, "face")
        val similarity = cosineSimilarity(bytesToFloats(stored.templateBytes), bytesToFloats(current))
        return RecognitionResult(
            isEnrolledChild = similarity >= RECOGNITION_THRESHOLD,
            confidence = similarity,
            mode = "face",
        )
    }

    override fun deleteTemplates() {
        enrolledCache = false
        bgScope.launch { dao.deleteAll() }
    }

    private suspend fun extractFaceTemplate(frame: CameraFrame): ByteArray? {
        val bitmap = frame.toBitmap()
        val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
        val face = faces.firstOrNull() ?: run { bitmap.recycle(); return null }
        val template = faceRegionTemplate(bitmap, face.boundingBox)
        bitmap.recycle()
        return template
    }

    private fun faceRegionTemplate(source: Bitmap, box: Rect): ByteArray {
        val padX = box.width() / 5
        val padY = box.height() / 5
        val left = (box.left - padX).coerceAtLeast(0)
        val top = (box.top - padY).coerceAtLeast(0)
        val w = ((box.right + padX).coerceAtMost(source.width) - left).coerceAtLeast(1)
        val h = ((box.bottom + padY).coerceAtMost(source.height) - top).coerceAtLeast(1)

        val face = Bitmap.createBitmap(source, left, top, w, h)
        val scaled = Bitmap.createScaledBitmap(face, TEMPLATE_SIZE, TEMPLATE_SIZE, true)
        if (face !== source) face.recycle()

        val pixels = IntArray(TEMPLATE_SIZE * TEMPLATE_SIZE)
        scaled.getPixels(pixels, 0, TEMPLATE_SIZE, 0, 0, TEMPLATE_SIZE, TEMPLATE_SIZE)
        scaled.recycle()

        // Grayscale conversion (ITU-R BT.601 luma)
        val floats = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            floats[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }

        // Zero-mean, unit-variance normalization
        var sum = 0.0
        for (v in floats) sum += v
        val mean = (sum / floats.size).toFloat()
        var sumDev = 0.0
        for (v in floats) { val d = v - mean; sumDev += d * d }
        val std = sqrt(sumDev / floats.size + 1e-8).toFloat()
        for (i in floats.indices) floats[i] = (floats[i] - mean) / std

        return floatsToBytes(floats)
    }

    companion object {
        private const val TEMPLATE_SIZE = 64
        private const val RECOGNITION_THRESHOLD = 0.75

        private fun floatsToBytes(floats: FloatArray): ByteArray {
            val buf = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            floats.forEach { buf.putFloat(it) }
            return buf.array()
        }

        private fun bytesToFloats(bytes: ByteArray): FloatArray {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / 4) { buf.getFloat() }
        }

        private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
            var dot = 0.0; var normA = 0.0; var normB = 0.0
            val n = minOf(a.size, b.size)
            for (i in 0 until n) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            return dot / (sqrt(normA) * sqrt(normB) + 1e-8)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
