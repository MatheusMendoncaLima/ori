package com.oriteam.ori

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.ui.app.OriApp
import com.oriteam.ori.ui.app.screens.preferences.AppPreferencesViewModel
import com.oriteam.ori.ui.components.text.LocalTextCaseToggle
import com.oriteam.ori.ui.theme.OriTheme
import com.oriteam.ori.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val preferencesViewModel: AppPreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by preferencesViewModel.preferences.collectAsStateWithLifecycle()
            // Nothing is drawn until the stored choices are read: it takes a few milliseconds.
            preferences?.let { current ->
                val isDark = current.account.theme.isDark()
                DisposableEffect(isDark) {
                    // The status bar icons follow the theme of the app, not the one of the system.
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose {}
                }
                OriTheme(preferences = current) {
                    CompositionLocalProvider(LocalTextCaseToggle provides preferencesViewModel::onToggleUppercase) {
                        OriApp(startArea = current.device.startArea)
                    }
                }
            }
        }
    }
}
