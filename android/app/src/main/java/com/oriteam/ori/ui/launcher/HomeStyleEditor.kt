package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.HomeScreenMode
import com.oriteam.ori.domain.launcher.IconShape
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The choices of how ORI Home is shown, apart from the [LauncherViewModel]: the shape of every icon, all the apps as a
 * list or as pages, and whether there is a home screen at all or only all the apps.
 */
class HomeStyleEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope
) {
    /** "Formato dos ícones": every icon round or square, whatever shape its app brings. */
    fun onIconShapeChange(shape: IconShape) = update { it.copy(iconShape = shape) }

    /** All the apps as a list that scrolls down, or on pages side by side, as the home screen. */
    fun onDrawerStyleChange(style: DrawerStyle) = update { it.copy(drawerStyle = style) }

    /**
     * The home screen and all the apps, or only all the apps. The items of the home screen stay where they are, so
     * going back to it finds everything in place.
     */
    fun onScreenModeChange(mode: HomeScreenMode) = update { it.copy(screenMode = mode) }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        scope.launch { settings.update(transform) }
    }
}
