package com.oriteam.ori.ui.app.screens.carelinks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.ui.components.dialog.OriConfirmationDialog
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.form.OriTransientNotice
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.components.state.ContentState

/** Name of the other person in the link, from the viewer's point of view. */
fun CareLink.otherPersonName(viewerSide: CareLinkSide): String = when (viewerSide) {
    CareLinkSide.PRIMARY_USER -> caregiverName
    CareLinkSide.CAREGIVER -> primaryUserName
}

/**
 * Linked people with "end link" and, for the primary user, what the caregiver may manage
 * (routine and communication boards).
 * Revocation always asks for confirmation because it immediately removes access.
 */
@Composable
fun CareLinksSection(
    state: CareLinksUiState,
    onRevokeRequest: (CareLink) -> Unit,
    onRevokeConfirm: () -> Unit,
    onRevokeDismiss: () -> Unit,
    onManagePermissionChange: (CareLink, Boolean) -> Unit,
    onCommunicationPermissionChange: (CareLink, Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(text = stringResource(R.string.links_title))
        FormMessage(messageRes = state.actionErrorRes, type = FormMessageType.ERROR)
        OriTransientNotice(stringResource(R.string.sync_pending_changes).takeIf { state.hasPendingChange })
        when (val links = state.links) {
            ContentState.Loading -> OriNotice(stringResource(R.string.state_loading))
            ContentState.Empty -> OriNotice(stringResource(R.string.links_empty))
            is ContentState.Error -> {
                OriNotice(stringResource(links.messageRes), isLive = true)
                OriTextButton(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
            is ContentState.Content -> links.value.forEach { link ->
                LinkRow(
                    link = link,
                    viewerSide = state.viewerSide,
                    onRevokeRequest = onRevokeRequest,
                    onManagePermissionChange = onManagePermissionChange,
                    onCommunicationPermissionChange = onCommunicationPermissionChange
                )
            }
        }
    }

    state.pendingRevocation?.let { link ->
        val name = link.otherPersonName(state.viewerSide)
        OriConfirmationDialog(
            title = stringResource(R.string.links_revoke_title, name),
            message = stringResource(R.string.links_revoke_message),
            confirmLabel = stringResource(R.string.links_revoke_confirm),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onRevokeConfirm,
            onDismiss = onRevokeDismiss,
            isDestructive = true
        )
    }
}

@Composable
private fun LinkRow(
    link: CareLink,
    viewerSide: CareLinkSide,
    onRevokeRequest: (CareLink) -> Unit,
    onManagePermissionChange: (CareLink, Boolean) -> Unit,
    onCommunicationPermissionChange: (CareLink, Boolean) -> Unit
) {
    val name = link.otherPersonName(viewerSide)
    OriListItem(
        headline = name,
        supportingText = stringResource(
            if (link.permissions.manageRoutines) R.string.links_can_manage else R.string.links_view_only
        ),
        trailingContent = {
            OriTextButton(
                text = stringResource(R.string.links_revoke),
                onClick = { onRevokeRequest(link) },
                color = MaterialTheme.colorScheme.error,
                isFullWidth = false
            )
        }
    )
    if (viewerSide == CareLinkSide.PRIMARY_USER) {
        SettingsSwitchItem(
            title = stringResource(R.string.links_allow_manage, name),
            description = stringResource(R.string.links_allow_manage_description),
            checked = link.permissions.manageRoutines,
            onCheckedChange = { onManagePermissionChange(link, it) }
        )
        SettingsSwitchItem(
            title = stringResource(R.string.links_allow_communication, name),
            description = stringResource(R.string.links_allow_communication_description),
            checked = link.permissions.manageCommunication,
            onCheckedChange = { onCommunicationPermissionChange(link, it) }
        )
    }
}
