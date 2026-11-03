package com.oriteam.ori.ui.components.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** A top-level area of the app, such as Rotina, Comunicação or Configurações. */
@Immutable
data class NavigationDestination(
    val key: String,
    val label: String,
    val icon: ImageVector
)
