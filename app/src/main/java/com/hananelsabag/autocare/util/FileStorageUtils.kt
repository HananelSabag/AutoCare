package com.hananelsabag.autocare.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies any content:// or file:// URI into the app's private filesDir.
 * Returns a stable FileProvider URI that survives gallery deletions and
 * permission resets forever.
 */
suspend fun copyUriToInternal(
    context: Context,
    sourceUri: Uri,
    subfolder: String
): Uri? = withContext(Dispatchers.IO) {
    try {
        val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val dir = File(context.filesDir, subfolder).apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: return@withContext null

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

/**
 * Saves a car photo (already stored in filesDir) into the device gallery
 * under Pictures/AutoCare so the user can find it in their gallery app.
 * No-op on Android < 10 (no permission needed on 10+).
 */
suspend fun saveCarPhotoToGallery(context: Context, file: File): Unit = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AutoCare")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } catch (_: Exception) {}
}
