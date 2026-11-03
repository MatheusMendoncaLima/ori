package com.oriteam.ori.ui.app.screens.aac

/** What the message bar can do; grouped so screens pass one value instead of eight callbacks. */
data class AacMessageBarActions(
    val onRemovePart: (index: Int) -> Unit,
    val onRemoveLast: () -> Unit,
    val onClear: () -> Unit,
    val onSpeak: () -> Unit,
    val onStop: () -> Unit,
    val onShow: () -> Unit,
    val onSave: () -> Unit,
    val onTyped: (String) -> Unit,
    val onOrganize: () -> Unit = {},
    val onPreferences: () -> Unit = {},
    /** Keeps a typed word on the board, with a picture; null when no board is open. */
    val onSaveWord: ((String) -> Unit)? = null
) {
    companion object {
        val None = AacMessageBarActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}
