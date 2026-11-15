package com.oriteam.ori.ui.app.screens.legal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private const val EXPORT_FILE_NAME = "ori-my-data.json"

/**
 * Where the data is, the documents, a copy of everything and, last and in red, erasing it (J1, J5). Withdrawing the
 * consent is erasing: the text says so, so nobody looks for a separate switch.
 */
@Composable
fun PrivacyScreen(
    ownerUid: String,
    isLocal: Boolean,
    onEraseLocalData: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var isConfirmingErase by rememberSaveable { mutableStateOf(false) }
    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        viewModel.onExportTo(ownerUid, uri?.toString())
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(
            maxWidth = OriDimensions.FormMaxWidth,
            title = stringResource(R.string.settings_privacy),
            onNavigateBack = onNavigateBack
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                WhereTheDataIs(isLocal)
                OriTextButton(
                    text = stringResource(R.string.consent_read_privacy),
                    onClick = { uriHandler.openUri(LegalLinks.privacyPolicy()) }
                )
                OriTextButton(
                    text = stringResource(R.string.consent_read_terms),
                    onClick = { uriHandler.openUri(LegalLinks.termsOfUse()) }
                )
                Spacer(modifier = Modifier.height(OriSpacing.Small))
                LoadingButton(
                    text = stringResource(R.string.privacy_export),
                    isLoading = state.isExporting,
                    onClick = { saveAs.launch(EXPORT_FILE_NAME) }
                )
                FormMessage(messageRes = state.successMessageRes, type = FormMessageType.SUCCESS)
                FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                Text(
                    text = stringResource(R.string.privacy_withdraw),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OriTextButton(
                    text = stringResource(if (isLocal) R.string.privacy_erase else R.string.privacy_delete_account),
                    onClick = { if (isLocal) isConfirmingErase = true else viewModel.onDeleteRequest() },
                    color = LocalOriColors.current.errorText,
                    enabled = !state.isDeleting
                )
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
    if (isConfirmingErase) {
        OriConfirmationDialog(
            title = stringResource(R.string.privacy_erase_title),
            message = stringResource(R.string.privacy_erase_message),
            // The same verb as the button that asked: "Apagar", not the "Excluir" of the boards.
            confirmLabel = stringResource(R.string.privacy_erase_confirm),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onEraseLocalData,
            onDismiss = { isConfirmingErase = false },
            isDestructive = true
        )
    }
    if (state.isConfirmingDeletion) {
        OriConfirmationDialog(
            title = stringResource(R.string.privacy_delete_title),
            message = stringResource(R.string.privacy_delete_message),
            confirmLabel = stringResource(R.string.privacy_erase_confirm),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = { viewModel.onDeleteConfirm(ownerUid) },
            onDismiss = viewModel::onDeleteDismiss,
            isDestructive = true
        )
    }
}

@Composable
private fun WhereTheDataIs(isLocal: Boolean) {
    OriCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.privacy_where_title), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        Text(
            text = stringResource(if (isLocal) R.string.privacy_where_local else R.string.privacy_where_account),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(OriSpacing.Small))
        Text(text = stringResource(R.string.privacy_device_only), style = MaterialTheme.typography.bodyMedium)
        if (!isLocal) {
            Spacer(modifier = Modifier.height(OriSpacing.Small))
            Text(text = stringResource(R.string.privacy_caregivers), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
