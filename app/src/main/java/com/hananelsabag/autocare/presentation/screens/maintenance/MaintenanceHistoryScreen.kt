package com.hananelsabag.autocare.presentation.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var selectedRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var recordToEdit by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.maintenance_history_title)) },
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
    if (showAddSheet || recordToEdit != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false; recordToEdit = null },
            sheetState = sheetState
        ) {
            AddMaintenanceRecordSheet(
                carId = carId,
                existingRecord = recordToEdit,
                onSave = { record ->
                    if (recordToEdit != null) viewModel.updateRecord(record)
                    else viewModel.insertRecord(record)
                    showAddSheet = false; recordToEdit = null
                },
                onDismiss = { showAddSheet = false; recordToEdit = null }
            )
        }
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

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left type accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(80.dp)
                    .background(accentColor)
            )

            // Type icon square
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = record.date.toFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 14.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = stringResource(record.type.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                record.costAmount?.let { cost ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.record_cost_format, cost.formatCost()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = record.type.iconVector(),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(record.type.labelRes()), style = MaterialTheme.typography.labelMedium, color = accentColor)
                Text(text = record.description, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = stringResource(R.string.record_detail_date), value = record.date.toFormattedDate())
        record.km?.let { km -> DetailRow(label = stringResource(R.string.record_detail_km), value = stringResource(R.string.record_km_format, km)) }
        record.costAmount?.let { cost -> DetailRow(label = stringResource(R.string.record_detail_cost), value = stringResource(R.string.record_cost_format, cost.formatCost())) }
        record.notes?.let { notes -> DetailRow(label = stringResource(R.string.record_detail_notes), value = notes) }

        // ── Receipt photo ────────────────────────────────────────
        record.receiptUri?.let { uri ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.record_receipt_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
                model = uri,
                contentDescription = stringResource(R.string.record_receipt_label),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_edit))
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAINTENANCE -> R.string.record_type_maintenance
    RecordType.REPAIR -> R.string.record_type_repair
    RecordType.WEAR -> R.string.record_type_wear
}

@Composable
private fun RecordType.accentColor() = when (this) {
    RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primary
    RecordType.REPAIR -> MaterialTheme.colorScheme.error
    RecordType.WEAR -> MaterialTheme.colorScheme.tertiary
}

private fun RecordType.iconVector(): ImageVector = when (this) {
    RecordType.MAINTENANCE -> Icons.Outlined.Build
    RecordType.REPAIR -> Icons.Outlined.CarRepair
    RecordType.WEAR -> Icons.Outlined.Autorenew
}

private fun Double.formatCost(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
