package com.oriteam.ori.domain.routine

import com.oriteam.ori.domain.model.TaskStatus

/**
 * What a viewer may do with a routine. These functions mirror `functions/firestore.rules`
 * so the interface only offers possible actions; the rules are what protect the data.
 *
 * - The primary user creates, edits and deletes **their own** activities, and may hide them
 *   from the caregiver. Activities created by the caregiver are reserved to the caregiver:
 *   the primary user can only mark them as done, and only when [RoutineTask.ownerCanComplete].
 * - The caregiver needs `manageRoutines` to create, edit, delete or complete, and never
 *   touches activities hidden from them.
 */
fun canCreateActivity(viewer: RoutineViewer): Boolean = when (viewer) {
    RoutineViewer.Owner -> true
    is RoutineViewer.Caregiver -> viewer.permissions.manageRoutines
}

fun canEditActivity(task: RoutineTask, viewer: RoutineViewer): Boolean = when (viewer) {
    RoutineViewer.Owner -> task.isCreatedByOwner
    is RoutineViewer.Caregiver ->
        viewer.permissions.manageRoutines && task.status != TaskStatus.HIDDEN_FROM_CAREGIVER
}

/** Deleting follows the same ownership as editing. */
fun canDeleteActivity(task: RoutineTask, viewer: RoutineViewer): Boolean = canEditActivity(task, viewer)

fun canToggleCompletion(task: RoutineTask, viewer: RoutineViewer): Boolean = when (viewer) {
    RoutineViewer.Owner -> task.isCreatedByOwner || task.ownerCanComplete
    is RoutineViewer.Caregiver -> canEditActivity(task, viewer)
}
