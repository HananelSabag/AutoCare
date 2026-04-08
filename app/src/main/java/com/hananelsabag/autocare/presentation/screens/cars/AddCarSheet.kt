package com.hananelsabag.autocare.presentation.screens.cars

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.util.CAR_MAKES
import com.hananelsabag.autocare.util.modelsForMake
import com.hananelsabag.autocare.util.toFormattedDate

// ── Color data ───────────────────────────────────────────────────────────────

private data class CarColorOption(val hebrewName: String, val displayColor: Color)

private val CAR_COLOR_OPTIONS = listOf(
    CarColorOption("לבן", Color.White),
    CarColorOption("שחור", Color(0xFF1A1A1A)),
    CarColorOption("אפור", Color(0xFF757575)),
    CarColorOption("כסוף", Color(0xFFC0C0C0)),
    CarColorOption("אדום", Color(0xFFE53935)),
    CarColorOption("כחול", Color(0xFF1E88E5)),
    CarColorOption("ירוק", Color(0xFF43A047)),
    CarColorOption("צהוב", Color(0xFFFDD835)),
    CarColorOption("כתום", Color(0xFFFF6D00)),
    CarColorOption("חום", Color(0xFF6D4C41)),
    CarColorOption("סגול", Color(0xFF8E24AA)),
    CarColorOption("ורוד", Color(0xFFE91E63)),
    CarColorOption("בז'", Color(0xFFF5F0DC)),
    CarColorOption("זהב", Color(0xFFFFCA28)),
    CarColorOption("נייבי", Color(0xFF1A237E)),
)

// ── Sheet content ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarSheetContent(
    viewModel: AddCarViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            viewModel.photoUri = it.toString()
        }
    }

    var makeExpanded by remember { mutableStateOf(false) }
    val makeQuery = viewModel.make
    val filteredMakes = remember(makeQuery) {
        if (makeQuery.isBlank()) CAR_MAKES
        else CAR_MAKES.filter { it.contains(makeQuery, ignoreCase = true) }
    }

    var modelExpanded by remember { mutableStateOf(false) }
    val modelQuery = viewModel.model
    val modelSuggestions = remember(viewModel.make) { modelsForMake(viewModel.make) }
    val filteredModels = remember(modelQuery, modelSuggestions) {
        if (modelQuery.isBlank()) modelSuggestions
        else modelSuggestions.filter { it.contains(modelQuery, ignoreCase = true) }
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
                    if (viewModel.isEditing) R.string.edit_car_title else R.string.add_car_title
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.btn_cancel)
                )
            }
        }

        // ── Photo picker — premium, inviting ─────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(180.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.photoUri != null) {
                AsyncImage(
                    model = viewModel.photoUri,
                    contentDescription = stringResource(R.string.content_description_car_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle overlay + change label
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_car_photo_tap_change),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = stringResource(R.string.add_car_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.add_car_photo_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Required fields ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.add_car_section_required))

        // Make dropdown
        ExposedDropdownMenuBox(
            expanded = makeExpanded && filteredMakes.isNotEmpty(),
            onExpandedChange = { makeExpanded = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = viewModel.make,
                onValueChange = {
                    viewModel.make = it
                    makeExpanded = true
                    if (viewModel.model.isNotBlank() && modelsForMake(it).isNotEmpty()) {
                        viewModel.model = ""
                    }
                },
                label = { Text(stringResource(R.string.add_car_make)) },
                isError = viewModel.makeError != null,
                supportingText = viewModel.makeError?.let {
                    { Text(stringResource(R.string.error_field_required)) }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = makeExpanded && filteredMakes.isNotEmpty()
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
                shape = MaterialTheme.shapes.medium
            )
            if (filteredMakes.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = makeExpanded,
                    onDismissRequest = { makeExpanded = false }
                ) {
                    filteredMakes.take(8).forEach { make ->
                        DropdownMenuItem(
                            text = { Text(make, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                viewModel.make = make
                                viewModel.model = ""
                                makeExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Model dropdown
        ExposedDropdownMenuBox(
            expanded = modelExpanded && filteredModels.isNotEmpty(),
            onExpandedChange = { modelExpanded = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = viewModel.model,
                onValueChange = {
                    viewModel.model = it
                    modelExpanded = true
                },
                label = { Text(stringResource(R.string.add_car_model)) },
                placeholder = {
                    Text(
                        stringResource(R.string.add_car_model_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                isError = viewModel.modelError != null,
                supportingText = viewModel.modelError?.let {
                    { Text(stringResource(R.string.error_field_required)) }
                },
                trailingIcon = if (filteredModels.isNotEmpty()) {
                    {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = modelExpanded && filteredModels.isNotEmpty()
                        )
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (filteredModels.isNotEmpty())
                            Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
                        else Modifier
                    ),
                shape = MaterialTheme.shapes.medium
            )
            if (filteredModels.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    filteredModels.take(8).forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                viewModel.model = model
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Year + License plate
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CarFormField(
                value = viewModel.year,
                onValueChange = { if (it.length <= 4) viewModel.year = it },
                label = stringResource(R.string.add_car_year),
                error = viewModel.yearError,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CarFormField(
                value = viewModel.licensePlate,
                onValueChange = { viewModel.licensePlate = it },
                label = stringResource(R.string.add_car_license_plate),
                error = viewModel.licensePlateError,
                modifier = Modifier.weight(1.6f)
            )
        }

        // ── Optional fields ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.add_car_section_optional))

        // Color picker
        Text(
            text = stringResource(R.string.add_car_color_picker_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
        )
        ColorCirclePicker(
            selectedColor = viewModel.color,
            onColorSelected = { viewModel.color = it }
        )

        // KM field
        CarFormField(
            value = viewModel.currentKm,
            onValueChange = { viewModel.currentKm = it },
            label = stringResource(R.string.add_car_current_km),
            keyboardType = KeyboardType.Number,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Date fields ──────────────────────────────────────────────
        SectionHeader(stringResource(R.string.add_car_section_dates))

        DatePickerField(
            label = stringResource(R.string.add_car_test_expiry),
            dateMs = viewModel.testExpiryDate,
            onDateSelected = { viewModel.testExpiryDate = it },
            onDateCleared = { viewModel.testExpiryDate = null },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        DatePickerField(
            label = stringResource(R.string.add_car_insurance_expiry),
            dateMs = viewModel.insuranceExpiryDate,
            onDateSelected = { viewModel.insuranceExpiryDate = it },
            onDateCleared = { viewModel.insuranceExpiryDate = null },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Notes ────────────────────────────────────────────────────
        CarFormField(
            value = viewModel.notes,
            onValueChange = { viewModel.notes = it },
            label = stringResource(R.string.add_car_notes),
            imeAction = ImeAction.Done,
            maxLines = 4,
            minLines = 3,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ── Save ─────────────────────────────────────────────────────
        Button(
            onClick = { viewModel.save(onSaved) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.add_car_save),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Color circle picker ───────────────────────────────────────────────────────

@Composable
private fun ColorCirclePicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    // Circles — wrap across rows (no horizontal scrolling)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CAR_COLOR_OPTIONS.forEach { option ->
            val isSelected = selectedColor.trim().equals(option.hebrewName, ignoreCase = true)
            ColorCircle(
                option = option,
                isSelected = isSelected,
                onClick = { onColorSelected(option.hebrewName) }
            )
        }
    }

    // Text field — always visible; shows the current value; allows custom entry
    OutlinedTextField(
        value = selectedColor,
        onValueChange = onColorSelected,
        label = { Text(stringResource(R.string.add_car_color)) },
        placeholder = {
            Text(
                text = stringResource(R.string.add_car_color_custom_hint),
                style = MaterialTheme.typography.bodySmall
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ColorCircle(
    option: CarColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Outer ring: primary color when selected, subtle outline otherwise
    // Inner swatch: the actual color, slightly inset to reveal the ring
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 34.dp else 38.dp)
                .clip(CircleShape)
                .background(option.displayColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Scrim + check so it reads on any color
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CarFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: FieldError? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    maxLines: Int = 1,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { err ->
            {
                Text(
                    text = when (err) {
                        FieldError.Required -> stringResource(R.string.error_field_required)
                        FieldError.InvalidYear -> stringResource(R.string.error_year_invalid)
                    }
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = maxLines == 1,
        maxLines = maxLines,
        minLines = minLines,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    dateMs: Long?,
    onDateSelected: (Long) -> Unit,
    onDateCleared: () -> Unit,
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
                tint = if (dateMs != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateMs?.toFormattedDate() ?: stringResource(R.string.date_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dateMs != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dateMs != null) {
                IconButton(onClick = onDateCleared, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
