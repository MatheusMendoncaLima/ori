package com.oriteam.ori.ui.app.screens.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest

/**
 * How long a notice about the connection waits before it appears: the same time a write waits for
 * the server before it counts as pending (see FirestorePolicies).
 */
internal const val SYNC_NOTICE_DELAY_MILLIS = 3_000L

/**
 * Online, every change is "waiting for the server" for a moment, and a day opens from the device
 * copy before the server answers. Saying so at once only makes a sentence flash on the screen. A
 * notice appears only when the situation lasts; the data itself is never held back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.withLastingSyncNotice(notice: (T) -> Int?, withoutNotice: (T) -> T): Flow<T> =
    transformLatest { value ->
        if (notice(value) != null) {
            emit(withoutNotice(value))
            delay(SYNC_NOTICE_DELAY_MILLIS)
        }
        emit(value)
    }
