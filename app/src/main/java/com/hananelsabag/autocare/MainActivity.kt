package com.hananelsabag.autocare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hananelsabag.autocare.notifications.ReminderCheckWorker
import com.hananelsabag.autocare.notifications.createNotificationChannel
import com.hananelsabag.autocare.presentation.MainScreen
import com.hananelsabag.autocare.presentation.SplashScreen
import com.hananelsabag.autocare.presentation.screens.settings.SettingsViewModel
import com.hananelsabag.autocare.presentation.theme.AutoCareTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Activity-scoped so theme persists across all screens
    private val settingsViewModel: SettingsViewModel by viewModels()

    private fun scheduleReminderWorker() {
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel(this)
        scheduleReminderWorker()
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            var splashDone by remember { mutableStateOf(false) }

            AutoCareTheme(themeMode = themeMode) {
                if (!splashDone) {
                    SplashScreen(onFinished = { splashDone = true })
                } else {
                    MainScreen()
                }
            }
        }
    }
}
