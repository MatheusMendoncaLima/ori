package com.oriteam.ori.ui.components.text

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors

private const val TOGGLE_ANIMATION_MILLIS = 150

/**
 * What the "Aa" button does, provided by the root of the app. Without it (tests, previews) the
 * button does not appear, so no screen has to receive this action as a parameter.
 */
val LocalTextCaseToggle = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The "Aa" button shown at the top of the screens: switches all text to uppercase and back. */
@Composable
fun TextCaseToggle(modifier: Modifier = Modifier) {
    val onToggle = LocalTextCaseToggle.current ?: return
    val isUppercase = LocalOriAppearance.current.isUppercase
    val colors = MaterialTheme.colorScheme
    val motion = LocalOriAppearance.current.motion
    val backgroundColor by animateColorAsState(
        targetValue = if (isUppercase) colors.onBackground else colors.surface,
        animationSpec = motion.decorative(TOGGLE_ANIMATION_MILLIS),
        label = "TextCaseToggleBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (isUppercase) colors.surface else colors.onBackground,
        animationSpec = motion.decorative(TOGGLE_ANIMATION_MILLIS),
        label = "TextCaseToggleText"
    )
    val description = stringResource(R.string.text_case_toggle_description)

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, if (isUppercase) colors.onBackground else LocalOriColors.current.toggleBorder),
        modifier = modifier
            // Keeps the compact 46 x 34 dp look while the touch area reaches 48 x 48 dp.
            .minimumInteractiveComponentSize()
            .size(width = 46.dp, height = 34.dp)
            .clip(CircleShape)
            // toggleable exposes the on/off state to screen readers, unlike a plain click.
            .toggleable(value = isUppercase, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // The label itself shows the option, so it is not affected by the current case.
            androidx.compose.material3.Text(
                text = stringResource(R.string.text_case_toggle_label),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}
