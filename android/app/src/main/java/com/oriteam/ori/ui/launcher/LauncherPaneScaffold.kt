package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** Every pane of ORI Home has the same width on a tablet, centered, so they read the same. */
private val PaneMaxWidth = OriDimensions.ContentMaxWidth

/**
 * The frame of every pane of ORI Home (options, wallpaper, widgets): the plain theme background, the same top bar
 * with the way back, and the content centered with the same width and margins. [content] scrolls on its own.
 */
@Composable
internal fun LauncherPaneScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    PlainPane {
        Column(modifier = Modifier.fillMaxSize()) {
            OriTopBar(title = title, maxWidth = PaneMaxWidth, onNavigateBack = onBack)
            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = PaneMaxWidth)
                        .fillMaxSize()
                        .padding(horizontal = OriSpacing.Medium)
                ) { content() }
            }
        }
    }
}
