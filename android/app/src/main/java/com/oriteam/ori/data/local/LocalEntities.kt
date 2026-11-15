package com.oriteam.ori.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Same meaning as the Firestore documents; dates and times are text for the same reason. */
@Entity(tableName = "routine_tasks", indices = [Index("date")])
data class RoutineTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val scheduledTime: String?,
    val description: String?,
    val isHiddenFromCaregiver: Boolean,
    val isDone: Boolean
)

/** A repeating activity: the rule, never one row per day. See domain/routine/RoutineSeries.kt. */
@Entity(tableName = "routine_series")
data class RoutineSeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: String,
    val scheduledTime: String?,
    val description: String?,
    val isHiddenFromCaregiver: Boolean,
    val repeatRule: String,
    /** Dates separated by commas. */
    val skippedDates: String
)

@Entity(tableName = "aac_boards")
data class AacBoardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    /** Null for a board from before version 4 of the database: it is in the app's language. */
    val language: String? = null
)

@Entity(
    tableName = "aac_symbols",
    indices = [Index("boardId")],
    foreignKeys = [
        ForeignKey(
            entity = AacBoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            // No symbol is left without a board, as in the Firestore batch.
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AacSymbolEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val label: String,
    val phrase: String?,
    val image: String?,
    val position: Int,
    /** [SymbolColor.key] of the chosen tile color; null (and every symbol from before version 5) has none. */
    val color: String? = null,
    /** The stored [com.oriteam.ori.domain.aac.WordClass] chosen for the word; null lets the tables say. */
    val wordClass: String? = null
)

@Entity(tableName = "saved_messages")
data class SavedMessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAtMillis: Long,
    /** Null for a message from before version 4 of the database: it is in the app's language. */
    val language: String? = null,
    /** The chosen picture; null (and every message from before version 5) shows the app's picture. */
    val emoji: String? = null,
    /** The place the person moved it to; null (and every message from before version 6) was never moved. */
    val position: Int? = null
)

/** How many times "Falar" was used on a day. Stays on the device: never sent to the account. */
@Entity(tableName = "aac_usage")
data class AacUsageEntity(
    @PrimaryKey val date: String,
    val count: Int
)
