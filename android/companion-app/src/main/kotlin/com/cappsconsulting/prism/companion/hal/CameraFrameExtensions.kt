package com.cappsconsulting.prism.companion.hal

import android.graphics.Bitmap

/** Converts the packed RGB `ByteArray` back to an ARGB_8888 [Bitmap] — needed by both the TFLite classifier and ML Kit recognition engine. */
fun CameraFrame.toBitmap(): Bitmap {
    val pixels = IntArray(widthPx * heightPx)
    for (i in pixels.indices) {
        val r = rgb[i * 3].toInt() and 0xFF
        val g = rgb[i * 3 + 1].toInt() and 0xFF
        val b = rgb[i * 3 + 2].toInt() and 0xFF
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    return Bitmap.createBitmap(pixels, widthPx, heightPx, Bitmap.Config.ARGB_8888)
}
