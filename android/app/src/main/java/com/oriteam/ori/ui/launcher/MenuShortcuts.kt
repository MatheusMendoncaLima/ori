package com.oriteam.ori.ui.launcher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.ui.components.text.Text

/** The picture of a shortcut in the menu: the size of the icons of the other choices, a little larger. */
private val ShortcutIconSize = 28.dp

/**
 * What the menu of an app needs to show its shortcuts, as on One UI: reading them (only when the menu opens), opening
 * one, and putting one on the home screen ([onAdd] is null where that is not possible: a locked home screen).
 */
internal class MenuShortcuts(
    val app: LauncherApp,
    val load: suspend (LauncherApp) -> List<LauncherApp>,
    val onOpen: (LauncherApp) -> Unit,
    val onAdd: ((LauncherApp) -> Unit)?,
    val isOnHome: (String) -> Boolean,
    val isMonochrome: Boolean
)

/** The shortcuts of [app] for its menu, reading what is on the home screen at the moment the menu opens. */
internal fun LauncherViewModel.menuShortcuts(app: LauncherApp, isLocked: Boolean, isMonochrome: Boolean) =
    MenuShortcuts(
        app = app,
        load = appMenu::shortcutsOf,
        onOpen = { onOpenApp(it) },
        onAdd = { shortcut: LauncherApp -> onToggleFavorite(shortcut) }.takeUnless { isLocked },
        isOnHome = { it in uiState.value.favoriteKeys },
        isMonochrome = isMonochrome
    )

/**
 * The shortcuts at the top of the menu, each with its picture: a tap opens it; "+" puts it on the home screen (as
 * holding and dragging it does on One UI, which not everyone can do). Nothing shows while they are read, or when the
 * app has none; a line parts them from the choices of the menu.
 */
@Composable
internal fun ShortcutRows(shortcuts: MenuShortcuts, onDismiss: () -> Unit) {
    val list by produceState(emptyList<LauncherApp>(), shortcuts.app.key) { value = shortcuts.load(shortcuts.app) }
    if (list.isEmpty()) return
    list.forEach { shortcut ->
        val add = shortcuts.onAdd?.takeUnless { shortcuts.isOnHome(shortcut.key) }
        DropdownMenuItem(
            text = {
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { AppIcon(shortcut, ShortcutIconSize, isMonochrome = shortcuts.isMonochrome) },
            trailingIcon = add?.let {
                {
                    IconButton(
                        onClick = {
                            onDismiss()
                            it(shortcut)
                        }
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.launcher_shortcut_add_named, shortcut.label)
                        )
                    }
                }
            },
            onClick = {
                onDismiss()
                shortcuts.onOpen(shortcut)
            }
        )
    }
    HorizontalDivider()
}
