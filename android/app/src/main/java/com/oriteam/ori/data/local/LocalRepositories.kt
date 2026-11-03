package com.oriteam.ori.data.local

import android.database.SQLException
import android.util.Log
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/*
 * What every repository of the device data shares: a database failure is reported, never thrown, and
 * its message is not logged, because it may contain stored content. The repositories are in
 * LocalRoutineRepositories.kt and LocalAacRepositories.kt.
 */

private const val TAG = "OriLocalData"

/** A local write has nothing to wait for, so it is never reported as pending. */
internal suspend fun runLocalWrite(write: suspend () -> Unit): DataResult<WriteStatus> =
    runLocalRead { write().let { WriteStatus.SYNCED } }

internal suspend fun <T> runLocalRead(read: suspend () -> T): DataResult<T> = try {
    DataResult.Success(read())
} catch (exception: SQLException) {
    // Only the class name: the message of a database error may contain stored content.
    Log.w(TAG, "Local database failure: ${exception.javaClass.simpleName}")
    DataResult.Failure(DataError.UNKNOWN)
}

internal fun <T> Flow<T>.asLocalResult(): Flow<DataResult<T>> =
    map<T, DataResult<T>> { DataResult.Success(it) }
        .catch { exception ->
            if (exception !is SQLException) throw exception
            Log.w(TAG, "Local database failure: ${exception.javaClass.simpleName}")
            emit(DataResult.Failure(DataError.UNKNOWN))
        }
