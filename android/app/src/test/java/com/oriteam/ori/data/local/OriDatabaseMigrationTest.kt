package com.oriteam.ori.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.routine.RepeatRule
import com.oriteam.ori.domain.routine.toStoredText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** People already have data in version 1. Room also validates the new table against the entity here. */
@RunWith(AndroidJUnit4::class)
class OriDatabaseMigrationTest {
    @Test
    fun `updating the app keeps the activities and adds repeating ones`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration_test.db")
        file.parentFile?.mkdirs()
        file.delete()
        val mediaParent = File(context.cacheDir, "migration-files")
        val mediaFile = File(mediaParent, "media/old.jpg").apply {
            parentFile?.mkdirs()
            writeText("x")
        }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL(
                "CREATE TABLE routine_tasks (id TEXT NOT NULL, title TEXT NOT NULL, date TEXT NOT NULL, " +
                    "scheduledTime TEXT, description TEXT, isHiddenFromCaregiver INTEGER NOT NULL, " +
                    "isDone INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            old.execSQL("CREATE INDEX index_routine_tasks_date ON routine_tasks (date)")
            old.execSQL(
                "CREATE TABLE aac_boards (id TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))"
            )
            old.execSQL(
                "CREATE TABLE aac_symbols (id TEXT NOT NULL, boardId TEXT NOT NULL, label TEXT NOT NULL, " +
                    "phrase TEXT, image TEXT, position INTEGER NOT NULL, PRIMARY KEY(id), " +
                    "FOREIGN KEY(boardId) REFERENCES aac_boards(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            old.execSQL("CREATE INDEX index_aac_symbols_boardId ON aac_symbols (boardId)")
            old.execSQL(
                "CREATE TABLE saved_messages (id TEXT NOT NULL, text TEXT NOT NULL, " +
                    "createdAtMillis INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            old.execSQL("CREATE TABLE aac_usage (date TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(date))")
            old.execSQL(
                "CREATE TABLE media_items (id TEXT NOT NULL, label TEXT NOT NULL, kind TEXT NOT NULL, " +
                    "fileName TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            old.execSQL(
                "INSERT INTO routine_tasks VALUES ('t1', 'Terapia', '2026-11-02', NULL, NULL, 0, 1)"
            )
            old.version = 1
        }

        val database = Room.databaseBuilder(context, OriDatabase::class.java, file.absolutePath)
            .addMigrations(OriDatabase.MIGRATION_1_2, OriDatabase.migration2To3(mediaParent))
            .allowMainThreadQueries()
            .build()

        assertEquals("Terapia", database.routineDao().get("t1")?.title)
        database.routineSeriesDao().upsert(
            RoutineSeriesEntity("s1", "Banho", "2026-11-02", null, null, false, RepeatRule.EveryDay.toStoredText(), "")
        )
        assertEquals(1, database.routineSeriesDao().observe(limit = 10).first().size)
        assertFalse("the copies of the removed menu are gone", mediaFile.exists())
        database.query("SELECT name FROM sqlite_master WHERE name = 'media_items'", null).use {
            assertFalse("its table is gone", it.moveToFirst())
        }
        database.close()
    }
}
