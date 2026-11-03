package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.carelink.CareLinkPermissions

/** Who is looking at a routine. The repository uses it to build a query the rules accept. */
sealed interface RoutineViewer {
    data object Owner : RoutineViewer

    data class Caregiver(val permissions: CareLinkPermissions) : RoutineViewer
}
