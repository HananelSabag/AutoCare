package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.ReminderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarRemindersScreen(
    carId: Int,
    onBack: () -> Unit
) {
    val viewModel = hiltViewModel<CarRemindersViewModel>()
    LaunchedEffect(carId) { viewModel.init(carId) }

    val car by viewModel.car.collectAsState()
    val savedReminders by viewModel.reminders.collectAsState()

    // Build a mutable working copy of reminders based on what's applicable to this car
    val applicableTypes = remember(car) { getApplicableTypes(car) }

    // For each applicable type, track enabled state and daysBeforeExpiry
    val reminderStates = remember(savedReminders, applicableTypes) {
        applicableTypes.associateWith { type ->
            val existing = savedReminders.find { it.type == type }
            ReminderState(
                enabled = existing?.enabled ?: false,
                daysBeforeExpiry = (existing?.daysBeforeExpiry ?: 14).toString()
            )
        }.toMutableMap()
    }

    // Use state holders so the UI can be reactive
    val stateMap = remember(reminderStates) {
        reminderStates.mapValues { (_, v) ->
            mutableStateOf(v)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = car?.let { "${it.make} ${it.model}" }
                            ?: stringResource(R.string.car_reminders_title)
                    )
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(applicableTypes) { type ->
                val stateHolder = stateMap[type] ?: return@items
                val state by stateHolder
                ReminderCard(
                    type = type,
                    state = state,
                    onStateChange = { stateHolder.value = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val updatedReminders = stateMap.mapNotNull { (type, stateHolder) ->
                            val s = stateHolder.value
                            val days = s.daysBeforeExpiry.toIntOrNull() ?: 14
                            Reminder(
                                carId = carId,
                                type = type,
                                enabled = s.enabled,
                                daysBeforeExpiry = days
                            )
                        }
                        viewModel.saveReminders(updatedReminders)
                        onBack()
                    },
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
            }
        }
    }
}

private data class ReminderState(
    val enabled: Boolean,
    val daysBeforeExpiry: String
)

@Composable
private fun ReminderCard(
    type: ReminderType,
    state: ReminderState,
    onStateChange: (ReminderState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(type.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { onStateChange(state.copy(enabled = it)) }
                )
            }
            if (state.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.daysBeforeExpiry,
                        onValueChange = { onStateChange(state.copy(daysBeforeExpiry = it)) },
                        label = { Text(stringResource(R.string.reminder_days_before_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reminder_days_before_suffix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getApplicableTypes(car: Car?): List<ReminderType> {
    if (car == null) return emptyList()
    return buildList {
        if (car.testExpiryDate != null) add(ReminderType.TEST_EXPIRY)
        if (car.insuranceExpiryDate != null) add(ReminderType.INSURANCE_COMPULSORY_EXPIRY)
        if (car.comprehensiveInsuranceExpiryDate != null) add(ReminderType.INSURANCE_COMPREHENSIVE_EXPIRY)
        add(ReminderType.SERVICE_DATE)
    }
}

private fun ReminderType.labelRes(): Int = when (this) {
    ReminderType.TEST_EXPIRY                     -> R.string.reminder_type_test_expiry
    ReminderType.INSURANCE_COMPULSORY_EXPIRY     -> R.string.reminder_type_insurance_compulsory
    ReminderType.INSURANCE_COMPREHENSIVE_EXPIRY  -> R.string.reminder_type_insurance_comprehensive
    ReminderType.SERVICE_DATE                    -> R.string.reminder_type_service_date
}
