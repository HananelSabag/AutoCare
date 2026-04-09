package com.hananelsabag.autocare.presentation.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.ActivityNotFoundException
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.BuildConfig
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.theme.ThemeMode

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

    // File picker for JSON import
    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { exportViewModel.onImportFileSelected(it) } }

    val pdfSavedMsg     = stringResource(R.string.export_pdf_saved)
    val pdfOpenMsg      = stringResource(R.string.export_pdf_open)
    val backupSavedMsg  = stringResource(R.string.export_json_saved)
    val shareActionMsg  = stringResource(R.string.export_json_share)
    val importSuccessFmt = stringResource(R.string.import_success)
    val importErrorMsg  = stringResource(R.string.import_error)
    val exportErrorMsg  = stringResource(R.string.export_error)

    LaunchedEffect(exportViewModel) {
        exportViewModel.events.collect { event ->
            when (event) {
                is ExportEvent.PdfSaved -> {
                    val result = snackbarHostState.showSnackbar(
                        message     = pdfSavedMsg,
                        actionLabel = pdfOpenMsg,
                        duration    = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(event.uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: ActivityNotFoundException) { /* no PDF viewer installed */ }
                    }
                }
                is ExportEvent.ShareIntent -> {
                    context.startActivity(
                        Intent.createChooser(event.intent, null)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                is ExportEvent.BackupSaved -> {
                    val result = snackbarHostState.showSnackbar(
                        message     = backupSavedMsg,
                        actionLabel = shareActionMsg,
                        duration    = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        exportViewModel.onShareBackup(event.shareUri)
                    }
                }
                is ExportEvent.ImportSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = String.format(importSuccessFmt, event.carsImported)
                    )
                }
                is ExportEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        if (event.tag == "import_error") importErrorMsg else exportErrorMsg
                    )
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            // ── Hero Header ────────────────────────────────────────────
            SettingsHero()

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Theme ──────────────────────────────────────────────
                SettingsCard(
                    title = stringResource(R.string.settings_section_theme),
                    icon = Icons.Outlined.Palette,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    PillSelectorRow(
                        options = ThemeChoice.entries.map { Triple(it.mode, it.labelRes, it.icon) },
                        selected = { it == themeMode },
                        onSelect = { viewModel.setThemeMode(it) }
                    )
                }

                // ── Language ───────────────────────────────────────────
                SettingsCard(
                    title = stringResource(R.string.settings_section_language),
                    icon = Icons.Outlined.Language,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                ) {
                    PillSelectorRow(
                        options = LanguageChoice.entries.map { Triple(it.tag, it.labelRes, it.icon) },
                        selected = { it == language },
                        onSelect = { tag ->
                            viewModel.setLanguage(tag)
                            when (tag) {
                                "system" -> AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.getEmptyLocaleList()
                                )
                                else -> AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(tag)
                                )
                            }
                        }
                    )
                }

                // ── Notifications ──────────────────────────────────────
                SettingsCard(
                    title = stringResource(R.string.settings_section_notifications),
                    icon = if (notificationsGranted) Icons.Outlined.NotificationsActive
                           else Icons.Outlined.NotificationsOff,
                    iconContainerColor = if (notificationsGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    iconTint = if (notificationsGranted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                ) {
                    if (notificationsGranted) {
                        NotificationsActiveRow()
                    } else {
                        NotificationsDisabledRow(
                            onEnable = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                        )
                    }
                }

                // ── Export / Import ────────────────────────────────────
                SettingsCard(
                    title = stringResource(R.string.settings_section_export),
                    icon = Icons.Outlined.FileDownload,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExportTile(
                            icon = Icons.Outlined.Description,
                            label = stringResource(R.string.settings_export_pdf),
                            description = stringResource(R.string.settings_export_pdf_desc),
                            isLoading = exportState.isGeneratingPdf,
                            onClick = { exportViewModel.onPdfExportRequested() }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ExportTile(
                            icon = Icons.Outlined.FileDownload,
                            label = stringResource(R.string.settings_export_json),
                            description = stringResource(R.string.settings_export_json_desc),
                            isLoading = exportState.isGeneratingJson,
                            onClick = { exportViewModel.onJsonExportRequested() }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ExportTile(
                            icon = Icons.Outlined.FileUpload,
                            label = stringResource(R.string.settings_import_json),
                            description = stringResource(R.string.settings_import_json_desc),
                            isLoading = exportState.isImporting,
                            onClick = { importFileLauncher.launch("application/json") }
                        )
                    }
                }

                // ── About ──────────────────────────────────────────────
                SettingsCard(
                    title = stringResource(R.string.settings_section_about),
                    icon = Icons.Outlined.Info,
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    AboutRow(
                        icon = Icons.Outlined.Info,
                        label = stringResource(R.string.settings_about_version),
                        value = BuildConfig.VERSION_NAME
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    AboutRow(
                        icon = Icons.Outlined.Person,
                        label = stringResource(R.string.settings_about_developer),
                        value = stringResource(R.string.settings_about_developer_name)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Car Picker Sheet ───────────────────────────────────────────────────────
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

}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // App logo — right side
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(40.dp),
            shape = CircleShape,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Image(
                painter = painterResource(R.drawable.autocare_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp)
            )
        }

        // Title + version — bottom-start
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ── Settings card ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

// ── Pill selector (theme / language) ─────────────────────────────────────────

@Composable
private fun <T> PillSelectorRow(
    options: List<Triple<T, Int, ImageVector>>,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, labelRes, icon) ->
            val isSelected = selected(value)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                              else Color.Transparent,
                animationSpec = tween(200),
                label = "pillBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "pillContent"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ── Notifications rows ────────────────────────────────────────────────────────

@Composable
private fun NotificationsActiveRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.settings_notifications_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_notifications_active),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun NotificationsDisabledRow(onEnable: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.settings_notifications_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_notifications_disabled),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Button(
            onClick = onEnable,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.settings_notifications_enable),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Export tile ───────────────────────────────────────────────────────────────

@Composable
private fun ExportTile(
    icon: ImageVector,
    label: String,
    description: String,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && !isLoading)
                    Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── About row ─────────────────────────────────────────────────────────────────

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Car picker sheet ──────────────────────────────────────────────────────────

@Composable
private fun CarPickerSheetContent(cars: List<Car>, onCarSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.export_pick_car_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${car.year} · ${car.licensePlate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

// ── Enum helpers ──────────────────────────────────────────────────────────────

private enum class ThemeChoice(val mode: ThemeMode, val labelRes: Int, val icon: ImageVector) {
    SYSTEM(ThemeMode.SYSTEM, R.string.settings_theme_system, Icons.Outlined.BrightnessAuto),
    LIGHT(ThemeMode.LIGHT,   R.string.settings_theme_light,  Icons.Outlined.LightMode),
    DARK(ThemeMode.DARK,     R.string.settings_theme_dark,   Icons.Outlined.DarkMode)
}

private enum class LanguageChoice(val tag: String, val labelRes: Int, val icon: ImageVector) {
    SYSTEM("system", R.string.settings_language_system,  Icons.Outlined.PhoneAndroid),
    HEBREW("iw",     R.string.settings_language_hebrew,  Icons.Outlined.Language),
    ENGLISH("en",    R.string.settings_language_english, Icons.Outlined.Language)
}
