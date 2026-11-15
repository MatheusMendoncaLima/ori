package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * A value chosen along a range: its name, the value in words, and what each end means ("Mais grave",
 * "Mais aguda"), so nobody has to guess the direction. Screen readers hear the name and the value and can
 * step it up or down. [onValueChangeFinished] runs when the finger lifts, the moment to save or to listen.
 */
@Composable
fun OriLabeledSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(vertical = OriSpacing.Small)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = valueText
            }
        )
        Row {
            Text(text = startLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = endLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
