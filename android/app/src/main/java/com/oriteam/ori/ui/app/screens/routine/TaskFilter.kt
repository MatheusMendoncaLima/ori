package com.oriteam.ori.ui.app.screens.routine

/** Which of today's activities the list shows. */
enum class TaskFilter { PENDING, DONE, ALL }

internal fun List<RoutineStepUiModel>.filteredBy(filter: TaskFilter): List<RoutineStepUiModel> = when (filter) {
    TaskFilter.PENDING -> filter { !it.isDone }
    TaskFilter.DONE -> filter { it.isDone }
    TaskFilter.ALL -> this
}
