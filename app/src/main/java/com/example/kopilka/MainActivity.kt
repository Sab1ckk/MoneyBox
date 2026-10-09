package com.example.kopilka

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.kopilka.data.datastore.SettingsDataStore
import com.example.kopilka.data.datastore.ThemeModeSetting
import com.example.kopilka.navigation.NavGraph
import com.example.kopilka.ui.theme.KopilkaTheme
import com.example.kopilka.ui.theme.toThemeMode
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    private val notifPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(

        )

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)

        //WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<DeadlineNotificationWorker>().build())

        setContent {
            val themeSetting by settingsDataStore.themeMode.collectAsState(initial = ThemeModeSetting.DARK)
            val darkTheme = when (themeSetting) {
                ThemeModeSetting.LIGHT -> false
                ThemeModeSetting.DARK -> true
                ThemeModeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            val themeModeSetting by settingsDataStore.themeMode.collectAsState(initial = ThemeModeSetting.DARK)
            KopilkaTheme(themeMode = themeModeSetting.toThemeMode()) {
                NavGraph()
            }
            }
        }
    }


