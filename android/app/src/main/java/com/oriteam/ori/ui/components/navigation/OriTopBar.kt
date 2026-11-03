package com.oriteam.ori.ui.components.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.LocalWindowLayout
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.components.text.TextCaseToggle
import com.oriteam.ori.ui.theme.OriSpacing

/** The back arrow is drawn this far inside its 48 dp button. */
private val BackIconInset = 12.dp

/**
 * Title row of a screen or pane. Shows a back button only when [onNavigateBack] is given,
 * so a detail pane shown beside its list does not offer a misleading "back". The "Aa" button
 * is part of every top bar, so the choice can be changed from anywhere.
 *
 * The title, or the back arrow, starts at the same margin as the content below, and "Aa" ends there.
 */
@Composable
fun OriTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val margin = LocalWindowLayout.current.horizontalPadding
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
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
        TextCaseToggle()
    }
}
