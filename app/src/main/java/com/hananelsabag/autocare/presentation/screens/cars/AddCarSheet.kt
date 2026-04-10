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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCarSheetContent(
    viewModel: AddCarViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

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

    var showMakePicker by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

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

        // Make picker field
        PickerField(
            label = stringResource(R.string.add_car_make),
            value = viewModel.make.ifBlank { null },
            placeholder = stringResource(R.string.add_car_make_select),
            error = viewModel.makeError != null,
            errorText = stringResource(R.string.error_field_required),
            onClick = { showMakePicker = true },
            onClear = { viewModel.make = ""; viewModel.model = "" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Model picker field
        PickerField(
            label = stringResource(R.string.add_car_model),
            value = viewModel.model.ifBlank { null },
            placeholder = if (viewModel.make.isBlank())
                stringResource(R.string.add_car_picker_select_make_first)
            else
                stringResource(R.string.add_car_model_hint),
            error = viewModel.modelError != null,
            errorText = stringResource(R.string.error_field_required),
            enabled = viewModel.make.isNotBlank(),
            onClick = { showModelPicker = true },
            onClear = { viewModel.model = "" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Year quick-pick chips — last 7 years, collapsible
        val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
        val yearChips = remember(currentYear) { (0..6).map { (currentYear - it).toString() } }
        var yearChipsExpanded by remember { mutableStateOf(true) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            tonalElevation = 0.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { yearChipsExpanded = !yearChipsExpanded }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_car_year_quick_pick),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!yearChipsExpanded && viewModel.year.isNotBlank()) {
                        Text(
                            text = viewModel.year,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = if (yearChipsExpanded) Icons.Filled.KeyboardArrowUp
                                      else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(
                    visible = yearChipsExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        yearChips.forEach { chipYear ->
                            FilterChip(
                                selected = viewModel.year == chipYear,
                                onClick = { viewModel.year = chipYear },
                                label = {
                                    Text(chipYear, style = MaterialTheme.typography.labelMedium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = if (viewModel.year == chipYear)
                                    FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = true,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        selectedBorderWidth = 1.5.dp
                                    )
                                else
                                    FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
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

    // ── Make picker sheet ─────────────────────────────────────────
    if (showMakePicker) {
        SearchPickerSheet(
            title = stringResource(R.string.add_car_make),
            searchHint = stringResource(R.string.add_car_picker_search_make),
            items = CAR_MAKES,
            selectedItem = viewModel.make,
            onItemSelected = { make ->
                if (!make.equals(viewModel.make, ignoreCase = true)) {
                    viewModel.make = make
                    // Clear model if it came from suggestions for the previous make
                    if (viewModel.model.isNotBlank() && modelsForMake(make).isNotEmpty()) {
                        viewModel.model = ""
                    }
                }
            },
            onDismiss = { showMakePicker = false }
        )
    }

    // ── Model picker sheet ────────────────────────────────────────
    if (showModelPicker) {
        SearchPickerSheet(
            title = stringResource(R.string.add_car_model),
            searchHint = stringResource(R.string.add_car_picker_search_model),
            items = modelsForMake(viewModel.make),
            selectedItem = viewModel.model,
            onItemSelected = { model -> viewModel.model = model },
            onDismiss = { showModelPicker = false }
        )
    }
}

// ── Compact color strip — placed right below the photo ───────────────────────

@OptIn(ExperimentalLayoutApi::class)
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

// ── PickerField — tappable card that opens a bottom-sheet picker ──────────────

@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    error: Boolean,
    errorText: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            border = when {
                error -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                else -> CardDefaults.outlinedCardBorder(enabled)
            }
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            error -> MaterialTheme.colorScheme.error
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value ?: placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            value != null -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                if (value != null && enabled && onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
        if (error) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ── SearchPickerSheet — searchable bottom-sheet list picker ──────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchPickerSheet(
    title: String,
    searchHint: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickerScope = rememberCoroutineScope()

    fun selectAndClose(value: String) {
        onItemSelected(value)
        pickerScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    var query by remember { mutableStateOf("") }

    val filtered = remember(query, items) {
        if (query.isBlank()) items
        else items.filter { it.contains(query, ignoreCase = true) }
    }
    val typedNotInList = query.isNotBlank() &&
        filtered.none { it.equals(query.trim(), ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Search field — keyboard opens here, not on the main form
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = searchHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = if (query.isNotBlank()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Scrollable list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // "Add manually" row — shown when typed value doesn't match any item
                if (typedNotInList) {
                    item(key = "__custom__") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectAndClose(query.trim()) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = query.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.add_car_picker_add_manually),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                items(filtered, key = { it }) { item ->
                    val isSelected = item.equals(selectedItem, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { selectAndClose(item) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
