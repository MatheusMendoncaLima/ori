package com.oriteam.ori.ui.launcher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.HomeGrid
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.WallpaperColor
import com.oriteam.ori.domain.launcher.WallpaperGradient
import com.oriteam.ori.domain.launcher.arranged
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.domain.launcher.photosInUse
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.theme.OriSpacing

private val gradientNames = mapOf(
    WallpaperGradient.DAWN to R.string.launcher_gradient_dawn,
    WallpaperGradient.SEA to R.string.launcher_gradient_sea,
    WallpaperGradient.MEADOW to R.string.launcher_gradient_meadow,
    WallpaperGradient.DUSK to R.string.launcher_gradient_dusk,
    WallpaperGradient.SAND to R.string.launcher_gradient_sand
)

private val colorNames = mapOf(
    WallpaperColor.MINT to R.string.launcher_wallpaper_mint,
    WallpaperColor.BLUE to R.string.launcher_wallpaper_blue,
    WallpaperColor.LILAC to R.string.launcher_wallpaper_lilac,
    WallpaperColor.YELLOW to R.string.launcher_wallpaper_yellow,
    WallpaperColor.PINK to R.string.launcher_wallpaper_pink
)

/** One choice of the picker. */
private class WallpaperChoice(val label: String, val wallpaper: LauncherWallpaper)

/** A title or a whole-width part of the grid: it takes the whole row. */
private val wholeRow: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

/**
 * The wallpaper, chosen as on One UI: first how the lock screen and the home screen look now, then a grid of small
 * screens with each wallpaper. "Do aparelho" has the Android wallpaper (the real one, seen through the window), a new
 * photo and the photos used before (hold one to take it out of the history); then soft gradients and calm colors. A
 * choice opens the preview of the lock and the home screen with it; only "Aplicar" changes the wallpaper.
 */
@Composable
internal fun WallpaperPicker(state: LauncherUiState, viewModel: LauncherViewModel, grid: HomeGrid) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.wallpaper.onPhotoPicked(it.toString()) }
    }
    val pickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val gradients = WallpaperGradient.entries.map {
        WallpaperChoice(stringResource(gradientNames.getValue(it)), LauncherWallpaper.Gradient(it))
    }
    val theme = WallpaperChoice(stringResource(R.string.launcher_wallpaper_theme), LauncherWallpaper.Theme)
    val colors = listOf(theme) + WallpaperColor.entries.map {
        WallpaperChoice(stringResource(colorNames.getValue(it)), LauncherWallpaper.Solid(it))
    }
    val settings = state.settings
    val home = remember(state.home, state.dock, settings, grid) {
        miniHomeOf(
            placed = settings.arranged(state.home.map { it.key }, grid),
            entries = state.home.associateBy { it.key },
            grid = grid,
            mainPage = settings.mainPage,
            dock = if (settings.showsDock) state.dock else emptyList()
        )
    }
    val minWidth = if (screenRatio() > 1f) ThumbnailMinWidthTurned else ThumbnailMinWidthUpright
    LauncherPaneScaffold(title = stringResource(R.string.launcher_edit_wallpaper), onBack = viewModel::onBack) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minWidth),
            horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
            contentPadding = PaddingValues(bottom = OriSpacing.Large),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "now", span = wholeRow) {
                WallpaperNow(settings.wallpaper, state.lockScreenShown, home, settings.isMonochrome)
            }
            deviceChoices(state, viewModel, pickPhoto)
            item(key = "message", span = wholeRow) {
                FormMessage(messageRes = state.messageRes, type = FormMessageType.ERROR)
            }
            choiceGroup("gradients", R.string.launcher_wallpaper_gradients, gradients, state, viewModel)
            choiceGroup("colors", R.string.launcher_wallpaper_colors, colors, state, viewModel)
        }
    }
}

/** The Android wallpaper, a new photo, and the photos used before, newest first. */
private fun LazyGridScope.deviceChoices(state: LauncherUiState, viewModel: LauncherViewModel, onPickPhoto: () -> Unit) {
    val current = state.settings.wallpaper
    item(key = "device", span = wholeRow) { SectionHeader(text = stringResource(R.string.launcher_wallpaper_device)) }
    item(key = "system") {
        ChoiceFrame(
            label = stringResource(R.string.launcher_wallpaper_system),
            isSelected = current == LauncherWallpaper.System,
            onClick = { viewModel.wallpaper.onPreview(LauncherWallpaper.System) }
        ) { AndroidWallpaperWindow() }
    }
    item(key = "new-photo") {
        ChoiceFrame(
            label = stringResource(R.string.launcher_wallpaper_my_photos),
            isSelected = false,
            onClick = onPickPhoto
        ) { IconPicture(Icons.Filled.Add, isBusy = state.wallpaperDraft.isImporting) }
    }
    val inUse = state.settings.photosInUse
    itemsIndexed(state.settings.wallpaperHistory, key = { _, fileName -> fileName }) { index, fileName ->
        val photo = LauncherWallpaper.Photo(fileName)
        val remove = MenuEntry(R.string.launcher_wallpaper_remove_history, Icons.Filled.Delete) {
            viewModel.wallpaper.onRemoveFromHistory(fileName)
        }
        ChoiceFrame(
            // Numbered, newest first: each photo has a name of its own, also for the screen reader.
            label = stringResource(R.string.launcher_wallpaper_history_photo, index + 1),
            isSelected = current == photo,
            onClick = { viewModel.wallpaper.onPreview(photo) },
            // A photo a screen shows (the home or the lock screen) stays in the history.
            menu = listOf(remove).takeUnless { fileName in inUse }
        ) { PhotoThumbnail(fileName) }
    }
}

private fun LazyGridScope.choiceGroup(
    key: String,
    title: Int,
    choices: List<WallpaperChoice>,
    state: LauncherUiState,
    viewModel: LauncherViewModel
) {
    item(key = key, span = wholeRow) { SectionHeader(text = stringResource(title)) }
    items(choices, key = { "$key-${it.label}" }) { choice ->
        ChoiceFrame(
            label = choice.label,
            isSelected = state.settings.wallpaper == choice.wallpaper,
            onClick = { viewModel.wallpaper.onPreview(choice.wallpaper) }
        ) { WallpaperBackground(choice.wallpaper) }
    }
}
