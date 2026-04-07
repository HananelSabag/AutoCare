package com.hananelsabag.autocare.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.BuildConfig
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.presentation.theme.ThemeMode

@Composable
fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current

    // Notification permission state
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Notifications don't need runtime permission below API 33
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Theme ────────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.settings_section_theme)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = themeMode == choice.mode,
                        onClick = { viewModel.setThemeMode(choice.mode) },
                        label = { Text(stringResource(choice.labelRes), style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            if (themeMode == choice.mode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = choice.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Language ─────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.settings_section_language)) {
            SettingsInfoRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.settings_language_current),
                value = stringResource(R.string.settings_language_hebrew)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_language_coming_soon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Notifications ────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(12.dp))

        // ── About ─────────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            SettingsInfoRow(
                label = stringResource(R.string.settings_about_version),
                value = BuildConfig.VERSION_NAME
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsInfoRow(
                label = stringResource(R.string.settings_about_developer),
                value = stringResource(R.string.settings_about_developer_name)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
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

private enum class ThemeChoice(
    val mode: ThemeMode,
    val labelRes: Int,
    val icon: ImageVector
) {
    SYSTEM(ThemeMode.SYSTEM, R.string.settings_theme_system, Icons.Outlined.BrightnessAuto),
    LIGHT(ThemeMode.LIGHT, R.string.settings_theme_light, Icons.Outlined.LightMode),
    DARK(ThemeMode.DARK, R.string.settings_theme_dark, Icons.Outlined.DarkMode)
}
