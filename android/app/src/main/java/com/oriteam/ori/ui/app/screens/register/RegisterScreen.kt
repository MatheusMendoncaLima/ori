package com.oriteam.ori.ui.app.screens.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.ui.app.screens.auth.AuthFormScaffold
import com.oriteam.ori.ui.components.form.BrandHeader
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.form.PasswordTextField
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.preview.OriPreview
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.OriTheme

/** Stateful entry point: connects [RegisterViewModel] to the stateless [RegisterScreen]. */
@Composable
fun RegisterRoute(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterViewModel.Event.RegisterSuccess -> onRegisterSuccess()
                RegisterViewModel.Event.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    RegisterScreen(
        state = state,
        onNameInputChange = viewModel::onNameInputChange,
        onEmailInputChange = viewModel::onEmailInputChange,
        onPasswordInputChange = viewModel::onPasswordInputChange,
        onConfirmPasswordInputChange = viewModel::onConfirmPasswordInputChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::onToggleConfirmPasswordVisibility,
        onNavigateToLoginClick = viewModel::onNavigateToLoginClick,
        onRegisterClick = viewModel::onRegisterClick
    )
}

@Composable
fun RegisterScreen(
    state: RegisterUiState = RegisterUiState(),
    onNameInputChange: (String) -> Unit = {},
    onEmailInputChange: (String) -> Unit = {},
    onPasswordInputChange: (String) -> Unit = {},
    onConfirmPasswordInputChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onToggleConfirmPasswordVisibility: () -> Unit = {},
    onNavigateToLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    val moveToNextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    AuthFormScaffold {
        BrandHeader()
        Spacer(modifier = Modifier.height(OriSpacing.Large))
        RegisterGreeting()
        Spacer(modifier = Modifier.height(OriSpacing.Large))

        LabeledTextField(
            label = stringResource(R.string.register_name_label),
            value = state.nameInputValue,
            onValueChange = onNameInputChange,
            placeholder = stringResource(R.string.register_name_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = moveToNextField
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        LabeledTextField(
            label = stringResource(R.string.email_label),
            value = state.emailInputValue,
            onValueChange = onEmailInputChange,
            placeholder = stringResource(R.string.email_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = moveToNextField
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        PasswordTextField(
            label = stringResource(R.string.password_label),
            value = state.passwordInputValue,
            onValueChange = onPasswordInputChange,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            imeAction = ImeAction.Next,
            keyboardActions = moveToNextField
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        PasswordTextField(
            label = stringResource(R.string.register_confirm_password_label),
            value = state.confirmPasswordInputValue,
            onValueChange = onConfirmPasswordInputChange,
            isPasswordVisible = state.isConfirmPasswordVisible,
            onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onRegisterClick()
                }
            )
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
        Spacer(modifier = Modifier.height(OriSpacing.Large))

        LoadingButton(
            text = stringResource(R.string.register_submit),
            isLoading = state.isLoading,
            onClick = onRegisterClick
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))

        OriTextButton(text = stringResource(R.string.register_has_account), onClick = onNavigateToLoginClick)
    }
}

@Composable
private fun RegisterGreeting() {
    val typography = MaterialTheme.typography
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(R.string.register_title),
            style = typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(OriSpacing.ExtraSmall))
        Text(
            text = stringResource(R.string.register_subtitle),
            style = typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OriPreview
@Composable
private fun RegisterScreenPreview() {
    OriTheme {
        RegisterScreen()
    }
}
