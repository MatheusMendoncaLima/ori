package com.oriteam.ori.ui.components.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.oriteam.ori.ui.theme.LocalOriColors

/** Kind of feedback shown by [FormMessage]; each kind has its own text color. */
enum class FormMessageType {
    ERROR,
    SUCCESS;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            ERROR -> LocalOriColors.current.errorText
            SUCCESS -> LocalOriColors.current.successText
        }
}
