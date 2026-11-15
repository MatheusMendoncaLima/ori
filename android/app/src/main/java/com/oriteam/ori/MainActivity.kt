package com.oriteam.ori

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.ui.app.OriApp
import com.oriteam.ori.ui.app.main.AreaRequest
import com.oriteam.ori.ui.app.screens.preferences.AppPreferencesViewModel
import com.oriteam.ori.ui.components.dialog.OriDialogHost
import com.oriteam.ori.ui.components.text.LocalAppLocale
import com.oriteam.ori.ui.components.text.LocalTextCaseToggle
import com.oriteam.ori.ui.components.text.rememberDeviceAppLocale
import com.oriteam.ori.ui.theme.OriTheme
import com.oriteam.ori.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val preferencesViewModel: AppPreferencesViewModel by viewModels()

    /** An area asked by ORI Home after the app was open (onNewIntent); the first one is the start area. */
    private var areaRequest by mutableStateOf<AreaRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedArea = areaOf(intent)
        enableEdgeToEdge()
        setContent {
            val preferences by preferencesViewModel.preferences.collectAsStateWithLifecycle()
            // Nothing is drawn until the stored choices are read: it takes a few milliseconds.
            preferences?.let { current ->
                val isDark = current.account.theme.isDark()
                // The status bar icons follow the theme of the app, not the one of the system.
                LaunchedEffect(isDark) {
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                OriTheme(preferences = current) {
                    CompositionLocalProvider(
                        LocalTextCaseToggle provides preferencesViewModel::onToggleUppercase,
                        // Dates and day names in the language of the texts, which follows the device.
                        LocalAppLocale provides rememberDeviceAppLocale()
                    ) {
                        // ORI Home opens the routine or communication directly (EXTRA_START_AREA).
                        OriDialogHost {
                            OriApp(startArea = requestedArea ?: current.device.startArea, areaRequest = areaRequest)
                        }
                    }
                }
            }
        }
    }

    /**
     * ORI Home brings the open app back instead of making a new one (opening it again cost about 170 ms on the test
     * tablet, against 50 to 70 ms to bring it back), and says which area to show.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        areaOf(intent)?.let { areaRequest = AreaRequest(it) }
    }

    private fun areaOf(intent: Intent): StartArea? = intent.getStringExtra(EXTRA_START_AREA)
        ?.let { name -> StartArea.entries.firstOrNull { it.name == name } }

    companion object {
        /** The area to open (a [StartArea] name), sent by the shortcuts of ORI Home. */
        const val EXTRA_START_AREA = "com.oriteam.ori.extra.START_AREA"
    }
}
