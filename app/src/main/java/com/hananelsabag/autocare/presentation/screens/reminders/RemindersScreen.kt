package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusGreenContainer
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusRedContainer
import com.hananelsabag.autocare.presentation.theme.StatusYellow
import com.hananelsabag.autocare.presentation.theme.StatusYellowContainer
import com.hananelsabag.autocare.util.StatusLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onCarClick: (Int) -> Unit) {
    val viewModel = hiltViewModel<RemindersDashboardViewModel>()
    val items by viewModel.items.collectAsState()

    // Partition into sections by urgency
    val urgent = items.filter {
        it.level == StatusLevel.EXPIRED || it.level == StatusLevel.RED
    }.sortedBy { it.daysLeft ?: Long.MAX_VALUE }

    val soon = items.filter {
        it.level == StatusLevel.YELLOW
    }.sortedBy { it.daysLeft }

    val ok = items.filter {
        it.level == StatusLevel.GREEN
    }.sortedBy { it.daysLeft }

    val unknown = items.filter {
        it.level == StatusLevel.UNKNOWN
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.reminders_dashboard_title)) })
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        if (items.isEmpty()) {
            RemindersEmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (urgent.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.reminders_section_urgent),
                            color = StatusRed
                        )
                    }
                    items(urgent, key = { "${it.car.id}_${it.type}" }) { item ->
                        ReminderDashboardCard(item = item, onClick = { onCarClick(item.car.id) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (soon.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.reminders_section_soon),
                            color = StatusYellow
                        )
                    }
                    items(soon, key = { "${it.car.id}_${it.type}" }) { item ->
                        ReminderDashboardCard(item = item, onClick = { onCarClick(item.car.id) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (ok.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.reminders_section_ok),
                            color = StatusGreen
                        )
                    }
                    items(ok, key = { "${it.car.id}_${it.type}" }) { item ->
                        ReminderDashboardCard(item = item, onClick = { onCarClick(item.car.id) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (unknown.isNotEmpty()) {
                    items(unknown, key = { "${it.car.id}_${it.type}" }) { item ->
                        ReminderDashboardCard(item = item, onClick = { onCarClick(item.car.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ReminderDashboardCard(
    item: ReminderDashboardItem,
    onClick: () -> Unit
) {
    val accentColor = when (item.level) {
        StatusLevel.GREEN -> StatusGreen
        StatusLevel.YELLOW -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant
    }
    val badgeContainerColor = when (item.level) {
        StatusLevel.GREEN -> StatusGreenContainer
        StatusLevel.YELLOW -> StatusYellowContainer
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRedContainer
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val daysLabel = when {
        item.expiryMs == null -> stringResource(R.string.reminders_item_no_date)
        item.daysLeft == null -> stringResource(R.string.reminders_item_no_date)
        item.daysLeft < 0 -> stringResource(R.string.reminders_item_expired, -item.daysLeft)
        item.daysLeft == 0L -> stringResource(R.string.reminders_item_expires_today)
        item.daysLeft == 1L -> stringResource(R.string.reminders_item_expires_tomorrow)
        else -> stringResource(R.string.reminders_item_days_left, item.daysLeft)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // Car photo thumbnail
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
            ) {
                if (item.car.photoUri != null) {
                    AsyncImage(
                        model = item.car.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Subtle gradient for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "${item.car.make} ${item.car.model}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = item.type.iconVector(),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(item.type.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Days badge
            Surface(
                color = badgeContainerColor,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(end = 14.dp)
            ) {
                Text(
                    text = daysLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RemindersEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.reminders_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reminders_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ReminderType.labelRes(): Int = when (this) {
    ReminderType.TEST_EXPIRY -> R.string.reminder_type_test_expiry
    ReminderType.INSURANCE_EXPIRY -> R.string.reminder_type_insurance_compulsory
    ReminderType.SERVICE_DATE -> R.string.reminder_type_service_date
}

private fun ReminderType.iconVector(): ImageVector = when (this) {
    ReminderType.TEST_EXPIRY -> Icons.Outlined.VerifiedUser
    ReminderType.INSURANCE_EXPIRY -> Icons.Outlined.Security
    ReminderType.SERVICE_DATE -> Icons.Outlined.Autorenew
}
