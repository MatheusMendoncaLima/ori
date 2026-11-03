package com.oriteam.ori.ui.app.screens.register

import androidx.annotation.StringRes

data class RegisterUiState(
    val nameInputValue: String = "",
    val emailInputValue: String = "",
    val passwordInputValue: String = "",
    val confirmPasswordInputValue: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)
