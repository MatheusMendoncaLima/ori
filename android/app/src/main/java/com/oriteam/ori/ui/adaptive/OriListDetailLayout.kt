package com.oriteam.ori.ui.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * List and detail that share the window when it is wide and alternate when it is narrow.
 *
 * - Wide window ([WindowLayout.showsListAndDetailTogether]): both panes are visible; with no
 *   selection the detail pane shows [emptyDetailPane].
 * - Narrow window: the list fills the screen; choosing an item opens its detail and the system
 *   back gesture returns to the list.
 *
 * The selection survives rotation, resizing and process death, so [T] must be storable in a
 * Bundle (use an id such as `String` or `Long`, not a whole object). Hinges of foldables are
 * avoided by the underlying Material scaffold.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : Any> OriListDetailLayout(
    listPane: @Composable (onItemSelected: (T) -> Unit) -> Unit,
    detailPane: @Composable (selectedItem: T, onNavigateBack: (() -> Unit)?) -> Unit,
    emptyDetailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowLayout = LocalWindowLayout.current
    val paneCount = if (windowLayout.showsListAndDetailTogether) 2 else 1
    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
        .copy(maxHorizontalPartitions = paneCount)
    val navigator = rememberListDetailPaneScaffoldNavigator<T>(scaffoldDirective = directive)
    val scope = rememberCoroutineScope()
    val navigateBack: () -> Unit = { scope.launch { navigator.navigateBack() } }

    BackHandler(enabled = navigator.canNavigateBack()) { navigateBack() }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                listPane { item ->
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item) }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val selectedItem = navigator.currentDestination?.contentKey
                if (selectedItem == null) {
                    emptyDetailPane()
                } else {
                    // A back button is only meaningful when the list is hidden.
                    detailPane(selectedItem, if (paneCount == 1) navigateBack else null)
                }
            }
        },
        modifier = modifier
    )
}
