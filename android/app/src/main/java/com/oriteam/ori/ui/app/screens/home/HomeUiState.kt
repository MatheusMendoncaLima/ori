package com.oriteam.ori.ui.app.screens.home

data class HomeUiState(
    val showOptions: Boolean = false,
    /** Whether the opening animation of the logo plays; null while the device preference is read. */
    val playsIntro: Boolean? = null
)
