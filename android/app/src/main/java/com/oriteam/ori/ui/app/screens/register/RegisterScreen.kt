package com.oriteam.ori.ui.app.screens.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.OriPreview
import com.oriteam.ori.R
import com.oriteam.ori.ui.theme.NunitoFontFamily
import com.oriteam.ori.ui.theme.NunitoSansFontFamily
import com.oriteam.ori.ui.theme.OriBackground
import com.oriteam.ori.ui.theme.OriButtonCyan
import com.oriteam.ori.ui.theme.OriCardBorder
import com.oriteam.ori.ui.theme.OriDarkBlue
import com.oriteam.ori.ui.theme.OriFieldPlaceholder
import com.oriteam.ori.ui.theme.OriSloganBlue

@Composable
fun RegisterRoute(
    isResponsavelMode: Boolean = false,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isResponsavelMode) {
        viewModel.setResponsavelMode(isResponsavelMode)
    }

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
        onToggleResponsavel = viewModel::onToggleResponsavel,
        onToggleUppercase = viewModel::onToggleUppercase,
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
    onToggleResponsavel: (Boolean) -> Unit = {},
    onToggleUppercase: () -> Unit = {},
    onNavigateToLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    val isUpper = state.isUppercase
    val isResponsavel = state.isResponsavel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OriBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Cabeçalho Superior: "ori" + botão "Aa"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ori".toCased(isUpper),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = OriDarkBlue
                )

                val buttonBgColor by animateColorAsState(
                    targetValue = if (isUpper) OriDarkBlue else Color.White,
                    animationSpec = tween(durationMillis = 150),
                    label = "AaBgColor"
                )
                val buttonTextColor by animateColorAsState(
                    targetValue = if (isUpper) Color.White else OriDarkBlue,
                    animationSpec = tween(durationMillis = 150),
                    label = "AaTextColor"
                )

                Surface(
                    shape = CircleShape,
                    color = buttonBgColor,
                    border = BorderStroke(1.dp, if (isUpper) OriDarkBlue else Color(0xFFE0ECF5)),
                    modifier = Modifier
                        .size(width = 46.dp, height = 34.dp)
                        .clip(CircleShape)
                        .clickable { onToggleUppercase() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Aa",
                            fontFamily = NunitoSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = buttonTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Título e Subtítulo dinâmicos
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                val titleText = if (isResponsavel) "Cadastro do responsável" else "Vamos começar"
                Text(
                    text = titleText.toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isResponsavel) 26.sp else 32.sp,
                    color = OriDarkBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Preencha seus dados para criar sua conta".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = OriSloganBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Campo: Seu Nome
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Seu nome".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OriDarkBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CustomInputField(
                    value = state.nameInputValue,
                    onValueChange = onNameInputChange,
                    placeholder = "Digite seu nome completo".toCased(isUpper),
                    isUppercase = isUpper,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: E-mail
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "E-mail".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OriDarkBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CustomInputField(
                    value = state.emailInputValue,
                    onValueChange = onEmailInputChange,
                    placeholder = "seu@email.com".toCased(isUpper),
                    isUppercase = isUpper,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Senha
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Senha".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OriDarkBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CustomInputField(
                    value = state.passwordInputValue,
                    onValueChange = onPasswordInputChange,
                    placeholder = "••••••••",
                    isUppercase = isUpper,
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = onTogglePasswordVisibility,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Crossfade(
                                targetState = state.isPasswordVisible,
                                animationSpec = tween(durationMillis = 200),
                                label = "PasswordToggle"
                            ) { isVisible ->
                                Image(
                                    painter = painterResource(
                                        id = if (isVisible) R.drawable.olho_aberto else R.drawable.olho_fechado
                                    ),
                                    contentDescription = if (isVisible) "Ocultar senha" else "Exibir senha",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Confirmar Senha
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Confirmar senha".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OriDarkBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                CustomInputField(
                    value = state.confirmPasswordInputValue,
                    onValueChange = onConfirmPasswordInputChange,
                    placeholder = "••••••••",
                    isUppercase = isUpper,
                    visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onRegisterClick()
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = onToggleConfirmPasswordVisibility,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Crossfade(
                                targetState = state.isConfirmPasswordVisible,
                                animationSpec = tween(durationMillis = 200),
                                label = "ConfirmPasswordToggle"
                            ) { isVisible ->
                                Image(
                                    painter = painterResource(
                                        id = if (isVisible) R.drawable.olho_aberto else R.drawable.olho_fechado
                                    ),
                                    contentDescription = if (isVisible) "Ocultar senha" else "Exibir senha",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch: Sou responsável
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sou responsável".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OriDarkBlue
                )

                Switch(
                    checked = isResponsavel,
                    onCheckedChange = onToggleResponsavel,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OriButtonCyan,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD0E3EF)
                    )
                )
            }

            // Mensagem de Erro
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.errorMessage?.let { error ->
                    Text(
                        text = error.toCased(isUpper),
                        fontFamily = NunitoSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão "Cadastrar"
            Button(
                onClick = onRegisterClick,
                enabled = !state.isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OriButtonCyan,
                    disabledContainerColor = OriButtonCyan.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Crossfade(
                    targetState = state.isLoading,
                    animationSpec = tween(durationMillis = 200),
                    label = "RegisterLoading"
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Cadastrar".toCased(isUpper),
                            fontFamily = NunitoSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link: "Já tem uma conta? Entrar"
            TextButton(
                onClick = onNavigateToLoginClick
            ) {
                Text(
                    text = "Já tem uma conta? Entrar".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OriDarkBlue
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun String.toCased(isUppercase: Boolean): String {
    return if (isUppercase) this.uppercase() else this
}

@Composable
private fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isUppercase: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) OriButtonCyan else OriCardBorder,
        animationSpec = tween(durationMillis = 150),
        label = "FieldBorderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = NunitoSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = OriFieldPlaceholder
                    )
                }

                val displayValue = if (isUppercase) value.uppercase() else value

                BasicTextField(
                    value = displayValue,
                    onValueChange = { newValue ->
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontFamily = NunitoSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = OriDarkBlue
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(OriButtonCyan),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }

            trailingIcon?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}

@OriPreview
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}
