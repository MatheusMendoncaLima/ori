package com.oriteam.ori.domain.aac

/**
 * What the interface offers. The same decisions are enforced by `functions/firestore.rules`;
 * these functions only avoid showing actions that the server would refuse.
 */
fun canManageBoards(viewer: AacViewer): Boolean = when (viewer) {
    AacViewer.Owner -> true
    // Configuring someone's voice needs an explicit permission, off by default.
    is AacViewer.Caregiver -> viewer.permissions.manageCommunication
}

/**
 * Saved messages are what the person said or wants to say: private to them, even for a
 * caregiver who manages their boards.
 */
fun canUseSavedMessages(viewer: AacViewer): Boolean = viewer == AacViewer.Owner

/**
 * Only the owner adds photos: they go to the owner's account, whose space and limit of photos are the owner's. A
 * caregiver who manages the boards uses emoji, and keeps a photo the owner already chose.
 */
fun canUseDevicePhotos(viewer: AacViewer): Boolean = viewer == AacViewer.Owner
