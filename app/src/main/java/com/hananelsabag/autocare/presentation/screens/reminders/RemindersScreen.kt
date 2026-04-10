package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car

private const val PICKER_INITIAL_VISIBLE = 4 // 2 rows × 2 cols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onCarClick: (Int) -> Unit) {
    val dashViewModel = hiltViewModel<RemindersDashboardViewModel>()
    val cars by dashViewModel.cars.collectAsState()

    var selectedCarId by remember { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.reminders_saved)

    LaunchedEffect(cars) {
        if (selectedCarId == -1 && cars.isNotEmpty()) {
            selectedCarId = cars.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.reminders_dashboard_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        if (cars.isEmpty()) {
            RemindersEmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CarPickerSection(
                    cars = cars,
                    selectedCarId = selectedCarId,
                    onSelect = { selectedCarId = it }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                if (selectedCarId != -1) {
                    val remindersVm = hiltViewModel<CarRemindersViewModel>(
                        key = "reminders_car_$selectedCarId"
                    )
                    LaunchedEffect(selectedCarId) { remindersVm.init(selectedCarId) }

                    CarRemindersContent(
                        viewModel = remindersVm,
                        onSaved = { scope.launch { snackbarHostState.showSnackbar(savedMsg) } }
                    )
                }
            }
        }
    }
}

// ── Car picker section ────────────────────────────────────────────────────────

@Composable
private fun CarPickerSection(
    cars: List<Car>,
    selectedCarId: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    // Collapse once a car is selected
    LaunchedEffect(selectedCarId) {
        if (selectedCarId != -1) expanded = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (selectedCarId != -1) {
            val selected = cars.firstOrNull { it.id == selectedCarId }
            if (selected != null) {
                SelectedCarHeader(
                    car = selected,
                    expanded = expanded,
                    onClick = { expanded = !expanded }
                )
            }
        }

        AnimatedVisibility(
            visible = expanded || selectedCarId == -1,
            enter = expandVertically(tween(280)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(280)) + fadeOut(tween(150))
        ) {
            CarPickerGrid(
                cars = cars,
                selectedCarId = selectedCarId,
                onSelect = { id ->
                    onSelect(id)
                    expanded = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedCarHeader(
    car: Car,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280),
        label = "arrow"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarThumbnail(car = car, sizeDp = 44)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${car.make} ${car.model}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                LicensePlateMini(plate = car.licensePlate)
            }

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CarPickerGrid(
    cars: List<Car>,
    selectedCarId: Int,
    onSelect: (Int) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val visibleCars = if (showAll || cars.size <= PICKER_INITIAL_VISIBLE) cars
                      else cars.take(PICKER_INITIAL_VISIBLE)
    val hiddenCount = cars.size - PICKER_INITIAL_VISIBLE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        visibleCars.chunked(2).forEach { rowCars ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCars.forEach { car ->
                    CarPickerCard(
                        car = car,
                        isSelected = car.id == selectedCarId,
                        onClick = { onSelect(car.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slot when last row has only 1 card
                if (rowCars.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (!showAll && hiddenCount > 0) {
            TextButton(
                onClick = { showAll = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.reminders_show_more_cars, hiddenCount))
            }
        }
    }
}

@Composable
private fun CarPickerCard(
    car: Car,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 88.dp)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CarThumbnail(car = car, sizeDp = 48)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${car.make} ${car.model}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                LicensePlateMini(plate = car.licensePlate)
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun CarThumbnail(car: Car, sizeDp: Int) {
    if (car.photoUri != null) {
        AsyncImage(
            model = car.photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size((sizeDp * 0.55f).dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LicensePlateMini(plate: String) {
    Surface(
        color = Color(0xFFFFD700),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = plate,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

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
