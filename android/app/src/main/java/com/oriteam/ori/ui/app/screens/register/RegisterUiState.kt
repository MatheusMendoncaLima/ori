package com.oriteam.ori.ui.app.screens.register

data class RegisterUiState(
    val nameInputValue: String = "",
    val emailInputValue: String = "",
    val passwordInputValue: String = "",
    val confirmPasswordInputValue: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isResponsavel: Boolean = false,
    val isUppercase: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
