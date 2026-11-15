package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.HomeSystem
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.LauncherSettingsRepository
import com.oriteam.ori.domain.launcher.withHiddenApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The choices of how ORI Home behaves, as the settings of One UI's home screen: the swipe down for the notifications,
 * new apps on the home screen and whether the home screen of a phone turns. Apart from how it looks
 * ([LauncherAppearanceEditor]).
 */
class HomeBehaviorEditor internal constructor(
    private val settings: LauncherSettingsRepository,
    private val scope: CoroutineScope,
    private val system: HomeSystem
) {
    /** "Mostrar onde o app vai ficar": the frame of the cell while an app is carried (widgets always have it). */
    fun onDropPreviewChange(value: Boolean) = update { it.copy(showsDropPreview = value) }

    /** "Mostrar apps recentes na barra" (a tablet's): the last apps opened, after the dock's own. */
    fun onRecentAppsChange(value: Boolean) = update { it.copy(showsRecentApps = value) }

    /** "Ocultar apps": the apps hidden as chosen in the list; the newly hidden ones leave the home screen. */
    fun onHiddenAppsChange(hidden: Set<String>) = update { it.withHiddenApps(hidden) }

    /** "Selos dos ícones de apps": off, a dot or the number. */
    fun onBadgesChange(style: BadgeStyle) = update { it.copy(badges = style) }

    /** Whether the person lets ORI Home count the notifications; read again each time the options come into view. */
    fun canCountNotifications(): Boolean = system.canCountNotifications()

    /** Android's page where the person allows the counting, or takes it back. */
    fun onOpenNotificationAccess() {
        system.openNotificationAccess()
    }

    /** "Deslizar para baixo para ver as notificações". */
    fun onSwipeDownChange(value: Boolean) = update { it.copy(swipesDownForNotifications = value) }

    /** "Adicionar novos apps à tela inicial". */
    fun onAddsNewAppsChange(value: Boolean) = update { it.copy(addsNewApps = value) }

    /** "Girar a tela inicial no celular": a phone turned gets the grid transposed, as a tablet. */
    fun onTurnsOnPhoneChange(value: Boolean) = update { it.copy(turnsOnPhone = value) }

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        scope.launch { settings.update(transform) }
    }
}
