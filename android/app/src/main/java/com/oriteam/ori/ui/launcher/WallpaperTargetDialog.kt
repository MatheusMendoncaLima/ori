package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.WallpaperTarget
import com.oriteam.ori.ui.components.dialog.OriAlertDialog
import com.oriteam.ori.ui.components.form.OriTextButton

private val targetLabels = listOf(
    WallpaperTarget.HOME to R.string.launcher_wallpaper_target_home,
    WallpaperTarget.LOCK to R.string.launcher_wallpaper_target_lock,
    WallpaperTarget.BOTH to R.string.launcher_wallpaper_target_both
)

/** Where the wallpaper goes, as Android asks before "Definir": the home screen, the lock screen, or both. */
@Composable
internal fun WallpaperTargetDialog(onChoose: (WallpaperTarget) -> Unit, onDismiss: () -> Unit) {
    OriAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.launcher_wallpaper_target_title),
        text = {
            Column {
                targetLabels.forEach { (target, label) ->
                    OriTextButton(text = stringResource(label), onClick = { onChoose(target) })
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OriTextButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss
            )
        }
    )
}
