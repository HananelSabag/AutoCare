package com.hananelsabag.autocare.presentation.components

import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.StrokeCap
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
 * Photo framing sheet — lets the user pinch-zoom and pan to compose their
 * car photo. Viewport is 3:4 portrait (matches the card's photo area).
 * Pan is clamped so no black ever leaks from the edges.
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

    // Transform handler — clamps pan so image always fills the viewport
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (userScale * zoomChange).coerceIn(1f, 5f)
        val maxPanX = viewportSize.width * (newScale - 1) / 2f
        val maxPanY = viewportSize.height * (newScale - 1) / 2f
        userScale = newScale
        userOffset = Offset(
            x = (userOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX),
            y = (userOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── Header: Reset | Title | Close ───────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { userScale = 1f; userOffset = Offset.Zero }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.image_crop_reset),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.image_crop_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.btn_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Dark frame surround + crop viewport ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 3:4 portrait viewport — matches the card photo area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black)
                    .onSizeChanged { viewportSize = it }
            ) {
                // Photo with zoom/pan
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

                // Grid overlay: rule-of-thirds + corner brackets
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    // Rule-of-thirds grid lines (subtle)
                    val gridColor = Color.White.copy(alpha = 0.2f)
                    val gridStroke = 1.dp.toPx()
                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), gridStroke)
                    drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), gridStroke)
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), gridStroke)
                    drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), gridStroke)

                    // Corner L-brackets (crisp white framing indicators)
                    val cornerLen = 28.dp.toPx()
                    val cornerStroke = 3.dp.toPx()
                    val cornerColor = Color.White.copy(alpha = 0.9f)
                    val cap = StrokeCap.Square

                    // Top-left
                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLen, 0f), cornerStroke, cap)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLen), cornerStroke, cap)
                    // Top-right
                    drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLen, 0f), cornerStroke, cap)
                    drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLen), cornerStroke, cap)
                    // Bottom-left
                    drawLine(cornerColor, Offset(0f, h), Offset(cornerLen, h), cornerStroke, cap)
                    drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLen), cornerStroke, cap)
                    // Bottom-right
                    drawLine(cornerColor, Offset(w, h), Offset(w - cornerLen, h), cornerStroke, cap)
                    drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLen), cornerStroke, cap)
                }
            }
        }

        // ── Zoom indicator ───────────────────────────────────────────
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.image_crop_zoom_level, "%.1f".format(userScale)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Hint ─────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.image_crop_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        // ── Buttons ──────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
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
                            ) ?: uri
                        } else {
                            uri
                        }
                        onConfirm(result)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
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
