package com.hananelsabag.autocare.presentation.screens.cars

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SheetValue
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
import com.hananelsabag.autocare.presentation.components.rememberCarPagerState
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
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var reminderPromptCar by remember { mutableStateOf<Car?>(null) }

    // Hoisted so the discard dialog can call addCarSheetState.expand() on "continue editing"
    val addCarSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && addCarViewModel.hasUnsavedData()) {
                showDiscardConfirm = true
                false
            } else {
                true
            }
        }
    )
    var showNotifRationale by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<Car?>(null) }
    var scrollToCarId by remember { mutableStateOf<Int?>(null) }
    val pagerState = rememberCarPagerState(cars.size)

    // Exit edit mode on back press
    BackHandler(enabled = isEditMode) { isEditMode = false }

    // Scroll to a car after reorder — fires when scrollToCarId or cars changes
    LaunchedEffect(scrollToCarId, cars) {
        val id = scrollToCarId ?: return@LaunchedEffect
        val idx = cars.indexOfFirst { it.id == id }
        if (idx >= 0) pagerState.animateScrollToPage(idx)
    }

    // Notification permission launcher
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled by system */ }

    // On first launch, ask for notification permission once (API 33+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) showNotifRationale = true
        }
    }

    // When a new car is saved, find it and offer reminder setup
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
                            text = if (isEditMode)
                                stringResource(R.string.car_edit_mode_title)
                            else
                                stringResource(R.string.screen_cars_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        AnimatedVisibility(
                            visible = cars.isNotEmpty() && !isEditMode,
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
                        AnimatedVisibility(
                            visible = isEditMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = stringResource(R.string.car_edit_mode_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { isEditMode = false }) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.car_edit_mode_done)
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
                onCarLongPress = { isEditMode = true },
                isEditMode = isEditMode,
                onMoveLeft = { car ->
                    carsViewModel.moveLeft(car)
                    scrollToCarId = car.id
                },
                onMoveRight = { car ->
                    carsViewModel.moveRight(car)
                    scrollToCarId = car.id
                },
                onDeleteRequest = { car -> carToDelete = car },
                pagerState = pagerState,
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
    fun closeAddSheet() {
        addCarViewModel.resetForm()
        showAddSheet = false
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!addCarViewModel.hasUnsavedData()) closeAddSheet()
                // If dirty: confirmValueChange showed the dialog; sheet stays open.
            },
            sheetState = addCarSheetState
        ) {
            AddCarSheetContent(
                viewModel = addCarViewModel,
                onSaved = { closeAddSheet() },
                onCancel = {
                    // X button: respect dirty state same as swipe-down
                    if (addCarViewModel.hasUnsavedData()) showDiscardConfirm = true
                    else closeAddSheet()
                }
            )
        }
    }

    // ── Discard confirmation ─────────────────────────────────────
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = {
                // Tapping outside = "continue editing"
                showDiscardConfirm = false
                scope.launch { addCarSheetState.expand() }
            },
            title = { Text(stringResource(R.string.add_car_discard_title)) },
            text = { Text(stringResource(R.string.add_car_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    closeAddSheet()
                }) {
                    Text(
                        stringResource(R.string.add_car_discard_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    scope.launch { addCarSheetState.expand() }
                }) {
                    Text(stringResource(R.string.add_car_discard_cancel))
                }
            }
        )
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

    // ── Delete car confirmation ──────────────────────────────────
    carToDelete?.let { car ->
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            title = { Text(stringResource(R.string.car_profile_delete_confirm_title)) },
            text = { Text(stringResource(R.string.car_profile_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    carsViewModel.deleteCar(car)
                    carToDelete = null
                    // If we deleted the last car, exit edit mode
                    if (cars.size <= 1) isEditMode = false
                }) {
                    Text(
                        text = stringResource(R.string.car_profile_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { carToDelete = null }) {
                    Text(stringResource(R.string.car_profile_delete_confirm_no))
                }
            }
        )
    }
}

@Composable
private fun ReminderPromptSheet(
    car: Car,
    onEnableDefaults: () -> Unit,
    onDone: () -> Unit,
    onAddService: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (step == 0) {
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
                    .then(Modifier.padding(8.dp)),
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
