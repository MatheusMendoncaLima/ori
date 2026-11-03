package com.oriteam.ori.ui.components.form

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriDimensions

/** Primary form button. While [isLoading] it is disabled and shows a progress indicator. */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OriDimensions.ControlMinHeight)
    ) {
        Crossfade(
            targetState = isLoading,
            animationSpec = LocalOriAppearance.current.motion.feedback(200),
            label = "LoadingButtonContent"
        ) { showProgress ->
            if (showProgress) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}
