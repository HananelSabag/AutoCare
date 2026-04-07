package com.hananelsabag.autocare.presentation.screens.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.util.toFormattedDate
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceRecordSheet(
    carId: Int,
    existingRecord: MaintenanceRecord? = null,
    onSave: (MaintenanceRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = existingRecord != null

    var type by remember { mutableStateOf(existingRecord?.type ?: RecordType.MAINTENANCE) }
    var dateMs by remember {
        mutableStateOf(
            existingRecord?.date ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
    var description by remember { mutableStateOf(existingRecord?.description ?: "") }
    var km by remember { mutableStateOf(existingRecord?.km?.toString() ?: "") }
    var cost by remember { mutableStateOf(existingRecord?.costAmount?.toString() ?: "") }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }

    var descriptionError by remember { mutableStateOf(false) }
    var kmError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        descriptionError = description.isBlank()
        kmError = type == RecordType.MAINTENANCE && km.isBlank()
        return !descriptionError && !kmError
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Header ──────────────────────────────────────────────
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

        // ── Type selector ────────────────────────────────────────
        RecordSectionHeader(stringResource(R.string.record_type_label))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordType.entries.forEach { recordType ->
                FilterChip(
                    selected = type == recordType,
                    onClick = { type = recordType; kmError = false },
                    label = {
                        Text(
                            text = stringResource(recordType.labelRes()),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // ── Date ─────────────────────────────────────────────────
        RecordSectionHeader(stringResource(R.string.record_date))
        RecordDatePickerField(
            dateMs = dateMs,
            onDateSelected = { dateMs = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Description (required) ───────────────────────────────
        RecordSectionHeader(stringResource(R.string.record_description))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it; descriptionError = false },
            label = { Text(stringResource(R.string.record_description)) },
            isError = descriptionError,
            supportingText = if (descriptionError) {
                { Text(stringResource(R.string.error_field_required)) }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            maxLines = 3,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Km ───────────────────────────────────────────────────
        RecordSectionHeader(
            if (type == RecordType.MAINTENANCE) stringResource(R.string.record_km)
            else stringResource(R.string.record_km_optional)
        )
        OutlinedTextField(
            value = km,
            onValueChange = { km = it; kmError = false },
            label = {
                Text(
                    if (type == RecordType.MAINTENANCE) stringResource(R.string.record_km)
                    else stringResource(R.string.record_km_optional)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Cost (optional) ──────────────────────────────────────
        RecordSectionHeader(stringResource(R.string.record_cost_optional))
        OutlinedTextField(
            value = cost,
            onValueChange = { cost = it },
            label = { Text(stringResource(R.string.record_cost_optional)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Notes (optional) ─────────────────────────────────────
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.record_notes)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            maxLines = 4,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Save button ──────────────────────────────────────────
        Button(
            onClick = {
                if (validate()) {
                    val record = MaintenanceRecord(
                        id = existingRecord?.id ?: 0,
                        carId = carId,
                        type = type,
                        date = dateMs,
                        description = description.trim(),
                        km = km.trim().toIntOrNull(),
                        costAmount = cost.trim().toDoubleOrNull(),
                        notes = notes.trim().ifBlank { null },
                        createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(record)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
}

private fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAINTENANCE -> R.string.record_type_maintenance
    RecordType.REPAIR      -> R.string.record_type_repair
    RecordType.WEAR        -> R.string.record_type_wear
}

@Composable
private fun RecordSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDatePickerField(
    dateMs: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { showPicker = true },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.record_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateMs.toFormattedDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateSelected(it) }
                    showPicker = false
                }) { Text(stringResource(R.string.btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
