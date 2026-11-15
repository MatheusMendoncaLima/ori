package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.DrawerStyle
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.LauncherIconSize
import com.oriteam.ori.domain.launcher.LauncherSettings
import com.oriteam.ori.domain.launcher.isMonochrome
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val HandleWidth = 32.dp
private val HandleHeight = 4.dp

/** The sheet of all the apps is almost opaque: the wallpaper only shows a little through it, as on Android. */
private const val SHEET_OPACITY = 0.97f
private val SheetCorner = 28.dp

/**
 * All the apps over the home screen, moved by [sheet]: they come up from the bottom with the finger and go back
 * down the same way. The movement is applied while drawing, so following the finger never recomposes the list.
 * They are made ahead and stay made when closed ([isShown] false): out of sight, not drawn and not read.
 */
@Composable
internal fun DrawerLayer(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    sheet: DrawerSheet,
    carry: AppCarry,
    metrics: HomeMetrics,
    isShown: Boolean
) {
    Surface(
        shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
        color = MaterialTheme.colorScheme.background.copy(alpha = SHEET_OPACITY),
        // Said outright: a see-through color does not match the theme, and the writing would keep the home's.
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1f - sheet.value) * size.height
                // Closed, nothing of it is drawn until it comes up.
                alpha = if (sheet.value > 0f) 1f else 0f
            }
            // Closed, the screen reader does not find it either.
            .then(if (isShown) Modifier else Modifier.clearAndSetSemantics { })
    ) {
        // The writing color of the home screen stays on the home screen: here it is the theme's.
        CompositionLocalProvider(LocalHomeInk provides null) {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                AppDrawer(state, viewModel, sheet, carry, metrics)
            }
        }
    }
}

/**
 * All the apps, as on Android: a handle and a search at the top, then every app in alphabetical order, with the
 * columns and the icon size of the home screen ([metrics]): on a tablet turned, the transposed grid, as One UI does.
 * They show as the person chose (DrawerStyle): a list that scrolls down, or pages side by side, as the home screen
 * ([AppPages]). A tap opens the app; holding it opens its menu (put on the home screen or take it off, the app's page,
 * uninstall). Pulling the top (or the list at its top) down takes the list down with the finger.
 *
 * Without a [sheet], all the apps are the whole of ORI Home ("Só todos os apps"): no handle and nothing to pull down,
 * [header] at the top, and nothing carried to a home screen there is not ([carry] null).
 */
@Composable
internal fun AppDrawer(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    sheet: DrawerSheet?,
    carry: AppCarry?,
    metrics: HomeMetrics,
    header: (@Composable () -> Unit)? = null
) {
    val pullDown = rememberDraggableState { delta -> sheet?.dragBy(delta) }
    val pullsDown = if (sheet == null) {
        Modifier
    } else {
        Modifier.draggable(pullDown, Orientation.Vertical, onDragStopped = { velocity -> sheet.settle(velocity) })
    }
    // Closed, all the apps stay made: the next opening starts at the top, and without the keyboard of the search.
    val isOnScreen = sheet?.isVisible ?: true
    val focus = LocalFocusManager.current
    var isSearching by remember { mutableStateOf(false) }
    LaunchedEffect(isOnScreen) { if (!isOnScreen && isSearching) focus.clearFocus() }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .then(pullsDown)
                .padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small)
        ) {
            if (sheet != null) {
                Box(
                    modifier = Modifier
                        .size(HandleWidth, HandleHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        .clearAndSetSemantics { }
                )
            }
            header?.invoke()
            SearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                // The search key opens the first app found, as on other home screens.
                onSearch = { state.drawer.firstOrNull()?.let(viewModel::onOpenApp) },
                onFocusChange = { isSearching = it }
            )
            FormMessage(messageRes = state.messageRes, type = FormMessageType.ERROR)
        }
        if (state.drawer.isEmpty() && !state.isLoading) {
            EmptyState(
                title = stringResource(R.string.launcher_search_empty_title),
                message = stringResource(R.string.launcher_search_empty_message),
                modifier = Modifier.weight(1f)
            )
        } else if (state.settings.drawerStyle == DrawerStyle.PAGES) {
            AppPages(state, viewModel, carry, metrics, isOnScreen, Modifier.weight(1f).then(pullsDown))
        } else {
            AppsGrid(state, viewModel, sheet, carry, metrics, isOnScreen, Modifier.weight(1f))
        }
    }
}

/** The search, as wide as the readable width at most, with a way to clear it. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    val label = stringResource(R.string.launcher_search_placeholder)
    val clearLabel = stringResource(R.string.launcher_search_clear)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = CircleShape,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = clearLabel)
                }
            }
        },
        placeholder = { Text(text = label) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = Modifier
            .widthIn(max = OriDimensions.ContentMaxWidth)
            .fillMaxWidth()
            .padding(top = OriSpacing.Medium)
            .onFocusChanged { onFocusChange(it.isFocused) }
    )
}

@Composable
private fun AppsGrid(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    sheet: DrawerSheet?,
    carry: AppCarry?,
    metrics: HomeMetrics,
    isOnScreen: Boolean,
    modifier: Modifier
) {
    val onHome = state.favoriteKeys
    val grid = rememberLazyGridState()
    // A new search starts at the top: the grid would keep the first app it showed and hide the ones before it.
    LaunchedEffect(state.query) { grid.scrollToItem(0) }
    LaunchedEffect(isOnScreen) { if (!isOnScreen) grid.scrollToItem(0) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(metrics.grid.columns),
        state = grid,
        contentPadding = PaddingValues(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = modifier
            .fillMaxWidth()
            .then(if (sheet == null) Modifier else Modifier.nestedScroll(remember(sheet) { sheet.connection() }))
    ) {
        items(state.drawer, key = { it.key }) { app ->
            val tile = drawerTileOf(state.settings, metrics, hasHome = carry != null)
            val isOnHome = app.key in onHome
            DrawerApp(app, tile, viewModel, carry) { listOfNotNull(homeToggle(app, isOnHome, tile, viewModel)) }
        }
    }
}

/** How the apps of all the apps are drawn, with the settings of the home screen. */
internal fun drawerTileOf(settings: LauncherSettings, metrics: HomeMetrics, hasHome: Boolean) = DrawerTile(
    settings.iconSize,
    if (metrics.isTiles) tileIconSize(metrics) else metrics.iconSize,
    settings.isMonochrome,
    settings.isLayoutLocked,
    hasHome = hasHome,
    tileHeight = metrics.cellHeight.takeIf { metrics.isTiles }
)

/**
 * "Adicionar à tela inicial", or "Remover da tela inicial" (not on a locked home screen); nothing when there is no
 * home screen ("Só todos os apps").
 */
internal fun homeToggle(app: LauncherApp, isOnHome: Boolean, tile: DrawerTile, viewModel: LauncherViewModel) = when {
    !tile.hasHome -> null
    !isOnHome -> MenuEntry(R.string.launcher_menu_add_home, Icons.Filled.Add) { viewModel.onToggleFavorite(app) }
    tile.isLayoutLocked -> null
    else -> MenuEntry(R.string.launcher_menu_remove_home, Icons.Filled.Close) { viewModel.onToggleFavorite(app) }
}

/** How an app of all the apps is drawn: the writing of its size, the icon of this screen, grey or not. */
internal data class DrawerTile(
    val size: LauncherIconSize,
    val icon: Dp,
    val isMonochrome: Boolean,
    /** The home screen is locked: an app held opens its menu and is not carried to it. */
    val isLayoutLocked: Boolean,
    /** The lines of the name under the icon: none in the dock, as on Android. */
    val labelLines: Int = 2,
    /** There is a home screen to carry the app to and to add it to (not with "Só todos os apps"). */
    val hasHome: Boolean = true,
    /**
     * With "Máximo", the height of a tile (a cell of the home screen): the app is a tile of its own color with the
     * name inside, as on the home screen. Null: an icon with its name under it.
     */
    val tileHeight: Dp? = null
)
