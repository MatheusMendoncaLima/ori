package com.oriteam.ori.ui.app.screens.legal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.components.state.ErrorState
import com.oriteam.ori.ui.components.state.LoadingState
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Shows [content] only after the consent of [ownerUid] is given (J2, J3). Before it, the consent screen; declining
 * leaves ([onDecline]: signs out, or goes back to the welcome without an account).
 */
@Composable
fun ConsentGate(ownerUid: String, onDecline: () -> Unit, content: @Composable () -> Unit) {
    val viewModel: ConsentViewModel = hiltViewModel(key = "consent-$ownerUid")
    LaunchedEffect(ownerUid) { viewModel.start(ownerUid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (state.status) {
        ConsentStatus.ACCEPTED -> content()
        ConsentStatus.CHECKING -> LoadingState(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
        ConsentStatus.UNAVAILABLE -> Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ErrorState(
                    message = stringResource(state.errorMessageRes ?: R.string.error_unknown),
                    onRetry = viewModel::onRetry
                )
            }
            OriTextButton(text = stringResource(R.string.consent_decline), onClick = onDecline)
        }
        ConsentStatus.REQUIRED -> ConsentScreen(state, viewModel, onDecline)
    }
}

@Composable
internal fun ConsentScreen(state: ConsentUiState, viewModel: ConsentViewModel, onDecline: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        OriTopBar(title = stringResource(R.string.consent_title), maxWidth = OriDimensions.FormMaxWidth)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.consent_summary), style = MaterialTheme.typography.bodyMedium)
                }
                OriTextButton(
                    text = stringResource(R.string.consent_read_privacy),
                    onClick = { uriHandler.openUri(LegalLinks.privacyPolicy()) }
                )
                OriTextButton(
                    text = stringResource(R.string.consent_read_terms),
                    onClick = { uriHandler.openUri(LegalLinks.termsOfUse()) }
                )
                SettingsChoiceGroup(
                    title = stringResource(R.string.consent_age_title),
                    choices = listOf(
                        SettingsChoice(true, stringResource(R.string.consent_age_adult)),
                        SettingsChoice(false, stringResource(R.string.consent_age_minor))
                    ),
                    selectedValue = state.isAdult,
                    onValueSelected = { viewModel.onAgeChange(it == true) }
                )
                if (state.isAdult == false) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.consent_guardian),
                        description = stringResource(R.string.consent_guardian_description),
                        checked = state.hasGuardianConsent,
                        onCheckedChange = viewModel::onGuardianConsentChange
                    )
                }
                SectionHeader(text = stringResource(R.string.consent_agreement_title))
                SettingsSwitchItem(
                    title = stringResource(R.string.consent_terms),
                    checked = state.acceptsTerms,
                    onCheckedChange = viewModel::onTermsChange
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.consent_sensitive),
                    description = stringResource(R.string.consent_sensitive_description),
                    checked = state.acceptsSensitiveData,
                    onCheckedChange = viewModel::onSensitiveDataChange
                )
                FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                OriPrimaryButton(
                    text = stringResource(R.string.consent_accept),
                    onClick = viewModel::onAccept,
                    enabled = state.canAccept
                )
                OriTextButton(text = stringResource(R.string.consent_decline), onClick = onDecline)
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}
