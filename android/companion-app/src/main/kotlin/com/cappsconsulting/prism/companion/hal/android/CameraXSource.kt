package com.cappsconsulting.prism.companion.hal.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cappsconsulting.prism.companion.hal.CameraFrame
import com.cappsconsulting.prism.companion.hal.CameraSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * CameraX implementation of [CameraSource] — replatform item 1's camera half.
 * Binds one [ImageAnalysis] use case (pull-based, on-demand frames via [captureFrame])
 * and one [Preview] use case (the live viewfinder the child watches) in a single
 * [ProcessCameraProvider.bindToLifecycle] call — they must bind together, not in
 * two separate calls, because CameraX replaces rather than adds on repeated calls.
 *
 * [previewView] is created here and exposed for [MechanicalPresentationScreen] to
 * mount via `AndroidView`. This lets [MechanicalPresentationScreen] skip its own
 * [ProcessCameraProvider] binding entirely when an orchestrator-wired [CameraXSource]
 * is available — see that composable's `cameraPreviewView` parameter.
 *
 * Frame delivery is pull-based: [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST] ensures the
 * analyzer only buffers one frame at a time; [captureFrame]'s `receive()` blocks until
 * the next frame delivers and returns immediately if one is already waiting. Frames that
 * arrive when no `captureFrame()` is pending are dropped via `trySend` — the camera
 * stream runs continuously but frames are only converted on demand, saving the
 * per-frame YUV→RGB conversion cost for taps that actually want a frame.
 */
class CameraXSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : CameraSource {

    /** Mount this in `AndroidView` to show the live viewfinder — see [MechanicalPresentationScreen]. */
    val previewView: PreviewView = PreviewView(context)

    private val analyzerExecutor = Executors.newSingleThreadExecutor()
    private val frameChannel = Channel<CameraFrame>(capacity = Channel.RENDEZVOUS)
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override suspend fun start() {
        val provider = awaitCameraProvider()
        cameraProvider = provider

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analyzerExecutor, ::analyzeFrame) }
        imageAnalysis = analysis

        val preview = Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        withContext(Dispatchers.Main) {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            cameraProvider?.unbindAll()
        }
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
    }

    override suspend fun captureFrame(): CameraFrame = frameChannel.receive()

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val frame = imageProxy.toCameraFrame()
        imageProxy.close()
        frameChannel.trySend(frame)
    }

    private fun ImageProxy.toCameraFrame(): CameraFrame {
        val sourceBitmap = this.toBitmap()
        val rotationDegrees = this.imageInfo.rotationDegrees
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, false)
        } else {
            sourceBitmap
        }

        val scaled = Bitmap.createScaledBitmap(rotated, TARGET_W, TARGET_H, false)
        val pixels = IntArray(TARGET_W * TARGET_H)
        scaled.getPixels(pixels, 0, TARGET_W, 0, 0, TARGET_W, TARGET_H)

        val rgb = ByteArray(TARGET_W * TARGET_H * 3)
        for (i in pixels.indices) {
            rgb[i * 3] = ((pixels[i] shr 16) and 0xFF).toByte()
            rgb[i * 3 + 1] = ((pixels[i] shr 8) and 0xFF).toByte()
            rgb[i * 3 + 2] = (pixels[i] and 0xFF).toByte()
        }

        if (scaled !== rotated) scaled.recycle()
        if (rotated !== sourceBitmap) rotated.recycle()
        sourceBitmap.recycle()

        return CameraFrame(widthPx = TARGET_W, heightPx = TARGET_H, rgb = rgb)
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(context),
            )
        }

    companion object {
        private const val TARGET_W = 640
        private const val TARGET_H = 480
    }
}
