package com.oriteam.ori.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.io.File

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_tasks WHERE date = :date LIMIT :limit")
    fun observeDay(date: String, limit: Int): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks WHERE date = :date LIMIT :limit")
    suspend fun getDay(date: String, limit: Int): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE id = :id")
    suspend fun get(id: String): RoutineTaskEntity?

    @Query("SELECT * FROM routine_tasks")
    suspend fun getAll(): List<RoutineTaskEntity>

    @Upsert
    suspend fun upsert(task: RoutineTaskEntity)

    @Query("UPDATE routine_tasks SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(id: String, isDone: Boolean)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM routine_tasks")
    suspend fun deleteAll()
}

@Dao
interface RoutineSeriesDao {
    @Query("SELECT * FROM routine_series LIMIT :limit")
    fun observe(limit: Int): Flow<List<RoutineSeriesEntity>>

    @Query("SELECT * FROM routine_series")
    suspend fun getAll(): List<RoutineSeriesEntity>

    @Query("SELECT * FROM routine_series WHERE id = :id")
    suspend fun get(id: String): RoutineSeriesEntity?

    @Upsert
    suspend fun upsert(series: RoutineSeriesEntity)

    @Query("DELETE FROM routine_series WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM routine_series")
    suspend fun deleteAll()
}

@Dao
@Suppress("TooManyFunctions") // One query per operation of the boards and their symbols.
interface AacDao {
    @Query("SELECT * FROM aac_boards LIMIT :limit")
    fun observeBoards(limit: Int): Flow<List<AacBoardEntity>>

    @Query("SELECT * FROM aac_boards")
    suspend fun getBoards(): List<AacBoardEntity>

    @Query("SELECT * FROM aac_symbols WHERE boardId = :boardId LIMIT :limit")
    fun observeSymbols(boardId: String, limit: Int): Flow<List<AacSymbolEntity>>

    @Query("SELECT * FROM aac_symbols WHERE boardId = :boardId LIMIT :limit")
    suspend fun getSymbols(boardId: String, limit: Int): List<AacSymbolEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBoard(board: AacBoardEntity)

    @Upsert
    suspend fun upsertSymbol(symbol: AacSymbolEntity)

    @Transaction
    suspend fun insertBoardWithSymbols(board: AacBoardEntity, symbols: List<AacSymbolEntity>) {
        insertBoard(board)
        symbols.forEach { upsertSymbol(it) }
    }

    @Query("UPDATE aac_boards SET name = :name WHERE id = :id")
    suspend fun renameBoard(id: String, name: String)

    @Query("UPDATE aac_boards SET position = :position WHERE id = :id")
    suspend fun moveBoard(id: String, position: Int)

    @Query("UPDATE aac_symbols SET position = :position WHERE id = :id")
    suspend fun moveSymbol(id: String, position: Int)

    @Query("UPDATE aac_symbols SET label = :label, phrase = :phrase, image = :image WHERE id = :id")
    suspend fun updateSymbol(id: String, label: String, phrase: String?, image: String?)

    @Query("DELETE FROM aac_boards WHERE id = :id")
    suspend fun deleteBoard(id: String)

    @Query("DELETE FROM aac_symbols WHERE id = :id")
    suspend fun deleteSymbol(id: String)

    @Query("DELETE FROM aac_boards")
    suspend fun deleteAllBoards()

    @Transaction
    suspend fun moveBoards(
        positions: Map<String, Int>
    ) = positions.forEach { (id, position) -> moveBoard(id, position) }

    @Transaction
    suspend fun moveSymbols(positions: Map<String, Int>) =
        positions.forEach { (id, position) -> moveSymbol(id, position) }
}

@Dao
interface SavedMessageDao {
    @Query("SELECT * FROM saved_messages ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<SavedMessageEntity>>

    @Query("SELECT * FROM saved_messages")
    suspend fun getAll(): List<SavedMessageEntity>

    @Upsert
    suspend fun upsert(message: SavedMessageEntity)

    @Query("DELETE FROM saved_messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE saved_messages SET emoji = :emoji WHERE id = :id")
    suspend fun setEmoji(id: String, emoji: String?)

    @Query("UPDATE saved_messages SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    @Transaction
    suspend fun move(positions: Map<String, Int>) = positions.forEach { (id, position) -> setPosition(id, position) }

    @Query("DELETE FROM saved_messages")
    suspend fun deleteAll()
}

@Dao
interface AacUsageDao {
    @Query("INSERT OR IGNORE INTO aac_usage (date, count) VALUES (:date, 0)")
    suspend fun ensureDay(date: String)

    @Query("UPDATE aac_usage SET count = count + 1 WHERE date = :date")
    suspend fun increment(date: String)

    @Transaction
    suspend fun record(date: String) {
        ensureDay(date)
        increment(date)
    }

    @Query("SELECT COALESCE(SUM(count), 0) FROM aac_usage WHERE date BETWEEN :start AND :end")
    suspend fun countBetween(start: String, end: String): Int

    @Query("DELETE FROM aac_usage")
    suspend fun deleteAll()
}

/** The ORI data that lives only on this device. See docs/funcionalidades/uso-local.md. */
@Database(
    entities = [
        RoutineTaskEntity::class,
        AacBoardEntity::class,
        AacSymbolEntity::class,
        SavedMessageEntity::class,
        AacUsageEntity::class,
        RoutineSeriesEntity::class
    ],
    version = 6,
    // Versions 1 and 2 were never exported; from 3 on, app/schemas keeps each one.
    exportSchema = true
)
abstract class OriDatabase : RoomDatabase() {
    companion object {
        private const val VERSION_WITH_SERIES = 2
        private const val VERSION_WITHOUT_MEDIA = 3
        private const val VERSION_WITH_LANGUAGES = 4
        private const val VERSION_WITH_SAVED_EMOJI = 5
        private const val VERSION_WITH_SAVED_ORDER = 6

        /** Adds repeating activities. Everything people already have stays where it is. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS routine_series (" +
                        "id TEXT NOT NULL, title TEXT NOT NULL, startDate TEXT NOT NULL, " +
                        "scheduledTime TEXT, description TEXT, isHiddenFromCaregiver INTEGER NOT NULL, " +
                        "repeatRule TEXT NOT NULL, skippedDates TEXT NOT NULL, PRIMARY KEY(id))"
                )
            }
        }

        /**
         * The "Mídia" menu left the app. The files were copies (the originals stay in the gallery),
         * so they go too instead of filling the storage of the device forever.
         */
        fun migration2To3(filesDirectory: File) = object : Migration(VERSION_WITH_SERIES, VERSION_WITHOUT_MEDIA) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS media_items")
                File(filesDirectory, "media").deleteRecursively()
            }
        }

        /**
         * Boards and saved messages record their language. What already exists stays without one (null), and
         * is read as being in the app's language, which it was written in.
         */
        val MIGRATION_3_4 = object : Migration(VERSION_WITHOUT_MEDIA, VERSION_WITH_LANGUAGES) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aac_boards ADD COLUMN language TEXT")
                db.execSQL("ALTER TABLE saved_messages ADD COLUMN language TEXT")
            }
        }

        /**
         * Saved messages may have their own picture, and symbols their own color and kind of word. What exists
         * keeps the app's picture, and the color and kind the grammar gives (null).
         */
        val MIGRATION_4_5 = object : Migration(VERSION_WITH_LANGUAGES, VERSION_WITH_SAVED_EMOJI) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_messages ADD COLUMN emoji TEXT")
                db.execSQL("ALTER TABLE aac_symbols ADD COLUMN color TEXT")
                db.execSQL("ALTER TABLE aac_symbols ADD COLUMN wordClass TEXT")
            }
        }

        /** Saved messages may be moved. What exists was never moved (null), and keeps its place: newest first. */
        val MIGRATION_5_6 = object : Migration(VERSION_WITH_SAVED_EMOJI, VERSION_WITH_SAVED_ORDER) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_messages ADD COLUMN position INTEGER")
            }
        }
    }

    abstract fun routineDao(): RoutineDao
    abstract fun routineSeriesDao(): RoutineSeriesDao
    abstract fun aacDao(): AacDao
    abstract fun savedMessageDao(): SavedMessageDao
    abstract fun aacUsageDao(): AacUsageDao
}
