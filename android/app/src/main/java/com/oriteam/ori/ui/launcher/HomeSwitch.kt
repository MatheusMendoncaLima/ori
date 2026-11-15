package com.oriteam.ori.ui.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * The official way to ask Android for the home role: the role request (Android 10+), or the system screen of
 * default apps when the request is not available (older versions, some manufacturers). Android shows its own
 * screen; the person decides.
 */
fun homeRequestIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles != null && roles.canAskForHome()) return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun RoleManager.canAskForHome() = isRoleAvailable(RoleManager.ROLE_HOME) && !isRoleHeld(RoleManager.ROLE_HOME)

/**
 * Goes to the home app used before ORI Home ([previousPackage]); without it, or when it is gone, to the system
 * screen where the person picks one. Called after the home entry of ORI was turned off.
 */
fun openPreviousLauncher(context: Context, previousPackage: String?) {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = previousPackage?.let {
        try {
            context.startActivity(Intent(home).setPackage(it))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    } ?: false
    if (!opened) {
        try {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(home)
        }
    }
}
