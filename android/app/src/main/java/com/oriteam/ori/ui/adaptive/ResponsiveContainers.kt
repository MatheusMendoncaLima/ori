package com.oriteam.ori.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.ui.theme.OriDimensions

/**
 * Centers content and limits its width, so wide windows show a readable column instead of
 * stretched fields. Side padding follows [WindowLayout.horizontalPadding].
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = OriDimensions.FormMaxWidth,
    content: @Composable ColumnScope.() -> Unit
) {
    val horizontalPadding = LocalWindowLayout.current.horizontalPadding
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * Scrollable column that fills at least the whole viewport.
 *
 * Content can be spread with `Arrangement.SpaceBetween` when it fits; when it does not
 * (enlarged text, short landscape window, open keyboard) it scrolls instead of being clipped.
 */
@Composable
fun FillOrScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Shows [supportingPane] beside the main content when [WindowLayout.showsSupportingPane];
 * otherwise only [content] is shown. Used by forms so tablets and landscape phones do not
 * receive a stretched phone layout.
 */
@Composable
fun SupportingPaneLayout(
    supportingPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (LocalWindowLayout.current.showsSupportingPane) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) { supportingPane() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { content() }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) { content() }
    }
}
