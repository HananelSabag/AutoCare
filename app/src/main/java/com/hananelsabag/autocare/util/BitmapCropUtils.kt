package com.hananelsabag.autocare.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Crops the bitmap at [uri] to the region currently visible in the crop viewport.
 *
 * @param context       App context for content resolver + filesDir
 * @param uri           Source image URI
 * @param viewportPxW   Crop viewport width in pixels (density-aware)
 * @param viewportPxH   Crop viewport height in pixels (density-aware)
 * @param userScale     Zoom level applied by the user (1f = no extra zoom)
 * @param userOffset    Pan offset applied by the user in pixels
 *
 * @return URI of the saved cropped image, or null on failure.
 */
suspend fun cropAndSaveImage(
    context: Context,
    uri: Uri,
    viewportPxW: Int,
    viewportPxH: Int,
    userScale: Float,
    userOffset: Offset
): Uri? = withContext(Dispatchers.IO) {
    try {
        // 1. Decode bitmap (downsampled to ~1200px max dimension for memory safety)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        val maxDim = 1200
        val sampleSize = computeSampleSize(options.outWidth, options.outHeight, maxDim)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return@withContext null

        val bitmapW = bitmap.width
        val bitmapH = bitmap.height

        // 2. Compute initial fill scale (ContentScale.Crop equivalent)
        val initialScale = max(viewportPxW.toFloat() / bitmapW, viewportPxH.toFloat() / bitmapH)
        val totalScale = initialScale * userScale

        // 3. Compute visible region in bitmap coordinates
        // The image is centered and then offset by userOffset (in screen px).
        // Center of viewport maps to bitmap center minus the pan offset divided by totalScale.
        val imageCenterX = bitmapW / 2f - userOffset.x / totalScale
        val imageCenterY = bitmapH / 2f - userOffset.y / totalScale

        val visibleW = viewportPxW / totalScale
        val visibleH = viewportPxH / totalScale

        val left   = (imageCenterX - visibleW / 2f).coerceAtLeast(0f).roundToInt()
        val top    = (imageCenterY - visibleH / 2f).coerceAtLeast(0f).roundToInt()
        val right  = (imageCenterX + visibleW / 2f).coerceAtMost(bitmapW.toFloat()).roundToInt()
        val bottom = (imageCenterY + visibleH / 2f).coerceAtMost(bitmapH.toFloat()).roundToInt()

        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        // 4. Crop
        val cropped = Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        bitmap.recycle()

        // 5. Save to filesDir/car_photos/
        val dir = File(context.filesDir, "car_photos").apply { mkdirs() }
        val file = File(dir, "car_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        cropped.recycle()

        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}

private fun computeSampleSize(width: Int, height: Int, maxDim: Int): Int {
    var size = 1
    while (width / size > maxDim || height / size > maxDim) size *= 2
    return size
}
