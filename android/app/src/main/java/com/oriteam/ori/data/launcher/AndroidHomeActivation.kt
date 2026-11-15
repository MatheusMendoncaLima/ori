package com.oriteam.ori.data.launcher

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.oriteam.ori.domain.launcher.HomeActivation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The home entry of the manifest (ui/launcher/LauncherActivity), named here so data does not depend on the screen. */
const val LAUNCHER_ACTIVITY_CLASS = "com.oriteam.ori.ui.launcher.LauncherActivity"

/** What answers "Adicionar à tela inicial" (ui/launcher/PinRequestActivity): only while ORI Home is on. */
private const val PIN_REQUEST_ACTIVITY_CLASS = "com.oriteam.ori.ui.launcher.PinRequestActivity"

/** The package Android shows when there is no default home: the "choose an app" screen. */
private const val RESOLVER_PACKAGE = "android"

/** Asks Android who the home app is, and turns the home entry of ORI on and off. */
class AndroidHomeActivation @Inject constructor(
    @ApplicationContext private val context: Context
) : HomeActivation {
    private val packageManager = context.packageManager

    override fun isOriHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(
                    RoleManager.ROLE_HOME
                )
            }
        }
        return currentHomePackage() == context.packageName
    }

    override fun currentHomePackage(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.takeIf { it != RESOLVER_PACKAGE }
    }

    override fun setLauncherEnabled(isEnabled: Boolean) {
        val state = if (isEnabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        // Each entry that answers for ORI Home; one already in that state is not written again.
        listOf(LAUNCHER_ACTIVITY_CLASS, PIN_REQUEST_ACTIVITY_CLASS, BadgeNotificationListener::class.java.name)
            .map { ComponentName(context.packageName, it) }
            .filter { packageManager.getComponentEnabledSetting(it) != state }
            .forEach { packageManager.setComponentEnabledSetting(it, state, PackageManager.DONT_KILL_APP) }
    }
}
