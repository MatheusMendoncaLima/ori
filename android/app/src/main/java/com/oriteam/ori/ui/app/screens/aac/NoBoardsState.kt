package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * First visit, no boards yet. Nobody is forced into one model: the person may use a suggested
 * board, build their own, or just type and speak with the message bar above.
 */
@Composable
fun NoBoardsState(
    isCreating: Boolean,
    onCreateStarterBoard: () -> Unit,
    onCreateOwn: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Centered and as wide as a form, like every other empty state, instead of buttons across a tablet.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = OriDimensions.FormMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OriSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.aac_no_boards_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.aac_no_boards_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            LoadingButton(
                text = stringResource(R.string.aac_use_starter_board),
                isLoading = isCreating,
                onClick = onCreateStarterBoard
            )
            OriSecondaryButton(text = stringResource(R.string.aac_create_own_board), onClick = onCreateOwn)
        }
    }
}
