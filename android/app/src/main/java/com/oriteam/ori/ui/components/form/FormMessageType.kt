package com.oriteam.ori.ui.components.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.oriteam.ori.ui.theme.LocalOriColors

/**
 * Kind of feedback shown by [FormMessage]; each kind has its own text color. [INFO] is a neutral word, neither a
 * failure nor a success (the home screen is locked): it is written in the usual text color, not in red.
 */
enum class FormMessageType {
    ERROR,
    SUCCESS,
    INFO;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            ERROR -> LocalOriColors.current.errorText
            SUCCESS -> LocalOriColors.current.successText
            INFO -> MaterialTheme.colorScheme.onSurface
        }
}
