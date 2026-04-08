package com.hananelsabag.autocare.presentation.screens.cars

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.presentation.components.ImageCropSheet
import com.hananelsabag.autocare.util.CAR_MAKES
import com.hananelsabag.autocare.util.ThousandsVisualTransformation
import com.hananelsabag.autocare.util.modelsForMake
import com.hananelsabag.autocare.util.toFormattedDate
import java.io.File

// ── Color data ───────────────────────────────────────────────────────────────

// key is the value stored in DB (language-agnostic); nameRes is the localized display name
private data class CarColorOption(val key: String, val nameRes: Int, val displayColor: Color)

private val CAR_COLOR_OPTIONS = listOf(
    CarColorOption("white",  R.string.color_white,  Color.White),
    CarColorOption("black",  R.string.color_black,  Color(0xFF1A1A1A)),
    CarColorOption("gray",   R.string.color_gray,   Color(0xFF757575)),
    CarColorOption("silver", R.string.color_silver, Color(0xFFC0C0C0)),
    CarColorOption("red",    R.string.color_red,    Color(0xFFE53935)),
    CarColorOption("blue",   R.string.color_blue,   Color(0xFF1E88E5)),
    CarColorOption("green",  R.string.color_green,  Color(0xFF43A047)),
    CarColorOption("yellow", R.string.color_yellow, Color(0xFFFDD835)),
    CarColorOption("orange", R.string.color_orange, Color(0xFFFF6D00)),
    CarColorOption("brown",  R.string.color_brown,  Color(0xFF6D4C41)),
    CarColorOption("purple", R.string.color_purple, Color(0xFF8E24AA)),
    CarColorOption("pink",   R.string.color_pink,   Color(0xFFE91E63)),
    CarColorOption("beige",  R.string.color_beige,  Color(0xFFF5F0DC)),
    CarColorOption("gold",   R.string.color_gold,   Color(0xFFFFCA28)),
    CarColorOption("navy",   R.string.color_navy,   Color(0xFF1A237E)),
)

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Formats raw digit string (no dashes) into Israeli license plate display format.
 * 7 digits → XX-XXX-XX, 8 digits → XXX-XX-XXX
 */
private fun formatPlateDigits(digits: String): String = when (digits.length) {
    in 0..2 -> digits
    in 3..5 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
    in 6..7 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}"
    8       -> "${digits.substring(0, 3)}-${digits.substring(3, 5)}-${digits.substring(5, 8)}"
    else    -> digits
}

/**
 * VisualTransformation that displays raw digits as a formatted Israeli license plate.
 * The underlying value stays as digits-only; dashes are visual-only.
 * Also forces LTR direction so the plate renders correctly in an RTL layout.
 */
private object LicensePlateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(8)
        val formatted = formatPlateDigits(digits)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (formatted.isEmpty()) return 0
                var digitsSeen = 0
                for (i in formatted.indices) {
                    if (formatted[i] != '-') {
                        if (digitsSeen == offset) return i
                        digitsSeen++
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (formatted.isEmpty()) return 0
                var digitCount = 0
                for (i in 0 until minOf(offset, formatted.length)) {
                    if (formatted[i] != '-') digitCount++
                }
                return minOf(digitCount, digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/** Creates a temp file in cacheDir and returns a FileProvider URI for camera capture. */
private fun createTempCameraUri(context: Context): Uri {
    val file = File.createTempFile("camera_photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ── Sheet content ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarSheetContent(
    viewModel: AddCarViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Holds the picked URI before cropping — null means no crop sheet open
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceMenu by remember { mutableStateOf(false) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            pendingCropUri = it
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) { cameraPhotoUri?.let { pendingCropUri = it } }
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

        // ── Photo picker ─────────────────────────────────────────────
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
                .clickable { showPhotoSourceMenu = true },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.photoUri != null) {
                AsyncImage(
                    model = viewModel.photoUri,
                    contentDescription = stringResource(R.string.content_description_car_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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

            DropdownMenu(
                expanded = showPhotoSourceMenu,
                onDismissRequest = { showPhotoSourceMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_option_camera)) },
                    leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                    onClick = {
                        showPhotoSourceMenu = false
                        val uri = createTempCameraUri(context)
                        cameraPhotoUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_option_gallery)) },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        showPhotoSourceMenu = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        // ── Color strip — compact, right below photo ──────────────────
        CompactColorStrip(
            selectedColor = viewModel.color,
            onColorSelected = { viewModel.color = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // ── Required fields ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.add_car_section_required))

        // Make dropdown
        ExposedDropdownMenuBox(
            expanded = makeExpanded && filteredMakes.isNotEmpty(),
            onExpandedChange = { makeExpanded = it; if (it) keyboardController?.hide() },
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
                    val displayMakes = filteredMakes.take(10)
                    displayMakes.forEach { make ->
                        DropdownMenuItem(
                            text = { Text(make, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                viewModel.make = make
                                viewModel.model = ""
                                makeExpanded = false
                            }
                        )
                    }
                    if (filteredMakes.size > 10) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "ועוד ${filteredMakes.size - 10}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { makeExpanded = false },
                            enabled = false
                        )
                    }
                }
            }
        }

        // Model dropdown
        ExposedDropdownMenuBox(
            expanded = modelExpanded && filteredModels.isNotEmpty(),
            onExpandedChange = { modelExpanded = it; if (it) keyboardController?.hide() },
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
                    val displayModels = filteredModels.take(10)
                    displayModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                viewModel.model = model
                                modelExpanded = false
                            }
                        )
                    }
                    if (filteredModels.size > 10) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "ועוד ${filteredModels.size - 10}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { modelExpanded = false },
                            enabled = false
                        )
                    }
                }
            }
        }

        // Year quick-pick chips — last 7 years
        val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
        val yearChips = remember(currentYear) { (0..6).map { (currentYear - it).toString() } }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            yearChips.forEach { chipYear ->
                FilterChip(
                    selected = viewModel.year == chipYear,
                    onClick = { viewModel.year = chipYear },
                    label = { Text(chipYear, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Year + License plate row
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CarFormField(
                value = viewModel.year,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) viewModel.year = it },
                label = stringResource(R.string.add_car_year),
                error = viewModel.yearError,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            CarFormField(
                value = viewModel.licensePlate,
                onValueChange = { viewModel.licensePlate = it.filter { c -> c.isDigit() }.take(8) },
                label = stringResource(R.string.add_car_license_plate),
                error = viewModel.licensePlateError,
                keyboardType = KeyboardType.Number,
                visualTransformation = LicensePlateVisualTransformation,
                textStyle = TextStyle(textDirection = TextDirection.Ltr),
                modifier = Modifier.weight(1.6f)
            )
        }

        // ── Optional fields ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.add_car_section_optional))

        // KM field — digits only, max 6, thousands formatting, helper text
        CarFormField(
            value = viewModel.currentKm,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(6)
                viewModel.currentKm = digits
            },
            label = stringResource(R.string.add_car_current_km),
            supportingText = stringResource(R.string.add_car_km_supporting),
            keyboardType = KeyboardType.Number,
            visualTransformation = ThousandsVisualTransformation,
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

        // ── Notes — with character counter ───────────────────────────
        val notesMaxLength = 500
        OutlinedTextField(
            value = viewModel.notes,
            onValueChange = { if (it.length <= notesMaxLength) viewModel.notes = it },
            label = { Text(stringResource(R.string.add_car_notes)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            maxLines = 4,
            minLines = 3,
            supportingText = {
                Text(
                    text = "${viewModel.notes.length}/$notesMaxLength",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        )

        // ── Save ─────────────────────────────────────────────────────
        val isFormComplete = viewModel.make.isNotBlank() &&
            viewModel.model.isNotBlank() &&
            viewModel.year.isNotBlank() &&
            viewModel.licensePlate.isNotBlank()

        Button(
            onClick = { viewModel.save(onSaved) },
            enabled = isFormComplete,
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

    // ── Crop sheet — shown after photo is picked ──────────────────
    pendingCropUri?.let { cropUri ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { pendingCropUri = null },
            sheetState = sheetState
        ) {
            ImageCropSheet(
                uri = cropUri,
                onConfirm = { croppedUri ->
                    viewModel.photoUri = croppedUri.toString()
                    pendingCropUri = null
                },
                onDismiss = { pendingCropUri = null }
            )
        }
    }
}

// ── Compact color strip — placed right below the photo ───────────────────────

@Composable
private fun CompactColorStrip(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCustomField by remember { mutableStateOf(false) }

    // If the current color is not a known key, we're in "custom" mode
    val isCustom = selectedColor.isNotBlank() &&
        CAR_COLOR_OPTIONS.none { it.key.equals(selectedColor, ignoreCase = true) }

    // Show custom field if the stored value is already custom, or user tapped "אחר"
    val customFieldVisible = showCustomField || isCustom

    Column(modifier = modifier.fillMaxWidth()) {
        // Row: label + selected color name
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.add_car_color_picker_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Show selected color name as a small chip
            if (selectedColor.isNotBlank()) {
                val displayName = CAR_COLOR_OPTIONS
                    .find { it.key.equals(selectedColor, ignoreCase = true) }
                    ?.let { context.getString(it.nameRes) }
                    ?: selectedColor
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Circles + "other" button
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CAR_COLOR_OPTIONS.forEach { option ->
                val isSelected = selectedColor.equals(option.key, ignoreCase = true)
                CompactColorCircle(
                    option = option,
                    isSelected = isSelected,
                    onClick = {
                        onColorSelected(option.key)
                        showCustomField = false
                    }
                )
            }
            // "אחר" button — pen icon, shows text field
            val isOtherActive = customFieldVisible
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOtherActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable {
                        showCustomField = !showCustomField
                        if (!showCustomField && isCustom) onColorSelected("")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.add_car_color_other),
                    tint = if (isOtherActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Animated custom text field
        AnimatedVisibility(
            visible = customFieldVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = if (isCustom) selectedColor else "",
                onValueChange = { typed ->
                    val matched = CAR_COLOR_OPTIONS.firstOrNull { option ->
                        context.getString(option.nameRes).equals(typed, ignoreCase = true)
                    }
                    onColorSelected(matched?.key ?: typed)
                },
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
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun CompactColorCircle(
    option: CarColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
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
                .size(if (isSelected) 30.dp else 34.dp)
                .clip(CircleShape)
                .background(option.displayColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
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
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    maxLines: Int = 1,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = TextStyle.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        supportingText = when {
            error != null -> {
                {
                    Text(
                        text = when (error) {
                            FieldError.Required -> stringResource(R.string.error_field_required)
                            FieldError.InvalidYear -> stringResource(R.string.error_year_invalid)
                            FieldError.InvalidLicensePlate -> stringResource(R.string.error_invalid_license_plate)
                        }
                    )
                }
            }
            supportingText != null -> {
                {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> null
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
