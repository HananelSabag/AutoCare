package com.hananelsabag.autocare.presentation.screens.carprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.getStatusLevel
import com.hananelsabag.autocare.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarProfileScreen(
    carId: Int,
    onBack: () -> Unit,
    onMaintenanceHistory: () -> Unit,
    onDocuments: () -> Unit
) {
    val viewModel = hiltViewModel<CarProfileViewModel>()
    val addCarViewModel = hiltViewModel<AddCarViewModel>()
    val car by viewModel.car.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTestHistorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { viewModel.init(carId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                    IconButton(onClick = {
                        car?.let { addCarViewModel.loadCarForEdit(it.id) }
                        showEditSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.content_description_edit)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.content_description_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        car?.let { currentCar ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Car photo / hero header ─────────────────────
                CarHeroSection(car = currentCar)

                // ── Status Banner ───────────────────────────────
                StatusBanner(car = currentCar)

                Spacer(modifier = Modifier.height(12.dp))

                // ── Quick Stats ─────────────────────────────────
                QuickStatsRow(car = currentCar)

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(12.dp))

                // ── Action buttons ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onMaintenanceHistory,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = stringResource(R.string.car_profile_maintenance_history),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        OutlinedButton(
                            onClick = { showTestHistorySheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AssignmentTurnedIn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = stringResource(R.string.car_profile_test_history),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDocuments,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.car_profile_documents),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // ── Stats ───────────────────────────────────────
                stats?.let { carStats ->
                    Spacer(modifier = Modifier.height(12.dp))
                    CarStatsCard(stats = carStats)
                }

                // ── Notes ───────────────────────────────────────
                if (!currentCar.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = stringResource(R.string.car_profile_notes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentCar.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Edit Sheet ──────────────────────────────────────────────
    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState
        ) {
            AddCarSheetContent(
                viewModel = addCarViewModel,
                onSaved = { showEditSheet = false },
                onCancel = { showEditSheet = false }
            )
        }
    }

    // ── Test History Sheet ──────────────────────────────────────
    if (showTestHistorySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showTestHistorySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.test_history_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Delete Dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.car_profile_delete_confirm_title)) },
            text = { Text(stringResource(R.string.car_profile_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCar(onDeleted = onBack) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.car_profile_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.car_profile_delete_confirm_no))
                }
            }
        )
    }
}

@Composable
private fun CarStatsCard(stats: CarStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_section_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = stringResource(R.string.stats_total_records),
                    value = stats.totalRecords.toString()
                )
                if (stats.lastServiceDate != null) {
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    StatItem(
                        label = stringResource(R.string.stats_last_service),
                        value = stats.lastServiceDate.toFormattedDate()
                    )
                }
                if (stats.totalSpentThisYear != null) {
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    StatItem(
                        label = stringResource(R.string.stats_spent_this_year),
                        value = stringResource(
                            R.string.record_cost_format,
                            if (stats.totalSpentThisYear % 1.0 == 0.0)
                                stats.totalSpentThisYear.toInt().toString()
                            else stats.totalSpentThisYear.toString()
                        )
                    )
                }
                if (stats.averageCost != null) {
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    StatItem(
                        label = stringResource(R.string.stats_average_cost),
                        value = stringResource(
                            R.string.record_cost_format,
                            if (stats.averageCost % 1.0 == 0.0)
                                stats.averageCost.toInt().toString()
                            else stats.averageCost.toString()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CarHeroSection(car: Car) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (car.photoUri != null) {
            AsyncImage(
                model = car.photoUri,
                contentDescription = stringResource(R.string.content_description_car_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
            // License plate + year overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = car.year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "·",
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = car.licensePlate,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = car.licensePlate,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(car: Car) {
    val testLevel = getStatusLevel(car.testExpiryDate)
    val insuranceLevel = getStatusLevel(car.insuranceExpiryDate)
    val comprehensiveLevel = getStatusLevel(car.comprehensiveInsuranceExpiryDate)

    // Overall banner color = worst status across all documents
    val worstLevel = listOf(testLevel, insuranceLevel, comprehensiveLevel)
        .maxByOrNull { it.ordinal } ?: StatusLevel.UNKNOWN

    val bannerColor = when (worstLevel) {
        StatusLevel.GREEN -> StatusGreenContainer
        StatusLevel.YELLOW -> StatusYellowContainer
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRedContainer
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow(
                label = stringResource(R.string.car_profile_test_expiry),
                expiryMs = car.testExpiryDate,
                level = testLevel
            )
            if (car.insuranceExpiryDate != null || car.comprehensiveInsuranceExpiryDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (car.insuranceExpiryDate != null) {
                StatusRow(
                    label = stringResource(R.string.car_profile_insurance_expiry),
                    expiryMs = car.insuranceExpiryDate,
                    level = insuranceLevel
                )
            }
            if (car.comprehensiveInsuranceExpiryDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = stringResource(R.string.car_profile_comprehensive_expiry),
                    expiryMs = car.comprehensiveInsuranceExpiryDate,
                    level = getStatusLevel(car.comprehensiveInsuranceExpiryDate)
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, expiryMs: Long?, level: StatusLevel) {
    val dotColor = when (level) {
        StatusLevel.GREEN -> StatusGreen
        StatusLevel.YELLOW -> StatusYellow
        StatusLevel.RED, StatusLevel.EXPIRED -> StatusRed
        StatusLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = statusDescriptionText(expiryMs = expiryMs)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = dotColor
            )
        }
    }
}

@Composable
private fun statusDescriptionText(expiryMs: Long?): String {
    if (expiryMs == null) return stringResource(R.string.status_not_set)
    val days = expiryMs.daysFromNow()
    return when {
        days < -1L -> stringResource(R.string.status_expired_days_ago, -days)
        days == -1L || days == 0L -> stringResource(R.string.status_expires_today)
        days == 1L -> stringResource(R.string.status_expires_tomorrow)
        else -> stringResource(R.string.status_valid_until_days, expiryMs.toFormattedDate(), days.toInt())
    }
}

@Composable
private fun QuickStatsRow(car: Car) {
    val stats = buildList {
        add(
            stringResource(R.string.car_profile_current_km) to
                (if (car.currentKm != null)
                    stringResource(R.string.car_card_km_format, car.currentKm.toString())
                else stringResource(R.string.car_profile_no_km))
        )
        if (car.testExpiryDate != null)
            add(stringResource(R.string.car_profile_test_expiry) to car.testExpiryDate.toFormattedDate())
        if (car.color != null)
            add(stringResource(R.string.car_profile_color) to car.color)
    }

    Card(
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
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stats.forEachIndexed { index, (label, value) ->
                StatItem(label = label, value = value)
                if (index < stats.lastIndex) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
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
