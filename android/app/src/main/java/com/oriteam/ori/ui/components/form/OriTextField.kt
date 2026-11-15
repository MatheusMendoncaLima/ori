package com.oriteam.ori.ui.components.form

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val FieldShape = RoundedCornerShape(14.dp)
private const val MULTI_LINE_MIN_LINES = 3

/** Text field with a label above it, following the ORI form style. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    /** False for longer texts, such as descriptions: the field grows line by line. */
    singleLine: Boolean = true,
    alwaysUppercase: Boolean = false
) {
    FieldWithLabel(label = label, modifier = modifier) {
        OriInputBox(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            visualTransformation = if (alwaysUppercase || LocalOriAppearance.current.isUppercase) {
                UppercaseVisualTransformation
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine
        )
    }
}

/** Password field with a show/hide button. The typed value is never changed by the "Aa" preference. */
@Composable
fun PasswordTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier
) {
    val visualTransformation = when {
        !isPasswordVisible -> PasswordVisualTransformation()
        LocalOriAppearance.current.isUppercase -> UppercaseVisualTransformation
        else -> VisualTransformation.None
    }

    FieldWithLabel(label = label, modifier = modifier) {
        OriInputBox(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.password_placeholder),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
            keyboardActions = keyboardActions,
            trailingIcon = {
                PasswordVisibilityButton(isPasswordVisible = isPasswordVisible, onClick = onTogglePasswordVisibility)
            }
        )
    }
}

@Composable
private fun FieldWithLabel(label: String, modifier: Modifier, field: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = OriSpacing.ExtraSmall)
        )
        field()
    }
}

@Composable
private fun PasswordVisibilityButton(isPasswordVisible: Boolean, onClick: () -> Unit) {
    // 48 dp to touch and for screen readers (A1): a forced 36 dp was too small, and the default (40 dp) only grows
    // to 48 for touch, not in the bounds that TalkBack and the Accessibility Scanner see.
    IconButton(onClick = onClick, modifier = Modifier.size(OriDimensions.MinTouchTarget)) {
        Crossfade(
            targetState = isPasswordVisible,
            animationSpec = LocalOriAppearance.current.motion.decorative(200),
            label = "PasswordVisibility"
        ) { isVisible ->
            Image(
                painter = painterResource(if (isVisible) R.drawable.olho_aberto else R.drawable.olho_fechado),
                contentDescription = stringResource(if (isVisible) R.string.hide_password else R.string.show_password),
                // The drawing has a fixed color: the tint keeps it visible on the dark theme too.
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Rounded input with an animated focus border. Shared by every form field. */
@Composable
private fun OriInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        // The edge of a control (3:1), not the light line of a card: on the light theme a white field on the light
        // background is found only by it.
        targetValue = if (isFocused) colors.primary else colors.outline,
        animationSpec = LocalOriAppearance.current.motion.feedback(150),
        label = "FieldBorderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Minimum, not fixed: the field grows with enlarged text instead of clipping it.
            .heightIn(min = OriDimensions.ControlMinHeight)
            .clip(FieldShape)
            .background(colors.surface)
            .border(width = if (isFocused) 1.5.dp else 1.dp, color = borderColor, shape = FieldShape)
            .padding(horizontal = OriSpacing.Medium, vertical = OriSpacing.Small),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                        color = LocalOriColors.current.fieldPlaceholder
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else MULTI_LINE_MIN_LINES,
                    cursorBrush = SolidColor(colors.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(OriSpacing.Small))
                it()
            }
        }
    }
}
