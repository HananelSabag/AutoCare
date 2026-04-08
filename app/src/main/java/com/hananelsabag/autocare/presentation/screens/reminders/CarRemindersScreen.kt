package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusGreenContainer
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusRedContainer
import com.hananelsabag.autocare.presentation.theme.StatusYellow
import com.hananelsabag.autocare.presentation.theme.StatusYellowContainer
import com.hananelsabag.autocare.util.StatusLevel
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.getStatusLevel
import com.hananelsabag.autocare.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarRemindersScreen(carId: Int, onBack: () -> Unit) {
    val viewModel = hiltViewModel<CarRemindersViewModel>()
    LaunchedEffect(carId) { viewModel.init(carId) }

    val car by viewModel.car.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val lastMaintenanceDate by viewModel.lastMaintenanceDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(car?.let { "${it.make} ${it.model}" }
                        ?: stringResource(R.string.car_reminders_title))
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ReminderType.entries, key = { it.ordinal }) { type ->
                val state = formState[type] ?: return@items

                // Determine the expiry date for this reminder type
                val expiryMs: Long? = when (type) {
                    ReminderType.TEST_EXPIRY -> car?.testExpiryDate
                    ReminderType.INSURANCE_COMPULSORY_EXPIRY -> car?.insuranceExpiryDate
                    ReminderType.SERVICE_DATE ->
                        CarRemindersViewModel.serviceDueDate(lastMaintenanceDate)
                }

                ReminderCard(
                    type = type,
                    state = state,
                    expiryMs = expiryMs,
                    isServiceType = type == ReminderType.SERVICE_DATE,
                    hasNoServiceRecords = type == ReminderType.SERVICE_DATE && lastMaintenanceDate == null,
                    onEnabledChange = { viewModel.updateEnabled(type, it) },
                    onDaysChange = { viewModel.updateDays(type, it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.saveReminders(onSaved = onBack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.reminder_save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ─── Reminder Card ────────────────────────────────────────────────────────────

@Composable
private fun ReminderCard(
    type: ReminderType,
    state: ReminderUiState,
    expiryMs: Long?,
    isServiceType: Boolean,
    hasNoServiceRecords: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDaysChange: (String) -> Unit
) {
    val status = getStatusLevel(expiryMs)
    val daysLeft = expiryMs?.daysFromNow()

    // Resolve badge colors unconditionally (no conditional @Composable calls)
    val scheme = MaterialTheme.colorScheme
    val (badgeContainerColor, badgeIconColor) = when {
        !state.enabled -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        status == StatusLevel.GREEN -> StatusGreenContainer to StatusGreen
        status == StatusLevel.YELLOW -> StatusYellowContainer to StatusYellow
        status == StatusLevel.RED || status == StatusLevel.EXPIRED ->
            StatusRedContainer to StatusRed
        else -> scheme.primaryContainer to scheme.primary // UNKNOWN / date not set
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.enabled)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row: badge • label • switch ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeContainerColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = type.iconVector(),
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(type.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Countdown chip — always shown, reflects urgency or missing-date state
                    CountdownChip(
                        expiryMs = expiryMs,
                        daysLeft = daysLeft,
                        status = status,
                        hasNoServiceRecords = hasNoServiceRecords,
                        isServiceType = isServiceType
                    )
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            // ── Expanded settings — only when enabled ──────────────
            AnimatedVisibility(
                visible = state.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Schedule info line
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.reminder_schedule_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom start-window field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_custom_window_prefix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.daysBeforeExpiry,
                            onValueChange = onDaysChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(76.dp),
                            shape = MaterialTheme.shapes.medium,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reminder_custom_window_suffix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─── Countdown chip ───────────────────────────────────────────────────────────

@Composable
private fun CountdownChip(
    expiryMs: Long?,
    daysLeft: Long?,
    status: StatusLevel,
    hasNoServiceRecords: Boolean,
    isServiceType: Boolean
) {
    val (containerColor, contentColor, icon, label) = when {
        // SERVICE_DATE — no maintenance records at all
        isServiceType && hasNoServiceRecords -> ChipData(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Outlined.WarningAmber,
            label = stringResource(R.string.reminder_no_service_records)
        )

        // Any type — date not set
        expiryMs == null -> ChipData(
            container = StatusYellowContainer,
            content = StatusYellow,
            icon = Icons.Outlined.WarningAmber,
            label = stringResource(R.string.reminder_no_date_warning)
        )

        // Expired
        status == StatusLevel.EXPIRED -> ChipData(
            container = StatusRedContainer,
            content = StatusRed,
            icon = Icons.Outlined.WarningAmber,
            label = stringResource(R.string.reminder_countdown_expired, -daysLeft!!)
        )

        // Expires today
        daysLeft == 0L -> ChipData(
            container = StatusRedContainer,
            content = StatusRed,
            icon = Icons.Outlined.WarningAmber,
            label = stringResource(R.string.reminder_countdown_today)
        )

        // Expires tomorrow
        daysLeft == 1L -> ChipData(
            container = StatusRedContainer,
            content = StatusRed,
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reminder_countdown_tomorrow)
        )

        // RED window (≤ 7 days)
        status == StatusLevel.RED -> ChipData(
            container = StatusRedContainer,
            content = StatusRed,
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reminder_countdown_days, daysLeft!!)
                + " · " + expiryMs.toFormattedDate()
        )

        // YELLOW window (8–30 days)
        status == StatusLevel.YELLOW -> ChipData(
            container = StatusYellowContainer,
            content = StatusYellow,
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reminder_countdown_days, daysLeft!!)
                + " · " + expiryMs.toFormattedDate()
        )

        // GREEN — plenty of time
        else -> ChipData(
            container = StatusGreenContainer,
            content = StatusGreen,
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.reminder_countdown_days, daysLeft!!)
                + " · " + expiryMs.toFormattedDate()
        )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class ChipData(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    val label: String
)

private fun ReminderType.labelRes(): Int = when (this) {
    ReminderType.TEST_EXPIRY                 -> R.string.reminder_type_test_expiry
    ReminderType.INSURANCE_COMPULSORY_EXPIRY -> R.string.reminder_type_insurance_compulsory
    ReminderType.SERVICE_DATE                -> R.string.reminder_type_service_date
}

private fun ReminderType.iconVector(): ImageVector = when (this) {
    ReminderType.TEST_EXPIRY                 -> Icons.Outlined.VerifiedUser
    ReminderType.INSURANCE_COMPULSORY_EXPIRY -> Icons.Outlined.Security
    ReminderType.SERVICE_DATE                -> Icons.Outlined.Autorenew
}
