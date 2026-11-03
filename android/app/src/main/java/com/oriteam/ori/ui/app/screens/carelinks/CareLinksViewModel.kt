package com.oriteam.ori.ui.app.screens.carelinks

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oriteam.ori.domain.carelink.CareLink
import com.oriteam.ori.domain.carelink.CareLinkRepository
import com.oriteam.ori.domain.carelink.CareLinkSide
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.ui.app.screens.common.messageRes
import com.oriteam.ori.ui.components.state.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class CareLinksUiState(
    val viewerSide: CareLinkSide = CareLinkSide.PRIMARY_USER,
    val links: ContentState<List<CareLink>> = ContentState.Loading,
    /** Link waiting for the user to confirm revocation in a dialog. */
    val pendingRevocation: CareLink? = null,
    @StringRes val actionErrorRes: Int? = null,
    /** The last change was saved on the device and waits for a connection. */
    val hasPendingChange: Boolean = false
)

/** Active care links of the signed-in person: list, revocation and (primary user) permissions. */
@HiltViewModel
class CareLinksViewModel @Inject constructor(
    private val careLinkRepository: CareLinkRepository
) : ViewModel() {

    private data class Request(val uid: String, val side: CareLinkSide, val attempt: Int = 0)

    /** User interaction state that does not come from Firestore. */
    private data class Interaction(
        val pendingRevocation: CareLink? = null,
        val actionErrorRes: Int? = null,
        val hasPendingChange: Boolean = false
    )

    private val request = MutableStateFlow<Request?>(null)
    private val interaction = MutableStateFlow(Interaction())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val links = request.filterNotNull().flatMapLatest { current ->
        careLinkRepository.observeActiveLinks(current.uid, current.side)
            .map<DataResult<List<CareLink>>, ContentState<List<CareLink>>> { result ->
                when (result) {
                    is DataResult.Success ->
                        if (result.value.isEmpty()) ContentState.Empty else ContentState.Content(result.value)
                    is DataResult.Failure -> ContentState.Error(result.error.messageRes())
                }
            }
            .onStart { emit(ContentState.Loading) }
    }

    /** The listener runs only while a screen collects this state. */
    val uiState: StateFlow<CareLinksUiState> = combine(links, interaction) { linkState, local ->
        CareLinksUiState(
            viewerSide = request.value?.side ?: CareLinkSide.PRIMARY_USER,
            links = linkState,
            pendingRevocation = local.pendingRevocation,
            actionErrorRes = local.actionErrorRes,
            hasPendingChange = local.hasPendingChange
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CareLinksUiState())

    fun start(uid: String, side: CareLinkSide) {
        val current = request.value
        if (current?.uid == uid && current.side == side) return
        request.value = Request(uid, side)
    }

    fun onRetry() {
        request.update { it?.copy(attempt = it.attempt + 1) }
    }

    fun onRevokeRequest(link: CareLink) {
        interaction.update { it.copy(pendingRevocation = link) }
    }

    fun onRevokeDismiss() {
        interaction.update { it.copy(pendingRevocation = null) }
    }

    fun onRevokeConfirm() {
        val link = interaction.value.pendingRevocation ?: return
        interaction.update { it.copy(pendingRevocation = null) }
        runAction { careLinkRepository.revoke(link.id) }
    }

    /** The primary user decides whether the caregiver may manage the routine. */
    fun onManagePermissionChange(link: CareLink, isAllowed: Boolean) {
        if (request.value?.side != CareLinkSide.PRIMARY_USER) return
        runAction {
            careLinkRepository.updatePermissions(link.id, link.permissions.copy(manageRoutines = isAllowed))
        }
    }

    /** The primary user decides whether the caregiver may organize their communication boards. */
    fun onCommunicationPermissionChange(link: CareLink, isAllowed: Boolean) {
        if (request.value?.side != CareLinkSide.PRIMARY_USER) return
        runAction {
            careLinkRepository.updatePermissions(link.id, link.permissions.copy(manageCommunication = isAllowed))
        }
    }

    private fun runAction(action: suspend () -> DataResult<WriteStatus>) {
        interaction.update { it.copy(actionErrorRes = null, hasPendingChange = false) }
        viewModelScope.launch {
            when (val result = action()) {
                is DataResult.Failure -> interaction.update { it.copy(actionErrorRes = result.error.messageRes()) }
                is DataResult.Success -> interaction.update {
                    it.copy(hasPendingChange = result.value == WriteStatus.PENDING_SYNC)
                }
            }
        }
    }
}
