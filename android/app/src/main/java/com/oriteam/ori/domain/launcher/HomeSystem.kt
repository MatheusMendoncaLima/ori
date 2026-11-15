package com.oriteam.ori.domain.launcher

import kotlinx.coroutines.flow.Flow

/**
 * What ORI Home asks of Android around the home screen, beyond the apps and the widgets: its notification panel and
 * the count of notifications for the badges. Kept behind this contract so the screen and its tests do not depend on
 * Android.
 */
interface HomeSystem {
    /**
     * How many notifications each app shows now, by [badgeKeyOf]; empty while the person does not allow ORI Home to
     * count them. Only in memory: what the notifications say is never read.
     */
    val notificationCounts: Flow<Map<String, Int>>

    /** Whether the person allows ORI Home to count the notifications (Android's "Acesso a notificações"). */
    fun canCountNotifications(): Boolean

    /** Opens Android's page where the person allows it or takes it back; false when it cannot be opened. */
    fun openNotificationAccess(): Boolean

    /**
     * Pulls down Android's notification panel, as a swipe down on the home screen of One UI does. False when this
     * version of Android does not let a home screen do it.
     */
    fun openNotifications(): Boolean
}
