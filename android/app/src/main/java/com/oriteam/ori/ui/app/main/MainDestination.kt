package com.oriteam.ori.ui.app.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.ui.components.icon.OriIcons

/**
 * The four areas of the bottom bar, the same for everyone, with or without an account. Labels are
 * short forms of the screen titles: the full titles wrap in uppercase and misalign the icons.
 */
enum class MainDestination(@StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.main_home, Icons.Filled.Home),
    ROUTINE(R.string.main_routine, Icons.Filled.DateRange),
    COMMUNICATE(R.string.main_sentences, OriIcons.Chat),
    PROFILE(R.string.main_profile, Icons.Filled.Person)
}

/** Screens opened from Início. They keep the bottom bar, so the way back is always visible. */
enum class HomeSubscreen { TASKS, REPORTS }

/**
 * ORI Home asked for [area] while the app was already open (its panel opens the routine or the phrases): the app
 * comes back as it was and only changes the area. Every request is a new object, so the same area twice still counts.
 */
class AreaRequest(val area: StartArea)

/** The area that opens first. */
fun startDestinationFor(startArea: StartArea): MainDestination = when (startArea) {
    StartArea.HOME -> MainDestination.HOME
    StartArea.ROUTINE -> MainDestination.ROUTINE
    StartArea.COMMUNICATION -> MainDestination.COMMUNICATE
}
