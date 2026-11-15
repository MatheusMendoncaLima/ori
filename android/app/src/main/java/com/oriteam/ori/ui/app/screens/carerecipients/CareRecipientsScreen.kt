package com.oriteam.ori.ui.app.screens.carerecipients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.profile.UserProfile
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.adaptive.OriListDetailLayout
import com.oriteam.ori.ui.app.screens.carelinks.CareLinksViewModel
import com.oriteam.ori.ui.app.screens.carelinks.InvitationSection
import com.oriteam.ori.ui.app.screens.carelinks.InvitationViewModel
import com.oriteam.ori.ui.components.icon.OriIcons
import com.oriteam.ori.ui.components.list.OriListItem
import com.oriteam.ori.ui.components.list.OriOpenArrow
import com.oriteam.ori.ui.components.motion.itemAnimation
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.state.ContentState
import com.oriteam.ori.ui.components.state.EmptyState
import com.oriteam.ori.ui.components.state.StatefulContent
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * Caregiver area: the people they support (list) and, for the chosen person (detail), the
 * routine and the communication boards, read with the permissions of that specific link.
 */
@Composable
fun CareRecipientsRoute(
    profile: UserProfile,
    onNavigateBack: () -> Unit,
    viewModel: CareLinksViewModel = hiltViewModel(key = "care-recipients"),
    invitationViewModel: InvitationViewModel = hiltViewModel(key = "care-recipients-invitation")
) {
    LaunchedEffect(profile.uid) { viewModel.start(profile.uid, CareLinkSide.CAREGIVER) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val links = (state.links as? ContentState.Content)?.value.orEmpty()
    val invitationState by invitationViewModel.uiState.collectAsStateWithLifecycle()

    OriListDetailLayout<String>(
        listPane = { onLinkSelected ->
            Column(modifier = Modifier.fillMaxSize()) {
                OriTopBar(title = stringResource(R.string.main_care_recipients), onNavigateBack = onNavigateBack)
                CenteredContent {
                    InvitationSection(
                        side = CareLinkSide.CAREGIVER,
                        state = invitationState,
                        onCreateInvitationClick = invitationViewModel::onCreateInvitationClick,
                        onCodeInputChange = invitationViewModel::onCodeInputChange,
                        onAcceptInvitationClick = invitationViewModel::onAcceptInvitationClick
                    )
                }
                StatefulContent(
                    state = state.links,
                    onRetry = viewModel::onRetry,
                    emptyContent = {
                        EmptyState(
                            icon = OriIcons.People,
                            title = stringResource(R.string.care_recipients_empty_title),
                            message = stringResource(R.string.care_recipients_empty_message)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { activeLinks ->
                    // The same margin and width as the invitation above.
                    CenteredContent {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = OriSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(LocalOriAppearance.current.metrics.listSpacing)
                        ) {
                            items(activeLinks, key = { it.id }) { link ->
                                OriListItem(
                                    modifier = Modifier.itemAnimation(this, LocalOriAppearance.current.motion),
                                    headline = link.primaryUserName,
                                    supportingText = stringResource(
                                        if (link.permissions.manageRoutines) {
                                            R.string.links_can_manage
                                        } else {
                                            R.string.links_view_only
                                        }
                                    ),
                                    leadingIcon = Icons.Filled.Face,
                                    onClick = { onLinkSelected(link.id) },
                                    trailingContent = { OriOpenArrow() }
                                )
                            }
                        }
                    }
                }
            }
        },
        detailPane = { linkId, onNavigateBack ->
            val link = links.firstOrNull { it.id == linkId }
            if (link == null) {
                // The link was revoked while open: its data is no longer reachable.
                EmptyState(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.care_recipients_link_ended_title),
                    message = stringResource(R.string.care_recipients_link_ended_message)
                )
            } else {
                CareRecipientDetail(link = link, onNavigateBack = onNavigateBack)
            }
        },
        emptyDetailPane = {
            EmptyState(
                icon = OriIcons.People,
                title = stringResource(R.string.care_recipients_choose_title),
                message = stringResource(R.string.care_recipients_choose_message)
            )
        }
    )
}
