package com.oriteam.ori.ui.app.screens.aac

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.oriteam.ori.ui.components.state.ContentState

/** A row of the organizer. Move buttons replace drag and drop, which is hard for many people. */
@Immutable
data class OrganizerItemUiModel<T>(
    val item: T,
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean
)

/** The dialog that names a new board ([boardId] null) or renames one. */
@Immutable
data class BoardNameDialog(
    val boardId: String?,
    val input: String = "",
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null
)

/** Deleting cannot be undone, so it always passes through a confirmation. */
sealed interface PendingAacDeletion {
    val name: String

    data class Board(val id: String, override val name: String) : PendingAacDeletion

    data class Symbol(val id: String, override val name: String) : PendingAacDeletion
}

@Immutable
data class AacOrganizeUiState(
    val canManage: Boolean = false,
    val canUsePhotos: Boolean = false,
    val boards: ContentState<List<OrganizerItemUiModel<BoardTabUiModel>>> = ContentState.Loading,
    val selectedBoardId: String? = null,
    val symbols: ContentState<List<OrganizerItemUiModel<AacSymbolUiModel>>> = ContentState.Loading,
    /** Words of the suggested board the open board lacks; above zero, "Acrescentar" offers them. */
    val suggestedWordsToAdd: Int = 0,
    val isAddingSuggestedWords: Boolean = false,
    val boardDialog: BoardNameDialog? = null,
    val pendingDeletion: PendingAacDeletion? = null,
    @StringRes val actionErrorRes: Int? = null,
    @StringRes val syncNoticeRes: Int? = null
) {
    val selectedBoardName: String?
        get() = (boards as? ContentState.Content)?.value?.firstOrNull { it.item.id == selectedBoardId }?.item?.name
}
