package com.oriteam.ori.ui.app.screens.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.auth.AuthFormScaffold
import com.oriteam.ori.ui.components.form.BrandHeader
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.form.PasswordTextField
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.preview.OriPreview
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.OriTheme

/** Stateful entry point: connects [LoginViewModel] to the stateless [LoginScreen]. */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginViewModel.Event.LoginSuccess -> onLoginSuccess()
                LoginViewModel.Event.NavigateToRegister -> onNavigateToRegister()
            }
        }
    }

    LoginScreen(
        state = state,
        onEmailInputChange = viewModel::onEmailInputChange,
        onPasswordInputChange = viewModel::onPasswordInputChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onLoginClick = viewModel::onLoginClick,
        onForgotPasswordClick = viewModel::onForgotPasswordClick,
        onNavigateToRegisterClick = viewModel::onNavigateToRegisterClick
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState = LoginUiState(),
    onEmailInputChange: (String) -> Unit = {},
    onPasswordInputChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToRegisterClick: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    AuthFormScaffold {
        BrandHeader()
        Spacer(modifier = Modifier.height(OriSpacing.Large))
        LoginGreeting()
        Spacer(modifier = Modifier.height(OriSpacing.Large))

        WelcomeCard()
        Spacer(modifier = Modifier.height(OriSpacing.Large))

        LabeledTextField(
            label = stringResource(R.string.email_label),
            value = state.emailInputValue,
            onValueChange = onEmailInputChange,
            placeholder = stringResource(R.string.email_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        PasswordTextField(
            label = stringResource(R.string.password_label),
            value = state.passwordInputValue,
            onValueChange = onPasswordInputChange,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLoginClick()
                }
            )
        )

        FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
        FormMessage(messageRes = state.successMessageRes, type = FormMessageType.SUCCESS)

        Spacer(modifier = Modifier.height(OriSpacing.Large))

        LoadingButton(
            text = stringResource(R.string.login_submit),
            isLoading = state.isLoading,
            onClick = onLoginClick
        )

        Spacer(modifier = Modifier.height(OriSpacing.Small))

        LoginFooter(
            onForgotPasswordClick = onForgotPasswordClick,
            onNavigateToRegisterClick = onNavigateToRegisterClick
        )
    }
}

@Composable
private fun LoginFooter(
    onForgotPasswordClick: () -> Unit,
    onNavigateToRegisterClick: () -> Unit
) {
    OriTextButton(text = stringResource(R.string.login_forgot_password), onClick = onForgotPasswordClick)

    Spacer(modifier = Modifier.height(OriSpacing.Medium))

    Text(
        text = stringResource(R.string.login_no_account_question),
        style = MaterialTheme.typography.bodySmall,
        color = LocalOriColors.current.subtleText,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(OriSpacing.Small))

    OriSecondaryButton(text = stringResource(R.string.login_create_account), onClick = onNavigateToRegisterClick)
}

@OriPreview
@Composable
private fun LoginScreenPreview() {
    OriTheme {
        LoginScreen()
    }
}
