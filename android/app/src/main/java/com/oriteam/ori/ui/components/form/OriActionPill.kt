package com.oriteam.ori.ui.components.form

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/** The floating "+ Nova tarefa" button of the wireframes: the main action of a list. */
@Composable
fun OriActionPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOriColors.current
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.strong, contentColor = colors.onStrong),
        modifier = modifier.heightIn(min = OriDimensions.ControlMinHeight)
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.size(OriSpacing.Small))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
