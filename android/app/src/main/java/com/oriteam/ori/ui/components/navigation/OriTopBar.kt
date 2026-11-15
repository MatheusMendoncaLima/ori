package com.oriteam.ori.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.TextCaseToggle
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** The back arrow is drawn this far inside its 48 dp button. */
private val BackIconInset = 12.dp

/** The height of every title row, so a pane without a title lines up with one beside it. */
private val TopBarMinHeight = 64.dp

/**
 * Whether the top bars below show "Aa". A list shown beside its detail turns it off: the detail's bar, at the
 * right edge of the screen, has it, and two "Aa" side by side looked like two different choices.
 */
val LocalTopBarShowsTextCase = staticCompositionLocalOf { true }

/**
 * Title row of a screen or pane. Shows a back button only when [onNavigateBack] is given,
 * so a detail pane shown beside its list does not offer a misleading "back". The "Aa" button
 * is part of every top bar, so the choice can be changed from anywhere.
 *
 * The title, or the back arrow, starts at the same margin as the content below, and "Aa" ends there. On a wide
 * window the bar takes the same [maxWidth] and center as that content (`CenteredContent`), so the title does not
 * sit far to the left of the cards it names.
 */
@Composable
fun OriTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    maxWidth: Dp = OriDimensions.ContentMaxWidth,
    actions: @Composable () -> Unit = {}
) {
    val margin = LocalWindowLayout.current.horizontalPadding
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .heightIn(min = TopBarMinHeight)
                .padding(start = if (onNavigateBack != null) margin - BackIconInset else 0.dp, end = margin),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onNavigateBack != null) OriSpacing.Small else margin, end = OriSpacing.Small)
                    .semantics { heading() }
            )
            actions()
            if (LocalTopBarShowsTextCase.current) TextCaseToggle()
        }
    }
}

/** A title row with only "Aa", for a pane that has no title (the empty detail beside a list). */
@Composable
fun OriTopBarWithoutTitle(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TopBarMinHeight)
            .padding(end = LocalWindowLayout.current.horizontalPadding)
    ) {
        TextCaseToggle()
    }
}
