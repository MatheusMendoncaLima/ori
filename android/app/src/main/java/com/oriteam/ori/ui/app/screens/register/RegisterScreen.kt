package com.oriteam.ori.ui.app.screens.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oriteam.ori.OriPreview
import com.oriteam.ori.ui.app.screens.components.UiPaddingBox
import com.oriteam.ori.ui.app.screens.home.HomeUiState
import com.oriteam.ori.ui.app.screens.home.HomeViewModel

@Composable
fun RegisterRoute(
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterViewModel.Event.RegisterSuccess -> onRegisterSuccess()
            }
        }
    }

    RegisterScreen(
        state = state,
        onNameInputChange = viewModel::onNameInputChange,
        onEmailInputChange = viewModel::onEmailInputChange,
        onPasswordInputChange = viewModel::onPasswordInputChange,
        onConfirmPasswordInputChange = viewModel::onConfirmPasswordInputChange,
        onRegisterClick = viewModel::onRegisterClick
    )
}

@OriPreview
@Composable
fun RegisterScreen(state : RegisterUiState = RegisterUiState(),
       onNameInputChange: (String)-> Unit = {},
       onEmailInputChange: (String)-> Unit = {},
       onPasswordInputChange: (String)-> Unit = {},
       onConfirmPasswordInputChange: (String)-> Unit = {},
                   focusManager: FocusManager = LocalFocusManager.current,
                   onRegisterClick : ()->Unit = {}){
    UiPaddingBox {
        Column {
        TextField(modifier = Modifier.fillMaxWidth(), value= state.nameInputValue, onValueChange = onNameInputChange, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions (
                onNext = { focusManager.moveFocus(FocusDirection.Down) } // pula pro próximo campo
            ))
        TextField(modifier = Modifier.fillMaxWidth(), value= state.emailInputValue, onValueChange = onEmailInputChange, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions (
                onNext = { focusManager.moveFocus(FocusDirection.Down) } // pula pro próximo campo
            ))
        TextField(modifier = Modifier.fillMaxWidth(), value= state.passwordInputValue, onValueChange = onPasswordInputChange, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions (
                onNext = { focusManager.moveFocus(FocusDirection.Down) } // pula pro próximo campo
            ))
        TextField(modifier = Modifier.fillMaxWidth(), value= state.confirmPasswordInputValue, onValueChange = onConfirmPasswordInputChange, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions (
                onNext = { focusManager.clearFocus() } // pula pro próximo campo
            )
            )
            Button(onClick = onRegisterClick) { }
        }
    }
}