package com.oriteam.ori.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.WallpaperTarget
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.theme.OriSpacing

/** The lock screen drawn over the home screen while its tab is chosen (a picture: the tab says what it is). */
internal const val LOCK_PREVIEW_TAG = "wallpaper-lock-preview"

/** The screen the preview shows: the lock screen or the home screen. */
private enum class PreviewedScreen(val label: Int) {
    LOCK(R.string.launcher_wallpaper_target_lock),
    HOME(R.string.launcher_wallpaper_target_home)
}

/**
 * Over the real home screen, which shows the wallpaper being tried (LauncherScreen), as One UI shows it before
 * "Concluído": "Tela de bloqueio" and "Tela inicial" switch between the two screens with it (the lock screen drawn over
 * the home screen, with the time and the date), nothing under them can be touched, and only "Aplicar" keeps the
 * wallpaper. Back is "Cancelar". As Android asks, "Aplicar" offers the home screen, the lock screen or both, also for
 * the Android wallpaper (on the lock screen, Android then shows its own wallpaper again).
 *
 * The buttons sit over the dots and the dock ([HomeMetrics.bottomHeight]), never on them, on a card that reads on any
 * wallpaper: the dock is part of what is being tried. On a wide screen they are one row; on a phone, two.
 */
@Composable
internal fun WallpaperPreviewControls(state: LauncherUiState, viewModel: LauncherViewModel, metrics: HomeMetrics) {
    val preview = state.wallpaperDraft.preview ?: return
    BackHandler { viewModel.wallpaper.onCancel() }
    val canSetLock = viewModel.wallpaper.canSetLockScreen
    var shown by remember { mutableStateOf(PreviewedScreen.HOME) }
    var isChoosingTarget by remember { mutableStateOf(false) }
    val lockColors = lockScreenColors(preview)
    val apply = { target: WallpaperTarget ->
        isChoosingTarget = false
        viewModel.wallpaper.onApply(target, lockColors)
        viewModel.onEditingChange(true)
    }
    if (isChoosingTarget) WallpaperTargetDialog(onChoose = apply, onDismiss = { isChoosingTarget = false })
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } })
        if (shown == PreviewedScreen.LOCK) {
            LockScreenPicture(preview, modifier = Modifier.testTag(LOCK_PREVIEW_TAG).clearAndSetSemantics { })
        }
        PreviewCard(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = metrics.bottomHeight)) {
            if (canSetLock) ScreenTabs(shown, onSelect = { shown = it })
            Row(horizontalArrangement = Arrangement.spacedBy(OriSpacing.Medium)) {
                OriSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = viewModel.wallpaper::onCancel,
                    isFullWidth = false
                )
                OriPrimaryButton(
                    text = stringResource(R.string.launcher_wallpaper_apply),
                    onClick = { if (canSetLock) isChoosingTarget = true else apply(WallpaperTarget.HOME) },
                    isFullWidth = false
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCard(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(OriSpacing.Small)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Large, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(OriSpacing.Small)
        ) { content() }
    }
}

/** Which screen the preview shows, as tabs: what they change is what is seen. */
@Composable
private fun ScreenTabs(shown: PreviewedScreen, onSelect: (PreviewedScreen) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.selectableGroup()
    ) {
        PreviewedScreen.entries.forEach { screen ->
            OriChoiceChip(
                label = stringResource(screen.label),
                isSelected = screen == shown,
                onClick = { onSelect(screen) },
                role = Role.Tab
            )
        }
    }
}
