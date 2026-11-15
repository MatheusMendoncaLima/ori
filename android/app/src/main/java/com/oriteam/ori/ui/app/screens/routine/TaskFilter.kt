package com.oriteam.ori.ui.app.screens.routine

/** Which of today's activities the list shows. */
enum class TaskFilter { PENDING, DONE, ALL }

/** [justDone]: activities marked a moment ago under "Pendentes", kept in sight so a tap by mistake can be undone. */
internal fun List<RoutineStepUiModel>.filteredBy(
    filter: TaskFilter,
    justDone: Set<String> = emptySet()
): List<RoutineStepUiModel> = when (filter) {
    TaskFilter.PENDING -> filter { !it.isDone || it.id in justDone }
    TaskFilter.DONE -> filter { it.isDone }
    TaskFilter.ALL -> this
}
