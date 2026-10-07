package com.oriteam.ori.ui.app.screens.login

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.oriteam.ori.ui.theme.OriIndicatorInactive
import com.oriteam.ori.ui.theme.OriSloganBlue
import com.oriteam.ori.ui.theme.OriSubtleText

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
        onToggleUppercase = viewModel::onToggleUppercase,
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
    onToggleUppercase: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onNavigateToRegisterClick: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    val isUpper = state.isUppercase

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

            // Cabeçalho Superior: "ori" na esquerda e botão "Aa" interativo na direita
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

            // Seção Saudação: "Olá!" + "Obrigado por voltar ao ori!"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Olá!".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = OriDarkBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitleText = buildAnnotatedString {
                    append("Obrigado por voltar ao ".toCased(isUpper))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = OriDarkBlue)) {
                        append("ori".toCased(isUpper))
                    }
                    append("!".toCased(isUpper))
                }

                Text(
                    text = subtitleText,
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = OriSloganBlue
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Balão de Boas-Vindas com ícone de Mão Acenando
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, OriCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mao_acenando),
                        contentDescription = "Mão acenando",
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Bem-vindo(a)!".toCased(isUpper),
                            fontFamily = NunitoSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = OriDarkBlue
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Acesse sua conta para continuar".toCased(isUpper),
                            fontFamily = NunitoSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = OriSloganBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Entrada: E-mail
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

            // Campo de Entrada: Senha com Toggle de Visibilidade Olho Aberto/Fechado
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
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }
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

            // Mensagens de Erro/Sucesso com Animação Suave
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

            AnimatedVisibility(
                visible = state.successMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.successMessage?.let { success ->
                    Text(
                        text = success.toCased(isUpper),
                        fontFamily = NunitoSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão Principal: "Entrar"
            Button(
                onClick = onLoginClick,
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
                    label = "LoginLoading"
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Entrar".toCased(isUpper),
                            fontFamily = NunitoSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Link: "Esqueci minha senha"
            TextButton(
                onClick = onForgotPasswordClick
            ) {
                Text(
                    text = "Esqueci minha senha".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = OriDarkBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seção de Registro: "Ainda não tem conta?" + Botão "Criar uma conta"
            Text(
                text = "Ainda não tem conta?".toCased(isUpper),
                fontFamily = NunitoSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = OriSubtleText
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onNavigateToRegisterClick,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, OriDarkBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OriDarkBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Criar uma conta".toCased(isUpper),
                    fontFamily = NunitoSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OriDarkBlue
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Função utilitária para converter texto para caixa alta se isUppercase for true.
 */
private fun String.toCased(isUppercase: Boolean): String {
    return if (isUppercase) this.uppercase() else this
}

/**
 * Campo de texto customizado com design arredondado limpo e transição animada de foco.
 */
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
fun LoginScreenPreview() {
    LoginScreen()
}
