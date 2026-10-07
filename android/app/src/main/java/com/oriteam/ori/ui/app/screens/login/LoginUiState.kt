package com.oriteam.ori.ui.app.screens.login
data class LoginUiState(
    val emailInputValue: String = "",
    val passwordInputValue: String = "",
    val isPasswordVisible: Boolean = false,
    val isUppercase: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
