package com.hananelsabag.autocare.presentation.screens.carprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.screens.cars.AddCarSheetContent
import com.hananelsabag.autocare.presentation.screens.cars.AddCarViewModel
import com.hananelsabag.autocare.presentation.theme.StatusGreen
import com.hananelsabag.autocare.presentation.theme.StatusGreenContainer
import com.hananelsabag.autocare.presentation.theme.StatusRed
import com.hananelsabag.autocare.presentation.theme.StatusRedContainer
import com.hananelsabag.autocare.presentation.theme.StatusYellow
import com.hananelsabag.autocare.presentation.theme.StatusYellowContainer
import com.hananelsabag.autocare.util.StatusLevel
import com.hananelsabag.autocare.util.carColorToComposeColor
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.getStatusLevel
import com.hananelsabag.autocare.util.toFormattedDate
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarProfileScreen(
    carId: Int,
    onBack: () -> Unit,
    onMaintenanceHistory: () -> Unit,
    onDocuments: () -> Unit,
    onReminders: () -> Unit
) {
    val viewModel = hiltViewModel<CarProfileViewModel>()
    val addCarViewModel = hiltViewModel<AddCarViewModel>()
    val car by viewModel.car.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val detailedStats by viewModel.detailedStats.collectAsState()
    val nextServiceDueMs by viewModel.nextServiceDueMs.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { viewModel.init(carId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Title visible when scrolled past hero
                    Text(
                        text = car?.let { "${it.make} ${it.model}" } ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                },
                actions = {
                    // Scrim pill behind edit/delete so icons read on any photo
                    Surface(
                        color = Color.Black.copy(alpha = 0.30f),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row {
                            IconButton(onClick = {
                                car?.let { addCarViewModel.loadCarForEdit(it.id) }
                                showEditSheet = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.content_description_edit),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.content_description_delete),
                                    tint = Color.White.copy(alpha = 0.90f)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        car?.let { currentCar ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Full-bleed top: hero starts at y=0 behind transparent TopAppBar.
                    // Only bottom padding is applied so the last item clears the nav bar.
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Hero ────────────────────────────────────────────────
                CarHeroSection(
                    car = currentCar,
                    topBarPadding = paddingValues.calculateTopPadding()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Status section ──────────────────────────────────────
                SectionLabel(stringResource(R.string.car_profile_status_section))
                Spacer(modifier = Modifier.height(8.dp))
                StatusBanner(car = currentCar, nextServiceDueMs = nextServiceDueMs)

                Spacer(modifier = Modifier.height(20.dp))

                // ── Details section ─────────────────────────────────────
                SectionLabel(stringResource(R.string.car_profile_details_section))
                Spacer(modifier = Modifier.height(8.dp))
                QuickStatsRow(car = currentCar)

                Spacer(modifier = Modifier.height(20.dp))

                // ── Actions section ─────────────────────────────────────
                SectionLabel(stringResource(R.string.car_profile_actions_section))
                Spacer(modifier = Modifier.height(8.dp))
                ActionTilesGrid(
                    onMaintenanceHistory = onMaintenanceHistory,
                    onDocuments = onDocuments,
                    onReminders = onReminders
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Stats or first-service CTA ──────────────────────────
                StatsSection(
                    stats = stats,
                    onAddFirstService = onMaintenanceHistory,
                    onViewStats = { showStatsSheet = true }
                )

                // ── Notes ───────────────────────────────────────────────
                if (!currentCar.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    NotesCard(notes = currentCar.notes)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Edit Sheet ──────────────────────────────────────────────────────
    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                addCarViewModel.resetForm()
                showEditSheet = false
            },
            sheetState = sheetState
        ) {
            AddCarSheetContent(
                viewModel = addCarViewModel,
                onSaved = {
                    addCarViewModel.resetForm()
                    showEditSheet = false
                },
                onCancel = {
                    addCarViewModel.resetForm()
                    showEditSheet = false
                }
            )
        }
    }

    // ── Stats Sheet ─────────────────────────────────────────────────────
    if (showStatsSheet && detailedStats != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStatsSheet = false },
            sheetState = sheetState
        ) {
            CarStatsSheet(stats = detailedStats!!)
        }
    }

    // ── Delete Dialog ───────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.car_profile_delete_confirm_title)) },
            text = { Text(stringResource(R.string.car_profile_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCar(onDeleted = onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.car_profile_delete_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.car_profile_delete_confirm_no))
                }
            }
        )
    }
}

// ── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// ── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun CarHeroSection(car: Car, topBarPadding: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
    ) {
        if (car.photoUri != null) {
            // ── Photo hero ──────────────────────────────────────────────
            AsyncImage(
                model = car.photoUri,
                contentDescription = stringResource(R.string.content_description_car_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient — strong, text always readable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )

            // Text overlay at bottom-start
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color accent dot
                    if (car.color != null) {
                        val dotColor = carColorToComposeColor(car.color)
                        if (dotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(dotColor, CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                            )
                        }
                    }
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = car.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(text = "·", color = Color.White.copy(alpha = 0.55f))
                    // Frosted plate badge
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = car.licensePlate,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        } else {
            // ── Placeholder hero — tinted by car color ──────────────────
            val carColor = car.color?.let { carColorToComposeColor(it) }
            val gradientTop = carColor ?: MaterialTheme.colorScheme.primaryContainer
            val gradientBottom = (carColor?.copy(alpha = 0.55f))
                ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(gradientTop, gradientBottom))
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconTint = if (carColor != null) Color.White.copy(alpha = 0.80f)
                               else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.60f)
                val textColor = if (carColor != null) Color.White.copy(alpha = 0.92f)
                                else MaterialTheme.colorScheme.onPrimaryContainer
                val subColor  = if (carColor != null) Color.White.copy(alpha = 0.70f)
                                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = iconTint
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${car.make} ${car.model}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = car.licensePlate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subColor
                    )
                }
            }
        }

        // Top scrim — ensures edit/delete icons are readable over any photo or tint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarPadding + 8.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Status banner — two individual mini-cards ─────────────────────────────────

@Composable
private fun StatusBanner(car: Car, nextServiceDueMs: Long?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatusMiniCard(
            icon = Icons.Outlined.VerifiedUser,
            label = stringResource(R.string.car_profile_test_expiry),
            expiryMs = car.testExpiryDate,
            level = getStatusLevel(car.testExpiryDate)
        )
        StatusMiniCard(
            icon = Icons.Outlined.Security,
            label = stringResource(R.string.car_profile_insurance_expiry),
            expiryMs = car.insuranceExpiryDate,
            level = getStatusLevel(car.insuranceExpiryDate)
        )
        StatusMiniCard(
            icon = Icons.Outlined.Build,
            label = stringResource(R.string.car_profile_service_status),
            expiryMs = nextServiceDueMs,
            level = getStatusLevel(nextServiceDueMs)
        )
    }
}

@Composable
private fun StatusMiniCard(
    icon: ImageVector,
    label: String,
    expiryMs: Long?,
    level: StatusLevel
) {
    val cardColor = when (level) {
        StatusLevel.GREEN                    -> StatusGreenContainer
        StatusLevel.YELLOW                   -> StatusYellowContainer
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRedContainer
        StatusLevel.UNKNOWN                  -> MaterialTheme.colorScheme.surfaceVariant
    }
    val accentColor = when (level) {
        StatusLevel.GREEN                    -> StatusGreen
        StatusLevel.YELLOW                   -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN                  -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val isExpired = level == StatusLevel.EXPIRED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isExpired) Modifier.border(2.dp, StatusRed, MaterialTheme.shapes.large)
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Label + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusDescriptionText(expiryMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // Prominent day-count badge on the right
            DaysBadge(expiryMs = expiryMs, accentColor = accentColor)
        }
    }
}

@Composable
private fun DaysBadge(expiryMs: Long?, accentColor: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 8.dp)
            .width(56.dp)
    ) {
        when {
            expiryMs == null -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                    modifier = Modifier.size(26.dp)
                )
            }
            else -> {
                val days = expiryMs.daysFromNow()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        days < 0L  -> {
                            Text(
                                text = stringResource(R.string.car_profile_status_expired_short),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                        days == 0L -> {
                            Text(
                                text = stringResource(R.string.car_profile_days_today),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = days.toInt().toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.car_profile_days_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusDescriptionText(expiryMs: Long?): String {
    if (expiryMs == null) return stringResource(R.string.status_not_set)
    val days = expiryMs.daysFromNow()
    return when {
        days < -1L -> stringResource(R.string.status_expired_days_ago, abs(days).toInt())
        days == -1L || days == 0L -> stringResource(R.string.status_expires_today)
        days == 1L -> stringResource(R.string.status_expires_tomorrow)
        else -> stringResource(R.string.status_valid_until_days, expiryMs.toFormattedDate(), days.toInt())
    }
}

// ── Quick details row ─────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(car: Car) {
    val items = buildList {
        add(Triple(
            Icons.Outlined.Speed,
            stringResource(R.string.car_profile_current_km),
            if (car.currentKm != null) stringResource(R.string.car_card_km_format, car.currentKm.toString())
            else stringResource(R.string.car_profile_no_km)
        ))
        if (car.testExpiryDate != null)
            add(Triple(
                Icons.Outlined.VerifiedUser,
                stringResource(R.string.car_profile_test_expiry),
                car.testExpiryDate.toFormattedDate()
            ))
        if (car.color != null)
            add(Triple(
                Icons.Outlined.DirectionsCar,
                stringResource(R.string.car_profile_color),
                car.color
            ))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label, value) ->
                DetailStatItem(icon = icon, label = label, value = value)
                if (index < items.lastIndex) {
                    VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailStatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Action tiles (2×2 grid) ───────────────────────────────────────────────────

@Composable
private fun ActionTilesGrid(
    onMaintenanceHistory: () -> Unit,
    onDocuments: () -> Unit,
    onReminders: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionTile(
            icon = Icons.Filled.History,
            label = stringResource(R.string.car_profile_maintenance_history),
            onClick = onMaintenanceHistory,
            isPrimary = true,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = Icons.Outlined.Description,
            label = stringResource(R.string.car_profile_documents),
            onClick = onDocuments,
            isPrimary = false,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = Icons.Outlined.NotificationsNone,
            label = stringResource(R.string.car_profile_reminders),
            onClick = onReminders,
            isPrimary = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPrimary)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (isPrimary)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ── Stats section ─────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(stats: CarStats?, onAddFirstService: () -> Unit, onViewStats: () -> Unit) {
    when {
        stats == null -> FirstServiceCtaCard(onClick = onAddFirstService)
        stats.totalRecords < 2 -> OneMoreServiceCtaCard(onClick = onAddFirstService)
        else -> CarStatsCard(stats = stats, onClick = onViewStats)
    }
}

@Composable
private fun FirstServiceCtaCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.car_profile_first_service_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.car_profile_first_service_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OneMoreServiceCtaCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.car_profile_one_service_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.car_profile_one_service_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Progress bar ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                )
                Text(
                    text = stringResource(R.string.car_profile_one_service_progress),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ── Locked stats preview ──────────────────────────────────
            Text(
                text = stringResource(R.string.car_profile_one_service_unlocks),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockedStatChip(
                    label = stringResource(R.string.stats_last_service),
                    modifier = Modifier.weight(1f)
                )
                LockedStatChip(
                    label = stringResource(R.string.stats_spent_this_year),
                    modifier = Modifier.weight(1f)
                )
                LockedStatChip(
                    label = stringResource(R.string.stats_average_cost),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LockedStatChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CarStatsCard(stats: CarStats, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_section_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.stats_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    label = stringResource(R.string.stats_total_records),
                    value = stats.totalRecords.toString(),
                    icon = Icons.Outlined.Build
                )
                if (stats.lastServiceDate != null) {
                    VerticalDivider(modifier = Modifier.height(44.dp))
                    StatItem(
                        label = stringResource(R.string.stats_last_service),
                        value = stats.lastServiceDate.toFormattedDate(),
                        icon = Icons.Outlined.CalendarToday
                    )
                }
                if (stats.totalSpentThisYear != null) {
                    VerticalDivider(modifier = Modifier.height(44.dp))
                    StatItem(
                        label = stringResource(R.string.stats_spent_this_year),
                        value = stringResource(
                            R.string.record_cost_format,
                            if (stats.totalSpentThisYear % 1.0 == 0.0)
                                stats.totalSpentThisYear.toInt().toString()
                            else stats.totalSpentThisYear.toString()
                        ),
                        icon = Icons.Outlined.AttachMoney
                    )
                }
                if (stats.averageCost != null) {
                    VerticalDivider(modifier = Modifier.height(44.dp))
                    StatItem(
                        label = stringResource(R.string.stats_average_cost),
                        value = stringResource(
                            R.string.record_cost_format,
                            if (stats.averageCost % 1.0 == 0.0)
                                stats.averageCost.toInt().toString()
                            else stats.averageCost.toString()
                        ),
                        icon = Icons.Outlined.Analytics
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Notes card ────────────────────────────────────────────────────────────────

@Composable
private fun NotesCard(notes: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.car_profile_notes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
