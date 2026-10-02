package com.oriteam.ori.ui.app.screens.register

data class RegisterUiState(
    val nameInputValue: String = "",
    val emailInputValue: String = "",
    val passwordInputValue: String = "",
    val confirmPasswordInputValue: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)