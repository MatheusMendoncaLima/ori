package com.oriteam.ori.testing

import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.AacViewer
import com.oriteam.ori.domain.aac.SymbolImage
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.ListSnapshot

val communicationManager = AacViewer.Caregiver(
    CareLinkPermissions(viewRoutines = true, manageRoutines = false, manageCommunication = true)
)
val routineOnlyCaregiver = AacViewer.Caregiver(
    CareLinkPermissions(viewRoutines = true, manageRoutines = true, manageCommunication = false)
)

fun board(id: String, name: String = "Prancha $id", position: Int = 0, createdBy: String = OWNER) =
    AacBoard(id = id, name = name, position = position, createdBy = createdBy)

fun symbol(
    id: String,
    label: String = "Símbolo $id",
    phrase: String? = null,
    image: SymbolImage? = SymbolImage.Emoji("💧"),
    position: Int = 0,
    boardId: String = "b1",
    createdBy: String = OWNER
) = AacSymbol(
    id = id,
    boardId = boardId,
    label = label,
    phrase = phrase,
    image = image,
    position = position,
    createdBy = createdBy
)

fun <T> listSnapshotOf(vararg items: T, isFromCache: Boolean = false, hasPendingWrites: Boolean = false) =
    DataResult.Success(ListSnapshot(items.toList(), isFromCache, hasPendingWrites))
