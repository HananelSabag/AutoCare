package com.hananelsabag.autocare.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.BuildConfig
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val exportViewModel = hiltViewModel<ExportViewModel>()

    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val cars by exportViewModel.cars.collectAsState()
    val exportState by exportViewModel.uiState.collectAsState()

    val carPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    // Collect share intents and launch them
    LaunchedEffect(exportViewModel) {
        exportViewModel.shareIntent.collect { intent ->
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    // Show error snackbar
    val errorMsg = stringResource(R.string.export_error)
    LaunchedEffect(exportState.error) {
        if (exportState.error != null) {
            snackbarHostState.showSnackbar(errorMsg)
            exportViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.screen_settings_title)) })
        },
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Theme ──────────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.settings_section_theme),
                icon = Icons.Outlined.Palette
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = themeMode == choice.mode,
                            onClick = { viewModel.setThemeMode(choice.mode) },
                            label = {
                                Text(
                                    text = stringResource(choice.labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = choice.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Language ───────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.settings_section_language),
                icon = Icons.Outlined.Language
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = language == choice.tag,
                            onClick = {
                                viewModel.setLanguage(choice.tag)
                                when (choice.tag) {
                                    "system" -> AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.getEmptyLocaleList()
                                    )
                                    else -> AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(choice.tag)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(choice.labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = choice.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Notifications ──────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.settings_section_notifications),
                icon = Icons.Outlined.Notifications
            ) {
                if (notificationsGranted) {
                    SettingsInfoRow(
                        icon = Icons.Outlined.Notifications,
                        label = stringResource(R.string.settings_notifications_label),
                        value = stringResource(R.string.settings_notifications_active),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    SettingsInfoRow(
                        icon = Icons.Outlined.NotificationsOff,
                        label = stringResource(R.string.settings_notifications_label),
                        value = stringResource(R.string.settings_notifications_disabled),
                        valueColor = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(stringResource(R.string.settings_notifications_enable))
                    }
                }
            }

            // ── Export ─────────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.settings_section_export),
                icon = Icons.Outlined.FileDownload
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // PDF — active
                    ExportRow(
                        icon = Icons.Outlined.Description,
                        label = stringResource(R.string.settings_export_pdf),
                        description = stringResource(R.string.settings_export_pdf_desc),
                        isLoading = exportState.isGeneratingPdf,
                        isComingSoon = false,
                        onClick = { exportViewModel.onPdfExportRequested() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    // Excel — still coming soon
                    ExportRow(
                        icon = Icons.Outlined.GridOn,
                        label = stringResource(R.string.settings_export_excel),
                        description = stringResource(R.string.settings_export_excel_desc),
                        isComingSoon = true
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    // JSON — active
                    ExportRow(
                        icon = Icons.Outlined.FileDownload,
                        label = stringResource(R.string.settings_export_json),
                        description = stringResource(R.string.settings_export_json_desc),
                        isLoading = exportState.isGeneratingJson,
                        isComingSoon = false,
                        onClick = { exportViewModel.onJsonExportRequested() }
                    )
                }
            }

            // ── About ──────────────────────────────────────────────────
            SettingsSection(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Outlined.Info
            ) {
                SettingsInfoRow(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.settings_about_version),
                    value = BuildConfig.VERSION_NAME
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsInfoRow(
                    icon = Icons.Outlined.Person,
                    label = stringResource(R.string.settings_about_developer),
                    value = stringResource(R.string.settings_about_developer_name),
                    clickable = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Car Picker Bottom Sheet ────────────────────────────────────────────────
    if (exportState.showCarPicker) {
        ModalBottomSheet(
            onDismissRequest = { exportViewModel.dismissCarPicker() },
            sheetState = carPickerSheetState
        ) {
            CarPickerSheetContent(
                cars = cars,
                onCarSelected = { exportViewModel.onCarSelectedForPdf(it) }
            )
        }
    }

    // ── Loading Dialog ────────────────────────────────────────────────────────
    if (exportState.isGeneratingPdf || exportState.isGeneratingJson) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text(
                        text = if (exportState.isGeneratingPdf)
                            stringResource(R.string.export_pdf_generating)
                        else
                            stringResource(R.string.export_json_generating),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ── Car Picker Sheet Content ──────────────────────────────────────────────────

@Composable
private fun CarPickerSheetContent(
    cars: List<Car>,
    onCarSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.export_pick_car_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        HorizontalDivider()
        cars.forEach { car ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCarSelected(car.id) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${car.year} · ${car.licensePlate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    clickable: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable {} else Modifier)
            .padding(vertical = if (clickable) 4.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

// ── Export row ────────────────────────────────────────────────────────────────

@Composable
private fun ExportRow(
    icon: ImageVector,
    label: String,
    description: String,
    isComingSoon: Boolean = false,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && !isComingSoon && !isLoading)
                    Modifier.clickable { onClick() }
                else
                    Modifier
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isComingSoon)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isComingSoon)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else if (isComingSoon) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Enum helpers ──────────────────────────────────────────────────────────────

private enum class ThemeChoice(val mode: ThemeMode, val labelRes: Int, val icon: ImageVector) {
    SYSTEM(ThemeMode.SYSTEM, R.string.settings_theme_system,  Icons.Outlined.BrightnessAuto),
    LIGHT( ThemeMode.LIGHT,  R.string.settings_theme_light,   Icons.Outlined.LightMode),
    DARK(  ThemeMode.DARK,   R.string.settings_theme_dark,    Icons.Outlined.DarkMode)
}

private enum class LanguageChoice(val tag: String, val labelRes: Int, val icon: ImageVector) {
    SYSTEM("system", R.string.settings_language_system,  Icons.Outlined.PhoneAndroid),
    HEBREW("iw",     R.string.settings_language_hebrew,  Icons.Outlined.Language),
    ENGLISH("en",    R.string.settings_language_english, Icons.Outlined.Language)
}
