package com.oriteam.ori.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Which soft color ([OriExtraColors.accents]) stands for each area of the app. The menus use them,
 * and so do the things that belong to an area (the report about the routine has the routine color).
 * The pages of Configurações, the symbols and the activities have their own sets
 * ([OriExtraColors.settings], [OriExtraColors.words], [OriExtraColors.dayParts]).
 */
object OriAreaAccents {
    const val ROUTINE = 0
    const val TASKS = 1
    const val REPORTS = 2
    const val PEOPLE = 3
    const val COMMUNICATION = 4
    const val CARE = 5
}

/** The color of an [area][OriAreaAccents], in the current theme. */
@Composable
@ReadOnlyComposable
fun areaAccent(area: Int): OriAccent = LocalOriColors.current.accents[area]
