package com.hananelsabag.autocare.presentation.screens.maintenance

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceHistoryScreen(carId: Int, onBack: () -> Unit) {
    val viewModel = hiltViewModel<MaintenanceHistoryViewModel>()
    LaunchedEffect(carId) { viewModel.init(carId) }

    val records by viewModel.records.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var recordFormIsDirty by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var recordToEdit by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    // Hoisted so the discard dialog can call sheetState.expand() on "continue editing"
    val addSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && recordFormIsDirty) {
                showDiscardConfirm = true
                false // keep the sheet open
            } else {
                true
            }
        }
    )
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.maintenance_history_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(
                            visible = records.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = if (records.size == 1)
                                    stringResource(R.string.maintenance_history_subtitle_single)
                                else
                                    stringResource(R.string.maintenance_history_subtitle_plural, records.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.maintenance_history_add))
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        if (records.isEmpty()) {
            MaintenanceEmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    MaintenanceRecordCard(record = record, onClick = { selectedRecord = record })
                }
            }
        }
    }

    // ── Add / Edit Sheet ────────────────────────────────────────
    fun closeSheet() {
        recordFormIsDirty = false
        showAddSheet = false
        recordToEdit = null
    }

    if (showAddSheet || recordToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!recordFormIsDirty) closeSheet()
                // If dirty: confirmValueChange showed the dialog; sheet stays open.
            },
            sheetState = addSheetState
        ) {
            AddMaintenanceRecordSheet(
                carId = carId,
                existingRecord = recordToEdit,
                onSave = { record ->
                    if (recordToEdit != null) viewModel.updateRecord(record)
                    else viewModel.insertRecord(record)
                    closeSheet()
                },
                onDismiss = {
                    if (recordFormIsDirty) showDiscardConfirm = true
                    else closeSheet()
                },
                onDirtyChanged = { recordFormIsDirty = it }
            )
        }
    }

    // ── Discard confirmation ─────────────────────────────────────
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = {
                // Tapping outside the dialog = "continue editing"
                showDiscardConfirm = false
                scope.launch { addSheetState.expand() }
            },
            title = { Text(stringResource(R.string.add_record_discard_title)) },
            text = { Text(stringResource(R.string.add_record_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        closeSheet()
                    }
                ) {
                    Text(
                        stringResource(R.string.add_record_discard_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        scope.launch { addSheetState.expand() }
                    }
                ) {
                    Text(stringResource(R.string.add_record_discard_cancel))
                }
            }
        )
    }

    // ── Detail Sheet ────────────────────────────────────────────
    selectedRecord?.let { record ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            sheetState = sheetState
        ) {
            RecordDetailSheet(
                record = record,
                onEdit = { selectedRecord = null; recordToEdit = record },
                onDelete = { recordToDelete = record; selectedRecord = null }
            )
        }
    }

    // ── Delete Dialog ────────────────────────────────────────────
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text(stringResource(R.string.record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.record_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord(record); recordToDelete = null }) {
                    Text(text = stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
private fun MaintenanceEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                imageVector = Icons.Outlined.BuildCircle,
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.maintenance_history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.maintenance_history_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MaintenanceRecordCard(record: MaintenanceRecord, onClick: () -> Unit) {
    val accentColor = record.type.accentColor()
    val iconVector = record.type.iconVector()

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            // Full-height accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // Type icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                    .size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 14.dp, bottom = 14.dp)
            ) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Date + KM on same row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.date.toFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    record.km?.let { km ->
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.record_km_format, km.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right side: type badge + cost + receipt thumbnail
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = stringResource(record.type.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                record.costAmount?.let { cost ->
                    Text(
                        text = stringResource(R.string.record_cost_format, cost.formatCost()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                record.receiptUri?.let { uri ->
                    val ctx = LocalContext.current
                    val isPdf = remember(uri) {
                        runCatching { ctx.contentResolver.getType(Uri.parse(uri)) }
                            .getOrNull()?.startsWith("image/") == false
                    }
                    if (isPdf) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDetailSheet(
    record: MaintenanceRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = record.type.accentColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        // ── Accent header ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = record.type.iconVector(),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = stringResource(record.type.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        // ── Detail grid ───────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DetailRow(label = stringResource(R.string.record_detail_date), value = record.date.toFormattedDate())
                record.km?.let { km ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    DetailRow(label = stringResource(R.string.record_detail_km), value = stringResource(R.string.record_km_format, km))
                }
                record.costAmount?.let { cost ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    DetailRow(label = stringResource(R.string.record_detail_cost), value = stringResource(R.string.record_cost_format, cost.formatCost()))
                }
                record.notes?.let { notes ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    DetailRow(label = stringResource(R.string.record_detail_notes), value = notes)
                }
            }
        }

        // ── Receipt / document attachment ─────────────────────────
        record.receiptUri?.let { uriStr ->
            val context = LocalContext.current
            val parsedUri = remember(uriStr) { Uri.parse(uriStr) }
            val mimeType = remember(uriStr) {
                runCatching { context.contentResolver.getType(parsedUri) }.getOrNull() ?: "image/*"
            }
            val isImage = mimeType.startsWith("image/")

            Text(
                text = stringResource(R.string.record_receipt_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
            )

            if (isImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = parsedUri,
                        contentDescription = stringResource(R.string.record_receipt_label),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.documents_pdf_file),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Open button — same for both image and PDF
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(parsedUri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) { }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.documents_open_file),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Action buttons ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = if (record.receiptUri == null) 4.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_edit))
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

private fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAINTENANCE -> R.string.record_type_maintenance
    RecordType.REPAIR -> R.string.record_type_repair
    RecordType.WEAR -> R.string.record_type_wear
    RecordType.UPGRADE -> R.string.record_type_upgrade
}

@Composable
private fun RecordType.accentColor() = when (this) {
    RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primary
    RecordType.REPAIR -> MaterialTheme.colorScheme.error
    RecordType.WEAR -> MaterialTheme.colorScheme.tertiary
    RecordType.UPGRADE -> MaterialTheme.colorScheme.secondary
}

private fun RecordType.iconVector(): ImageVector = when (this) {
    RecordType.MAINTENANCE -> Icons.Outlined.Build
    RecordType.REPAIR -> Icons.Outlined.CarRepair
    RecordType.WEAR -> Icons.Outlined.Autorenew
    RecordType.UPGRADE -> Icons.Outlined.Upgrade
}

private fun Double.formatCost(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
