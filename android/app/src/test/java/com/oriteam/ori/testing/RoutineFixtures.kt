package com.oriteam.ori.testing

import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.routine.RoutineSnapshot
import com.oriteam.ori.domain.routine.RoutineTask
import com.oriteam.ori.domain.routine.RoutineViewer
import com.oriteam.ori.domain.routine.TaskStatus
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

const val OWNER = "pat"
const val CAREGIVER_UID = "cara"
val DAY: LocalDate = LocalDate.of(2026, 10, 29)

/** "Today" is always [DAY] in tests, whatever the computer's date. */
val fixedClock: Clock = Clock.fixed(DAY.atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

val manager = RoutineViewer.Caregiver(CareLinkPermissions(viewRoutines = true, manageRoutines = true))
val viewer = RoutineViewer.Caregiver(CareLinkPermissions(viewRoutines = true, manageRoutines = false))

fun activity(
    id: String,
    title: String = "Atividade $id",
    time: LocalTime? = LocalTime.of(8, 0),
    date: LocalDate = DAY,
    createdBy: String = CAREGIVER_UID,
    ownerCanComplete: Boolean = true,
    isDone: Boolean = false,
    status: TaskStatus = TaskStatus.ACTIVE,
    description: String? = null
) = RoutineTask(
    id = id,
    ownerUid = OWNER,
    title = title,
    date = date,
    scheduledTime = time,
    description = description,
    status = status,
    ownerCanComplete = ownerCanComplete,
    isDone = isDone,
    createdBy = createdBy
)

fun snapshotOf(vararg tasks: RoutineTask, isFromCache: Boolean = false, hasPendingWrites: Boolean = false) =
    DataResult.Success(RoutineSnapshot(tasks.toList(), isFromCache, hasPendingWrites))
