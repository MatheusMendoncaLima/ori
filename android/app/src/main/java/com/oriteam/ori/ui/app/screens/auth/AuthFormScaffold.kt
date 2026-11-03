package com.oriteam.ori.ui.app.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.FillOrScrollColumn
import com.oriteam.ori.ui.adaptive.SupportingPaneLayout
import com.oriteam.ori.ui.components.brand.OriBrandBlock
import com.oriteam.ori.ui.components.brand.OriDecorativeArcs
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Frame shared by the login and registration forms.
 *
 * - Respects system bars, display cutouts and the on-screen keyboard (`safeDrawing`), so the
 *   focused field is never hidden behind the keyboard.
 * - Scrolls when the content is taller than the window (large fonts, landscape).
 * - Limits the form width and, on wide or short-and-wide windows, shows the brand beside the
 *   form instead of stretching it (see `WindowLayout.showsSupportingPane`).
 */
@Composable
fun AuthFormScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SupportingPaneLayout(
            supportingPane = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OriDecorativeArcs()
                    OriBrandBlock(logoHeight = 140.dp)
                }
            }
        ) {
            FillOrScrollColumn(modifier = Modifier.fillMaxSize()) {
                CenteredContent {
                    Spacer(modifier = Modifier.height(OriSpacing.Medium))
                    content()
                    Spacer(modifier = Modifier.height(OriSpacing.Huge))
                }
            }
        }
    }
}
