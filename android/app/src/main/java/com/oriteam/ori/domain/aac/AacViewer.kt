package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.carelink.CareLinkPermissions

/** Who is looking at someone's communication boards. */
sealed interface AacViewer {
    /** The primary user: communicates with the boards and decides everything about them. */
    data object Owner : AacViewer

    /** A linked caregiver, limited by the permissions of that link. */
    data class Caregiver(val permissions: CareLinkPermissions) : AacViewer
}
