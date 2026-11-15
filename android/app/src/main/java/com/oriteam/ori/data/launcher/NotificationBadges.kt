package com.oriteam.ori.data.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How many notifications each app shows now, by "package@profile", as [BadgeNotificationListener] counts them. Only
 * in memory, for the badges of ORI Home: nothing of it is written or sent, and it empties when Android stops the
 * listener (the person took the access back).
 */
@Singleton
class NotificationBadges @Inject constructor() {
    /** Each notification counted, by Android's key for it, with the app it belongs to and how much it counts. */
    private val counted = mutableMapOf<String, Pair<String, Int>>()
    private val state = MutableStateFlow<Map<String, Int>>(emptyMap())

    val counts: StateFlow<Map<String, Int>> = state.asStateFlow()

    /** All the notifications at once, when the listener connects or Android ranks them again. */
    @Synchronized
    fun replaceAll(notifications: Map<String, Pair<String, Int>>) {
        counted.clear()
        counted.putAll(notifications)
        publish()
    }

    /** One notification posted or updated ([count] 0: it no longer counts, as one that became ongoing). */
    @Synchronized
    fun put(notificationKey: String, app: String, count: Int) {
        if (count > 0) counted[notificationKey] = app to count else counted.remove(notificationKey)
        publish()
    }

    @Synchronized
    fun remove(notificationKey: String) {
        if (counted.remove(notificationKey) != null) publish()
    }

    @Synchronized
    fun clear() = replaceAll(emptyMap())

    private fun publish() {
        val sums = counted.values.groupingBy { it.first }.fold(0) { sum, (_, count) -> sum + count }
        state.update { if (it == sums) it else sums }
    }
}
