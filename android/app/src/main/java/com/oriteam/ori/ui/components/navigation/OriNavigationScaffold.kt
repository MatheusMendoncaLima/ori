package com.oriteam.ori.ui.components.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.adaptive.NavigationLayout
import com.oriteam.ori.ui.components.text.Text

/**
 * Top-level navigation that follows the window: bottom bar on narrow windows, rail on medium
 * or short windows and a permanent drawer on wide windows (see [NavigationLayout]).
 *
 * Labels are always visible: icons alone are harder to understand for many ORI users.
 */
@Composable
fun OriNavigationScaffold(
    destinations: List<NavigationDestination>,
    selectedKey: String,
    onDestinationSelected: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val layoutType = when (LocalWindowLayout.current.navigationLayout) {
        NavigationLayout.BOTTOM_BAR -> NavigationSuiteType.NavigationBar
        NavigationLayout.RAIL -> NavigationSuiteType.NavigationRail
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            destinations.forEach { destination ->
                item(
                    selected = destination.key == selectedKey,
                    onClick = { onDestinationSelected(destination) },
                    // The visible label already names the destination for screen readers.
                    icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                    label = { Text(text = destination.label) },
                    alwaysShowLabel = true
                )
            }
        },
        modifier = modifier,
        layoutType = layoutType,
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}
