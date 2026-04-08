package com.hananelsabag.autocare.presentation.screens.cars

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.presentation.components.CarPager
import com.hananelsabag.autocare.presentation.screens.reminders.CarRemindersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsScreen(
    onCarClick: (Int) -> Unit,
    onAddServiceForCar: (Int) -> Unit = {}
) {
    val carsViewModel = hiltViewModel<CarsViewModel>()
    val addCarViewModel = hiltViewModel<AddCarViewModel>()
    val remindersViewModel = hiltViewModel<CarRemindersViewModel>()

    val cars by carsViewModel.cars.collectAsState()
    val nextServiceDueMsByCarId by carsViewModel.nextServiceDueMsByCarId.collectAsState()
    val lastSavedCarId by addCarViewModel.lastSavedCarId.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var reminderPromptCar by remember { mutableStateOf<Car?>(null) }
    var showNotifRationale by remember { mutableStateOf(false) }

    // Notification permission launcher
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled by system — no extra action needed */ }

    // On first launch, ask for notification permission once (API 33+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                showNotifRationale = true
            }
        }
    }

    // When a new car is saved, find it and offer reminder setup.
    // Keyed on BOTH lastSavedCarId and cars so that if the Room Flow hasn't
    // emitted the new car yet when lastSavedCarId fires, this effect re-runs
    // once cars updates and correctly finds the car on the next attempt.
    LaunchedEffect(lastSavedCarId, cars) {
        val id = lastSavedCarId ?: return@LaunchedEffect
        val car = cars.find { it.id == id }
        if (car != null) {
            reminderPromptCar = car
            addCarViewModel.clearLastSavedCarId()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.screen_cars_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(
                            visible = cars.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = if (cars.size == 1)
                                    stringResource(R.string.screen_cars_subtitle_single)
                                else
                                    stringResource(R.string.screen_cars_subtitle_plural, cars.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        if (cars.isEmpty()) {
            CarsEmptyState(
                onAddCar = { showAddSheet = true },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            CarPager(
                cars = cars,
                nextServiceDueMsByCarId = nextServiceDueMsByCarId,
                onCarClick = onCarClick,
                onAddCar = { showAddSheet = true },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // ── Notification permission rationale sheet ──────────────────
    if (showNotifRationale) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNotifRationale = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.notification_permission_rationale_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.notification_permission_rationale_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = {
                        showNotifRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(stringResource(R.string.notification_permission_allow))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showNotifRationale = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.notification_permission_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // ── Add Car Sheet ────────────────────────────────────────────
    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                addCarViewModel.resetForm()
                showAddSheet = false
            },
            sheetState = sheetState
        ) {
            AddCarSheetContent(
                viewModel = addCarViewModel,
                onSaved = {
                    addCarViewModel.resetForm()
                    showAddSheet = false
                },
                onCancel = {
                    addCarViewModel.resetForm()
                    showAddSheet = false
                }
            )
        }
    }

    // ── Post-save Reminder Prompt ────────────────────────────────
    reminderPromptCar?.let { car ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { reminderPromptCar = null },
            sheetState = sheetState
        ) {
            ReminderPromptSheet(
                car = car,
                onEnableDefaults = { remindersViewModel.enableDefaultReminders(car) },
                onDone = { reminderPromptCar = null },
                onAddService = {
                    reminderPromptCar = null
                    onAddServiceForCar(car.id)
                }
            )
        }
    }
}

@Composable
private fun ReminderPromptSheet(
    car: Car,
    onEnableDefaults: () -> Unit,  // enable but don't close
    onDone: () -> Unit,            // close the sheet
    onAddService: () -> Unit       // close + navigate to add service
) {
    // step 0 = reminders, step 1 = service
    var step by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (step == 0) {
            // ── Step 1: Reminders ──────────────────────────────────
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.enable_reminders_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.enable_reminders_prompt_subtitle, car.make, car.model),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.enable_reminders_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onEnableDefaults(); step = 1 },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.enable_reminders_default))
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { step = 1 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.enable_reminders_skip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // ── Step 2: Add service ────────────────────────────────
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_service_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_service_subtitle, car.make, car.model),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddService,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.setup_service_add))
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.setup_service_skip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CarsEmptyState(
    onAddCar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .then(
                        Modifier.padding(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.screen_cars_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.screen_cars_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            FilledTonalButton(
                onClick = onAddCar,
                modifier = Modifier.height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.screen_cars_empty_cta),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
