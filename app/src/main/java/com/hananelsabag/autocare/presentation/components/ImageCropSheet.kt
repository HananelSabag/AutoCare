package com.hananelsabag.autocare.presentation.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.util.cropAndSaveImage
import kotlinx.coroutines.launch

/**
 * A bottom-sheet-style composable that lets the user pinch-zoom and drag
 * to frame their car photo. On confirm it crops and saves the result.
 *
 * @param uri       The source image URI (already has persistable permission)
 * @param onConfirm Called with the new cropped URI (or the original if crop fails)
 * @param onDismiss Called when the user taps Cancel
 */
@Composable
fun ImageCropSheet(
    uri: Uri,
    onConfirm: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userScale by remember { mutableFloatStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var isSaving by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (userScale * zoomChange).coerceIn(1f, 4f)
        userScale = newScale
        userOffset += panChange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.image_crop_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.btn_cancel)
                )
            }
        }

        // ── Crop viewport ────────────────────────────────────────────
        // 4:3 landscape viewport — matches usage in CarCard and hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black)
                .onSizeChanged { viewportSize = it }
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = userScale
                        scaleY = userScale
                        translationX = userOffset.x
                        translationY = userOffset.y
                    }
                    .transformable(state = transformState)
            )
        }

        // ── Hint ─────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.image_crop_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // ── Buttons ──────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        val result = if (viewportSize != IntSize.Zero) {
                            cropAndSaveImage(
                                context = context,
                                uri = uri,
                                viewportPxW = viewportSize.width,
                                viewportPxH = viewportSize.height,
                                userScale = userScale,
                                userOffset = userOffset
                            ) ?: uri // fall back to original if crop fails
                        } else {
                            uri
                        }
                        onConfirm(result)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isSaving)
                        stringResource(R.string.image_crop_saving)
                    else
                        stringResource(R.string.image_crop_confirm),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
