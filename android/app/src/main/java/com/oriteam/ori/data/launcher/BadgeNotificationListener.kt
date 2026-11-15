package com.oriteam.ori.data.launcher

import android.app.Notification
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.oriteam.ori.domain.launcher.badgeKeyOf
import com.oriteam.ori.domain.launcher.countsOnBadge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "BadgeListener"

/**
 * Counts the notifications of each app for the badges of ORI Home. Android binds it only while the person allows it
 * (Configurações > Notificações > Acesso a notificações), and it is turned on and off with ORI Home itself. Of each
 * notification it reads only the app, the profile, its number and whether it counts ([countsOnBadge]): never the
 * title, the text or anything inside, and nothing is logged, kept or sent.
 */
@AndroidEntryPoint
class BadgeNotificationListener : NotificationListenerService() {
    @Inject lateinit var badges: NotificationBadges

    private val users by lazy { getSystemService(UserManager::class.java) }

    override fun onListenerConnected() = recount()

    override fun onListenerDisconnected() = badges.clear()

    override fun onNotificationPosted(notification: StatusBarNotification, rankingMap: RankingMap) {
        badges.put(notification.key, appOf(notification), countOf(notification, rankingMap))
    }

    override fun onNotificationRemoved(notification: StatusBarNotification, rankingMap: RankingMap) {
        badges.remove(notification.key)
    }

    /** An app or a channel may turn its badges off: everything is counted again. */
    override fun onNotificationRankingUpdate(rankingMap: RankingMap) = recount()

    private fun recount() {
        val active = try {
            activeNotifications.orEmpty()
        } catch (exception: SecurityException) {
            // Asked while Android is still connecting the listener, or after it was disconnected.
            Log.w(TAG, "Notifications not readable now: ${exception.javaClass.simpleName}")
            return
        }
        val ranking = currentRanking
        badges.replaceAll(
            active.associate { it.key to (appOf(it) to countOf(it, ranking)) }.filterValues { it.second > 0 }
        )
    }

    private fun appOf(notification: StatusBarNotification): String =
        badgeKeyOf(notification.packageName, users.getSerialNumberForUser(notification.user))

    /** The number the app gives (as "3 mensagens"), or one; none for one that does not count. */
    private fun countOf(notification: StatusBarNotification, rankingMap: RankingMap?): Int {
        val ranking = Ranking()
        val canShowBadge = rankingMap?.getRanking(notification.key, ranking) != true || ranking.canShowBadge()
        val flags = notification.notification.flags
        val counts = countsOnBadge(
            isOngoing = notification.isOngoing || flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
            canShowBadge = canShowBadge
        )
        return if (counts) notification.notification.number.coerceAtLeast(1) else 0
    }
}
