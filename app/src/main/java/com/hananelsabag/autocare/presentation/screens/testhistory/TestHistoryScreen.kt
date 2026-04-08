package com.hananelsabag.autocare.presentation.screens.testhistory

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusGreenContainer
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusRedContainer
import com.hananelsabag.autocare.util.toFormattedDate
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryScreen(
    carId: Int,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<TestHistoryViewModel>()
    LaunchedEffect(carId) { viewModel.init(carId) }

    val records by viewModel.records.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<TestRecord?>(null) }
    var recordToEdit by remember { mutableStateOf<TestRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<TestRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.test_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.test_history_add)
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        if (records.isEmpty()) {
            TestHistoryEmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    TestRecordCard(
                        record = record,
                        onClick = { selectedRecord = record }
                    )
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
            AddTestRecordSheet(
                carId = carId,
                existingRecord = recordToEdit,
                onSave = { record ->
                    if (recordToEdit != null) viewModel.update(record) else viewModel.insert(record)
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
            TestRecordDetailSheet(
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
            title = { Text(stringResource(R.string.test_record_delete_title)) },
            text = { Text(stringResource(R.string.test_record_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(record); recordToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TestHistoryEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.test_history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.test_history_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TestRecordCard(record: TestRecord, onClick: () -> Unit) {
    val passColor = if (record.passed) StatusGreen else StatusRed
    val passContainer = if (record.passed) StatusGreenContainer else StatusRedContainer
    val passIcon = if (record.passed) Icons.Filled.CheckCircle else Icons.Outlined.Cancel
    val passLabel = if (record.passed) R.string.test_history_passed else R.string.test_history_failed

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .background(passColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = passContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = passIcon,
                            contentDescription = null,
                            tint = passColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.date.toFormattedDate(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (!record.notes.isNullOrBlank()) {
                        Text(
                            text = record.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    color = passContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = stringResource(passLabel),
                        style = MaterialTheme.typography.labelSmall,
                        color = passColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestRecordDetailSheet(
    record: TestRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val passColor = if (record.passed) StatusGreen else StatusRed
    val passContainer = if (record.passed) StatusGreenContainer else StatusRedContainer
    val passLabel = if (record.passed) R.string.test_history_passed else R.string.test_history_failed

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
                color = passContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = null,
                        tint = passColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.test_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.date.toFormattedDate(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(color = passContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    text = stringResource(passLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = passColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (!record.notes.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.add_test_record_notes).removeSuffix(" (אופציונלי)").removeSuffix(" (optional)"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = record.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        record.certificateUri?.let { uri ->
            Text(
                text = stringResource(R.string.test_record_certificate_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            AsyncImage(
                model = uri,
                contentDescription = stringResource(R.string.test_record_certificate_label),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTestRecordSheet(
    carId: Int,
    existingRecord: TestRecord?,
    onSave: (TestRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingRecord != null
    var dateMs by remember {
        mutableStateOf(
            existingRecord?.date
                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
    var passed by remember { mutableStateOf(existingRecord?.passed ?: true) }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }
    var certificateUri by remember { mutableStateOf(existingRecord?.certificateUri) }
    var showDatePicker by remember { mutableStateOf(false) }

    val certificatePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            certificateUri = it.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(if (isEditing) R.string.edit_test_record_title else R.string.add_test_record_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.btn_cancel))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date
        Text(
            text = stringResource(R.string.add_test_record_date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = dateMs.toFormattedDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Pass/Fail toggle
        Text(
            text = stringResource(R.string.add_test_record_passed_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val passLabel = if (passed) R.string.add_test_record_passed_yes else R.string.add_test_record_passed_no
            val passColor = if (passed) StatusGreen else StatusRed
            Text(
                text = stringResource(passLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = passColor,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = passed,
                onCheckedChange = { passed = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StatusGreen,
                    checkedTrackColor = StatusGreenContainer,
                    uncheckedThumbColor = StatusRed,
                    uncheckedTrackColor = StatusRedContainer
                )
            )
        }

        // Notes
        Text(
            text = stringResource(R.string.add_test_record_notes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.add_test_record_notes)) },
            maxLines = 4,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        )

        // Certificate photo
        Text(
            text = stringResource(R.string.test_record_certificate_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        if (certificateUri != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                AsyncImage(
                    model = certificateUri,
                    contentDescription = stringResource(R.string.test_record_certificate_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
                IconButton(
                    onClick = { certificateUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            OutlinedCard(
                onClick = {
                    certificatePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
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
                        text = stringResource(R.string.test_record_certificate_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Save button
        Button(
            onClick = {
                onSave(
                    TestRecord(
                        id = existingRecord?.id ?: 0,
                        carId = carId,
                        date = dateMs,
                        passed = passed,
                        notes = notes.trim().ifBlank { null },
                        certificateUri = certificateUri,
                        createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.add_test_record_save),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

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
