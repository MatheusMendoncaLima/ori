package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * The end of a long form, always in sight: "Cancelar" and the main action side by side, below the fields and above
 * the keyboard. Before, both came after the last field, and someone who filled in only the first one had to scroll
 * to find how to finish.
 */
@Composable
fun FormActionBar(
    saveLabel: String,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = OriDimensions.FormMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small)
            ) {
                OriTextButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                LoadingButton(text = saveLabel, isLoading = isSaving, onClick = onSave, modifier = Modifier.weight(1f))
            }
        }
    }
}
