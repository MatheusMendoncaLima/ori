package com.oriteam.ori.data.launcher

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.oriteam.ori.domain.launcher.HomeSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OriHomeSystem"

/** The name Android gives its status bar service; there is no constant for it in the public API. */
private const val STATUS_BAR_SERVICE = "statusbar"

/** Android's notification panel, as home screens open it. */
@Singleton
class AndroidHomeSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    badges: NotificationBadges
) : HomeSystem {
    override val notificationCounts: Flow<Map<String, Int>> = badges.counts

    override fun canCountNotifications(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Android's page for this listener (Android 11+), or the list of every app with access. Opening it changes
     * nothing: the person decides there.
     */
    override fun openNotificationAccess(): Boolean {
        val pages = listOfNotNull(detailPage(), Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        return pages.any { page ->
            try {
                context.startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (exception: ActivityNotFoundException) {
                Log.w(TAG, "Notification access page not found: ${exception.javaClass.simpleName}")
                false
            }
        }
    }

    /**
     * Android has no public call to open its panel: home screens (Launcher3 and the others) call the panel's own
     * method, `StatusBarManager.expandNotificationsPanel`, with the normal permission EXPAND_STATUS_BAR (in the
     * manifest, no question to the person). When a version of Android does not allow it, nothing happens (false):
     * the swipe down simply does nothing, as on a home screen without that option.
     */
    /** The page of this listener alone, on Android 11+; null before. */
    private fun detailPage(): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(context, BadgeNotificationListener::class.java).flattenToString()
        )
    } else {
        null
    }

    @SuppressLint("WrongConstant", "PrivateApi", "DiscouragedPrivateApi")
    override fun openNotifications(): Boolean = try {
        val service = context.getSystemService(STATUS_BAR_SERVICE)
        if (service == null) {
            false
        } else {
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
            true
        }
    } catch (exception: ReflectiveOperationException) {
        Log.w(TAG, "The notification panel could not be opened: ${exception.javaClass.simpleName}")
        false
    } catch (exception: SecurityException) {
        Log.w(TAG, "The notification panel could not be opened: ${exception.javaClass.simpleName}")
        false
    }
}
