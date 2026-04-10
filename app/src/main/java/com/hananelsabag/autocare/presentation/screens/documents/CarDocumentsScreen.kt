package com.hananelsabag.autocare.presentation.screens.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import com.hananelsabag.autocare.util.toFormattedDate
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDocumentsScreen(
    carId: Int,
    onBack: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val viewModel = hiltViewModel<CarDocumentsViewModel>()
    LaunchedEffect(carId) { viewModel.init(carId) }

    val testRecord by viewModel.activeTestRecord.collectAsState()
    val insuranceRecord by viewModel.activeInsuranceRecord.collectAsState()

    var editingType by remember { mutableStateOf<VehicleRecordType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.documents_title)) },
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
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VehicleRecordCard(
                    type = VehicleRecordType.TEST,
                    record = testRecord,
                    icon = Icons.Outlined.VerifiedUser,
                    sectionLabel = stringResource(R.string.documents_section_test),
                    onEditClick = { editingType = VehicleRecordType.TEST }
                )
            }
            item {
                VehicleRecordCard(
                    type = VehicleRecordType.INSURANCE,
                    record = insuranceRecord,
                    icon = Icons.Outlined.Security,
                    sectionLabel = stringResource(R.string.documents_section_insurance),
                    onEditClick = { editingType = VehicleRecordType.INSURANCE }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.documents_history_button))
                }
            }
        }
    }

    // Edit sheet
    editingType?.let { type ->
        val currentRecord = if (type == VehicleRecordType.TEST) testRecord else insuranceRecord
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingType = null },
            sheetState = sheetState
        ) {
            EditVehicleRecordSheet(
                carId = carId,
                type = type,
                existingRecord = currentRecord,
                onSave = { expiryDate, fileUri ->
                    viewModel.saveRecord(carId, type, expiryDate, fileUri)
                    editingType = null
                },
                onDismiss = { editingType = null }
            )
        }
    }
}

// ── Vehicle record card ────────────────────────────────────────────────────────

@Composable
private fun VehicleRecordCard(
    type: VehicleRecordType,
    record: VehicleRecord?,
    icon: ImageVector,
    sectionLabel: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = sectionLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onEditClick,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.documents_edit_record),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            if (record != null) {
                // Expiry date row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.documents_expiry_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record.expiryDate.toFormattedDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // File preview + open button
                if (record.fileUri != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DocumentFilePreview(
                        fileUri = record.fileUri,
                        uploadedAt = record.createdAt
                    )
                }
            } else {
                // Empty state
                Text(
                    text = stringResource(R.string.documents_no_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Document file preview ─────────────────────────────────────────────────────

@Composable
private fun DocumentFilePreview(fileUri: String, uploadedAt: Long) {
    val context = LocalContext.current
    val uri = remember(fileUri) { Uri.parse(fileUri) }
    val mimeType = remember(fileUri) {
        context.contentResolver.getType(uri) ?: "*/*"
    }
    val isImage = mimeType.startsWith("image/")

    Column {
        if (isImage) {
            // Image thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.documents_file_attached),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // PDF / unknown file indicator
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.documents_uploaded_on, uploadedAt.toFormattedDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
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
    }
}

// ── Edit sheet ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVehicleRecordSheet(
    carId: Int,
    type: VehicleRecordType,
    existingRecord: VehicleRecord?,
    onSave: (expiryDate: Long, fileUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val defaultDateMs = existingRecord?.expiryDate
        ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    var expiryDateMs by remember { mutableStateOf(defaultDateMs) }
    var fileUri by remember { mutableStateOf(existingRecord?.fileUri) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            fileUri = it.toString()
        }
    }

    val sheetTitle = stringResource(
        if (type == VehicleRecordType.TEST) R.string.documents_sheet_title_test
        else R.string.documents_sheet_title_insurance
    )

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
                text = sheetTitle,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Expiry date picker
        Text(
            text = stringResource(R.string.documents_expiry_label),
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
                    text = expiryDateMs.toFormattedDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Optional file attachment
        Text(
            text = stringResource(R.string.documents_attach_file),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (fileUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.documents_file_attached),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { fileUri = null }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.documents_remove_file),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(R.string.documents_replace_file))
            }
        } else {
            OutlinedCard(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.documents_attach_file),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Save
        Button(
            onClick = { onSave(expiryDateMs, fileUri) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.documents_save_record),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expiryDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { expiryDateMs = it }
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
