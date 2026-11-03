package com.oriteam.ori.ui.app.screens.login

import androidx.annotation.StringRes

data class LoginUiState(
    val emailInputValue: String = "",
    val passwordInputValue: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val successMessageRes: Int? = null
)
