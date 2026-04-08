package com.hananelsabag.autocare.presentation.screens.maintenance

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.util.toFormattedDate
import java.time.LocalDate
import java.time.ZoneId

private fun createTempCameraUri(context: Context): Uri {
    val file = File.createTempFile("receipt_photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceRecordSheet(
    carId: Int,
    existingRecord: MaintenanceRecord? = null,
    onSave: (MaintenanceRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingRecord != null

    var type by remember { mutableStateOf(existingRecord?.type ?: RecordType.MAINTENANCE) }
    var dateMs by remember {
        mutableStateOf(
            existingRecord?.date
                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
    var description by remember { mutableStateOf(existingRecord?.description ?: "") }
    var km by remember { mutableStateOf(existingRecord?.km?.toString() ?: "") }
    var cost by remember { mutableStateOf(existingRecord?.costAmount?.toString() ?: "") }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }
    var receiptUri by remember { mutableStateOf(existingRecord?.receiptUri) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showReceiptSourceMenu by remember { mutableStateOf(false) }
    var cameraReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var descriptionError by remember { mutableStateOf(false) }
    var kmError by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            receiptUri = it.toString()
        }
    }

    val receiptCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) { cameraReceiptUri?.let { receiptUri = it.toString() } }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            receiptUri = it.toString()
        }
    }

    fun validate(): Boolean {
        descriptionError = description.isBlank()
        kmError = type == RecordType.MAINTENANCE && km.isBlank()
        return !descriptionError && !kmError
    }

    // Quick chip lists per type — resolved here so stringResource works
    val maintenanceChips = listOf(
        stringResource(R.string.chip_oil_change),
        stringResource(R.string.chip_routine_check),
        stringResource(R.string.chip_air_filter),
        stringResource(R.string.chip_oil_filter),
        stringResource(R.string.chip_periodic_service)
    )
    val repairChips = listOf(
        stringResource(R.string.chip_brakes_repair),
        stringResource(R.string.chip_part_replacement),
        stringResource(R.string.chip_electrical_repair),
        stringResource(R.string.chip_engine_repair),
        stringResource(R.string.chip_body_work)
    )
    val wearChips = listOf(
        stringResource(R.string.chip_tires_replacement),
        stringResource(R.string.chip_brakes_replacement),
        stringResource(R.string.chip_battery_replacement),
        stringResource(R.string.chip_wipers_replacement),
        stringResource(R.string.chip_belts_replacement)
    )
    val upgradeChips = listOf(
        stringResource(R.string.chip_audio_upgrade),
        stringResource(R.string.chip_window_tinting),
        stringResource(R.string.chip_suspension_upgrade),
        stringResource(R.string.chip_body_kit),
        stringResource(R.string.chip_paint_job),
        stringResource(R.string.chip_performance_upgrade)
    )

    val quickChips = when (type) {
        RecordType.MAINTENANCE -> maintenanceChips
        RecordType.REPAIR -> repairChips
        RecordType.WEAR -> wearChips
        RecordType.UPGRADE -> upgradeChips
    }

    val descriptionPlaceholder = when (type) {
        RecordType.MAINTENANCE -> stringResource(R.string.record_description_placeholder_maintenance)
        RecordType.REPAIR -> stringResource(R.string.record_description_placeholder_repair)
        RecordType.WEAR -> stringResource(R.string.record_description_placeholder_wear)
        RecordType.UPGRADE -> stringResource(R.string.record_description_placeholder_upgrade)
    }

    // Chip container / label colors match the selected record type accent
    val chipContainerColor = when (type) {
        RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primaryContainer
        RecordType.REPAIR -> MaterialTheme.colorScheme.errorContainer
        RecordType.WEAR -> MaterialTheme.colorScheme.tertiaryContainer
        RecordType.UPGRADE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val chipLabelColor = when (type) {
        RecordType.MAINTENANCE -> MaterialTheme.colorScheme.onPrimaryContainer
        RecordType.REPAIR -> MaterialTheme.colorScheme.onErrorContainer
        RecordType.WEAR -> MaterialTheme.colorScheme.onTertiaryContainer
        RecordType.UPGRADE -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val chipBorderColor = when (type) {
        RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        RecordType.REPAIR -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        RecordType.WEAR -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        RecordType.UPGRADE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
    ) {

        // ── Header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(
                    if (isEditing) R.string.edit_record_title else R.string.add_record_title
                ),
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

        // ── Record type selector ─────────────────────────────────────
        SheetSectionHeader(stringResource(R.string.record_type_label))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordType.entries.forEach { recordType ->
                val accentColor = when (recordType) {
                    RecordType.MAINTENANCE -> MaterialTheme.colorScheme.primary
                    RecordType.REPAIR -> MaterialTheme.colorScheme.error
                    RecordType.WEAR -> MaterialTheme.colorScheme.tertiary
                    RecordType.UPGRADE -> MaterialTheme.colorScheme.secondary
                }
                val icon = when (recordType) {
                    RecordType.MAINTENANCE -> Icons.Outlined.Build
                    RecordType.REPAIR -> Icons.Outlined.CarRepair
                    RecordType.WEAR -> Icons.Outlined.Autorenew
                    RecordType.UPGRADE -> Icons.Outlined.Upgrade
                }
                FilterChip(
                    selected = type == recordType,
                    onClick = { type = recordType; kmError = false },
                    label = {
                        Text(
                            text = stringResource(recordType.labelRes()),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.15f),
                        selectedLabelColor = accentColor,
                        selectedLeadingIconColor = accentColor
                    ),
                    border = if (type == recordType)
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            selectedBorderColor = accentColor,
                            selectedBorderWidth = 1.5.dp
                        ) else FilterChipDefaults.filterChipBorder(enabled = true, selected = false),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Date ─────────────────────────────────────────────────────
        SheetSectionHeader(stringResource(R.string.record_date))
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = dateMs.toFormattedDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Description ───────────────────────────────────────────────
        SheetSectionHeader(stringResource(R.string.record_description))

        // Quick-select suggestion chips (type-aware, wrapping)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickChips.forEach { chip ->
                SuggestionChip(
                    onClick = {
                        description = chip
                        descriptionError = false
                    },
                    label = {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = chipContainerColor.copy(alpha = 0.6f),
                        labelColor = chipLabelColor
                    ),
                    border = BorderStroke(1.dp, chipBorderColor)
                )
            }
        }

        // Description text field with dynamic placeholder
        OutlinedTextField(
            value = description,
            onValueChange = { description = it; descriptionError = false },
            label = { Text(stringResource(R.string.record_description)) },
            placeholder = {
                Text(
                    text = descriptionPlaceholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            isError = descriptionError,
            supportingText = if (descriptionError) {
                { Text(stringResource(R.string.error_field_required)) }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            maxLines = 4,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── KM + Cost ─────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = km,
                onValueChange = { km = it; kmError = false },
                label = {
                    Text(
                        if (type == RecordType.MAINTENANCE)
                            stringResource(R.string.record_km)
                        else
                            stringResource(R.string.record_km_optional)
                    )
                },
                isError = kmError,
                supportingText = if (kmError) {
                    { Text(stringResource(R.string.error_field_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text(stringResource(R.string.record_cost_optional)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
        }

        // ── Notes ─────────────────────────────────────────────────────
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.record_notes)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            maxLines = 3,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Receipt photo ─────────────────────────────────────────────
        SheetSectionHeader(stringResource(R.string.record_receipt_label))

        // Shared source-picker dropdown
        val receiptSourceDropdown: @Composable () -> Unit = {
            DropdownMenu(
                expanded = showReceiptSourceMenu,
                onDismissRequest = { showReceiptSourceMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_option_camera)) },
                    leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                    onClick = {
                        showReceiptSourceMenu = false
                        val uri = createTempCameraUri(context)
                        cameraReceiptUri = uri
                        receiptCameraLauncher.launch(uri)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_option_gallery)) },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        showReceiptSourceMenu = false
                        receiptPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_option_pdf)) },
                    leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                    onClick = {
                        showReceiptSourceMenu = false
                        pdfPicker.launch("application/pdf")
                    }
                )
            }
        }

        if (receiptUri != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable { showReceiptSourceMenu = true }
            ) {
                AsyncImage(
                    model = receiptUri,
                    contentDescription = stringResource(R.string.record_receipt_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { receiptUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                receiptSourceDropdown()
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { showReceiptSourceMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.record_receipt_add),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                receiptSourceDropdown()
            }
        }

        // ── Save ──────────────────────────────────────────────────────
        val isSaveable = description.isNotBlank() &&
            (type != RecordType.MAINTENANCE || km.isNotBlank())

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                if (validate()) {
                    onSave(
                        MaintenanceRecord(
                            id = existingRecord?.id ?: 0,
                            carId = carId,
                            type = type,
                            date = dateMs,
                            description = description.trim(),
                            km = km.trim().toIntOrNull(),
                            costAmount = cost.trim().toDoubleOrNull(),
                            notes = notes.trim().ifBlank { null },
                            receiptUri = receiptUri,
                            createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            },
            enabled = isSaveable,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.add_record_save),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ── Date picker dialog ────────────────────────────────────────────
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMs = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun SheetSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

private fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAINTENANCE -> R.string.record_type_maintenance
    RecordType.REPAIR -> R.string.record_type_repair
    RecordType.WEAR -> R.string.record_type_wear
    RecordType.UPGRADE -> R.string.record_type_upgrade
}
