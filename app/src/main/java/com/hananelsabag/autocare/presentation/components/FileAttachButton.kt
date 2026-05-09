package com.hananelsabag.autocare.presentation.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.util.copyUriToInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified file attach UI — used in both CarDocumentsScreen and AddMaintenanceRecordSheet.
 *
 * Handles: gallery pick, camera capture, PDF pick, URI accessibility check,
 * inline image preview, "file not available" warning, and clear.
 */
@Composable
fun FileAttachButton(
    fileUri: String?,
    onFileSelected: (String) -> Unit,
    onClearFile: () -> Unit,
    cameraUriProvider: (() -> Uri)? = null,
    subfolder: String = "documents",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Check if stored URI is actually accessible
    var isAccessible by remember(fileUri) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(fileUri) {
        if (fileUri == null) { isAccessible = null; return@LaunchedEffect }
        isAccessible = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(fileUri))?.close()
                true
            } catch (_: Exception) { false }
        }
    }

    val mimeType = remember(fileUri) {
        if (fileUri == null) null
        else try { context.contentResolver.getType(Uri.parse(fileUri)) ?: "*/*" } catch (_: Exception) { "*/*" }
    }
    val isImage = mimeType?.startsWith("image/") == true

    // Launchers
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { scope.launch { copyAndSet(context, it, subfolder, onFileSelected) } }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { scope.launch { copyAndSet(context, it, subfolder, onFileSelected) } }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val cameraUri = pendingCameraUri
        pendingCameraUri = null  // always reset to avoid stale URI on next open
        if (success && cameraUri != null) {
            scope.launch { copyAndSet(context, cameraUri, subfolder, onFileSelected) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (fileUri == null) {
            // ── Empty state: choose source ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cameraUriProvider != null) {
                    OutlinedButton(
                        onClick = {
                            val uri = cameraUriProvider()
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.photo_option_camera), style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.photo_option_gallery), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { pdfLauncher.launch("application/pdf") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            // ── File selected state ─────────────────────────────────────────
            when {
                isAccessible == false -> {
                    // File gone — show warning
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.file_not_available),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearFile, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Allow re-attaching
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.file_reattach), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PDF", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                isImage -> {
                    // Image preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = Uri.parse(fileUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Clear button
                        IconButton(
                            onClick = onClearFile,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    MaterialTheme.shapes.small
                                )
                                .size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OpenFileRow(fileUri = fileUri, mimeType = mimeType ?: "*/*")
                }

                else -> {
                    // PDF / other file
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.documents_pdf_file),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearFile, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OpenFileRow(fileUri = fileUri, mimeType = mimeType ?: "*/*")
                }
            }
        }
    }
}

@Composable
private fun OpenFileRow(fileUri: String, mimeType: String) {
    val context = LocalContext.current
    val uri = Uri.parse(fileUri)
    val noAppMsg = stringResource(R.string.file_no_app_to_open)
    FilledTonalButton(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, noAppMsg, Toast.LENGTH_SHORT).show()
            }
        },
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.documents_open_file), style = MaterialTheme.typography.labelSmall)
    }
}

private suspend fun copyAndSet(
    context: android.content.Context,
    uri: Uri,
    subfolder: String,
    onResult: (String) -> Unit
) {
    val copied = copyUriToInternal(context, uri, subfolder)
    onResult((copied ?: uri).toString())
}
