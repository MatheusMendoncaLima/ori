package com.oriteam.ori.ui.launcher

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.oriteam.ori.MainActivity
import com.oriteam.ori.data.launcher.AndroidInstalledApps
import com.oriteam.ori.data.launcher.AndroidWidgetHost
import com.oriteam.ori.data.launcher.DeviceWallpaperStore
import com.oriteam.ori.data.launcher.previewViewOf
import com.oriteam.ori.data.launcher.setListening
import com.oriteam.ori.data.launcher.widgetLabelOf
import com.oriteam.ori.domain.launcher.HomeActivation
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.ui.components.dialog.OriDialogHost
import com.oriteam.ori.ui.components.text.LocalAppLocale
import com.oriteam.ori.ui.components.text.rememberDeviceAppLocale
import com.oriteam.ori.ui.theme.OriAppColors
import com.oriteam.ori.ui.theme.OriTheme
import com.oriteam.ori.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * ORI Home, the optional home screen. It is its own activity, turned off in the manifest until the person asks for
 * it. Its window shows the Android wallpaper (the default background), so the screen itself is transparent and each
 * part paints what it needs. Nothing of the account is read until the panel on the left is opened.
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    @Inject lateinit var installedApps: AndroidInstalledApps

    @Inject lateinit var wallpapers: DeviceWallpaperStore

    @Inject lateinit var widgetHost: AndroidWidgetHost

    @Inject lateinit var activation: HomeActivation

    @Inject lateinit var widgetSteps: WidgetStepResults

    private val systemWallpaperLook = mutableStateOf(SystemWallpaperLook())

    /** The widget views on the screen, to lift one when it is held. */
    private val shownWidgets = ShownWidgetViews()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = homeOrientationFor(resources.configuration.smallestScreenWidthDp)
        enableEdgeToEdge()
        keepNavigationBarClear()
        followWidgetRequests()
        // An update may bring an entry that answers for ORI Home (the "Adicionar à tela inicial" of the apps): it is
        // turned on with the home entry, also for whoever turned ORI Home on before.
        lifecycleScope.launch(Dispatchers.IO) { activation.setLauncherEnabled(true) }
        setContent {
            val preferences by viewModel.appPreferences.collectAsStateWithLifecycle()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // A phone turns only when the person chose so (One UI's "Girar para o modo paisagem").
            val turnsOnPhone = state.settings.turnsOnPhone
            LaunchedEffect(turnsOnPhone) {
                requestedOrientation = homeOrientationFor(resources.configuration.smallestScreenWidthDp, turnsOnPhone)
            }
            preferences?.let { current ->
                val isDark = current.account.theme.isDark()
                LaunchedEffect(isDark) {
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    keepNavigationBarClear()
                }
                OriTheme(preferences = current) {
                    CompositionLocalProvider(
                        LocalAppLocale provides rememberDeviceAppLocale(),
                        LocalAppIconLoader provides rememberIconLoader(state.settings.iconShape),
                        LocalCachedAppIcon provides rememberCachedIcons(state.settings.iconShape),
                        LocalWallpaperLoader provides wallpapers::load,
                        LocalWallpaperThumbnailLoader provides wallpapers::thumbnail,
                        LocalWidgetPreviewLoader provides widgetHost::previewOf,
                        LocalWidgetPreviewView provides widgetHost::previewViewOf,
                        LocalWidgetLabelLoader provides { widgetId -> widgetLabelOf(applicationContext, widgetId) },
                        LocalSystemWallpaperLook provides systemWallpaperLook.value
                    ) {
                        OriAppColors { OriDialogHost { LauncherContent(state) } }
                        // The lock screen may have changed in ORI itself: its preview is read again on opening.
                        val isWallpaperOpen = state.pane == LauncherPane.WALLPAPER
                        LaunchedEffect(isWallpaperOpen) { if (isWallpaperOpen) readWallpaperLook() }
                    }
                }
            }
        }
    }

    /** The icons in [shape]: a new loader when the shape changes, so every icon is read again in it. */
    @Composable
    private fun rememberIconLoader(shape: IconShape): suspend (String) -> Bitmap? =
        remember<suspend (String) -> Bitmap?>(shape) { { key -> installedApps.iconOf(key, shape) } }

    @Composable
    private fun rememberCachedIcons(shape: IconShape): (String) -> Bitmap? =
        remember(shape) { { key -> installedApps.cachedIconOf(key, shape) } }

    @Composable
    private fun LauncherContent(state: LauncherUiState) {
        val slots = remember {
            LauncherSlots(
                panel = { panel -> OriPanel(panel, ::openOri) },
                widget = { widgetId, modifier -> HostedWidget(widgetHost, widgetId, modifier, shownWidgets) },
                liftWidget = shownWidgets::lift
            )
        }
        // Transparent: the Android wallpaper, drawn behind the window, shows through the home screen. The writing
        // color is the theme's (a transparent surface would leave it black); the home screen sets its own.
        Surface(
            color = ComposeColor.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize()
        ) {
            LauncherScreen(
                state = state,
                viewModel = viewModel,
                actions = LauncherActions(
                    onOpenOri = ::openOri,
                    onLeaveHome = { leaveHome(state.settings.previousLauncher) }
                ),
                slots = slots
            )
        }
    }

    /** Android works the colors of a new wallpaper out a moment after it is set: the previews follow when it says. */
    private val wallpaperColorsChanged = WallpaperManager.OnColorsChangedListener { _, _ -> readWallpaperLook() }

    override fun onStart() {
        super.onStart()
        widgetHost.setListening(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.getInstance(this).addOnColorsChangedListener(wallpaperColorsChanged, Handler(mainLooper))
        }
    }

    /**
     * The Android wallpapers may have changed in the system settings: the writing and the previews follow them (the
     * lock screen too, while the wallpaper screen is open).
     */
    override fun onResume() {
        super.onResume()
        readWallpaperLook()
        if (viewModel.uiState.value.pane == LauncherPane.WALLPAPER) viewModel.wallpaper.onRefreshLockScreen()
    }

    /** Off the main thread: it asks Android. */
    private fun readWallpaperLook() {
        lifecycleScope.launch(Dispatchers.Default) {
            systemWallpaperLook.value = readSystemWallpaperLook(this@LauncherActivity)
        }
    }

    override fun onStop() {
        super.onStop()
        widgetHost.setListening(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.getInstance(this).removeOnColorsChangedListener(wallpaperColorsChanged)
        }
    }

    /** The Home button pressed while ORI Home is already shown: back to its first page, as any home screen does. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onHomePressed()
    }

    /**
     * Android's windows for a widget (the "allow" window, the app's setup) open from [WidgetStepActivity], whose
     * answers come back through [WidgetStepResults]: opened from the home screen, they would answer "canceled" at once.
     */
    private fun followWidgetRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.widgets.requests.collect { request ->
                    when (request) {
                        is WidgetRequest.AskToBind ->
                            startActivity(
                                WidgetStepActivity.bind(this@LauncherActivity, request.widgetId, request.providerId)
                            )
                        is WidgetRequest.SetUp -> startActivity(
                            WidgetStepActivity.setUp(this@LauncherActivity, request.widgetId)
                        )
                        is WidgetRequest.SetUpAgain -> startActivity(
                            WidgetStepActivity.setUpAgain(this@LauncherActivity, request.widgetId)
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                widgetSteps.results.collect { result ->
                    when (result.step) {
                        WidgetStep.BIND -> viewModel.widgets.onBindResult(result.widgetId, result.isDone)
                        WidgetStep.SET_UP -> viewModel.widgets.onSetUpResult(result.widgetId, result.isDone)
                        // The widget is on the home screen already: whatever the answer, it stays.
                        WidgetStep.SET_UP_AGAIN -> Unit
                    }
                }
            }
        }
    }

    /**
     * No band behind the navigation bar of three buttons: with an automatic style, Android draws one for contrast (seen
     * over a gradient as a dark strip at the bottom). A home screen shows its wallpaper to the bottom, as One UI does;
     * the buttons keep their own color, light or dark by the wallpaper.
     */
    private fun keepNavigationBarClear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    }

    /** The app already open comes back as it was, on [area] when one is asked; otherwise it opens. */
    private fun openOri(area: StartArea?) {
        val bringsBack = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_START_AREA, area?.name)
                .addFlags(bringsBack)
        )
    }

    /** ORI Home turns its home entry off and hands over to the launcher used before. */
    private fun leaveHome(previousLauncher: String?) {
        activation.setLauncherEnabled(false)
        openPreviousLauncher(this, previousLauncher)
        finish()
    }
}
