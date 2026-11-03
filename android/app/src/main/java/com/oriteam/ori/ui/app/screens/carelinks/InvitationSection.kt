package com.oriteam.ori.ui.app.screens.carelinks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LabeledTextField
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriSpacing
import java.text.DateFormat
import java.util.Date

/** Caregivers create a code to share; primary users type the code they received. */
@Composable
fun InvitationSection(
    side: CareLinkSide,
    state: InvitationUiState,
    onCreateInvitationClick: () -> Unit,
    onCodeInputChange: (String) -> Unit,
    onAcceptInvitationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OriCard(modifier = modifier.fillMaxWidth()) {
        when (side) {
            CareLinkSide.CAREGIVER -> CaregiverInvitation(state, onCreateInvitationClick)
            CareLinkSide.PRIMARY_USER -> PrimaryUserInvitation(state, onCodeInputChange, onAcceptInvitationClick)
        }
        FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
    }
}

@Composable
private fun CaregiverInvitation(state: InvitationUiState, onCreateInvitationClick: () -> Unit) {
    SectionText(
        title = stringResource(R.string.invitation_caregiver_title),
        body = stringResource(R.string.invitation_caregiver_body)
    )
    state.createdInvitation?.let { invitation ->
        val expiresAt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(invitation.expiresAtMillis))
        Text(
            text = invitation.code,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        Text(
            text = stringResource(R.string.invitation_expires_at, expiresAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(OriSpacing.Medium))
    }
    LoadingButton(
        text = stringResource(R.string.invitation_create),
        isLoading = state.isLoading,
        onClick = onCreateInvitationClick
    )
}

@Composable
private fun PrimaryUserInvitation(
    state: InvitationUiState,
    onCodeInputChange: (String) -> Unit,
    onAcceptInvitationClick: () -> Unit
) {
    SectionText(
        title = stringResource(R.string.invitation_primary_title),
        body = stringResource(R.string.invitation_primary_body)
    )
    LabeledTextField(
        label = stringResource(R.string.invitation_code_label),
        value = state.codeInputValue,
        onValueChange = onCodeInputChange,
        placeholder = stringResource(R.string.invitation_code_placeholder),
        alwaysUppercase = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onAcceptInvitationClick() })
    )
    state.linkedCaregiverName?.let { name ->
        Text(
            text = stringResource(R.string.invitation_linked, name),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    LoadingButton(
        text = stringResource(R.string.invitation_accept),
        isLoading = state.isLoading,
        onClick = onAcceptInvitationClick
    )
}

@Composable
private fun SectionText(title: String, body: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
}
