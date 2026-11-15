package com.oriteam.ori.ui.app.screens.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

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
/**
 * How long a screen keeps what it shows while the next data is read. Another day usually comes from
 * the device copy in a few milliseconds: "Carregando" would only flash between two lists.
 */
internal const val LOADING_GRACE_MILLIS = 300L

/** Emits [placeholder] only when this flow has said nothing for [graceMillis]; until then, nothing changes. */
internal fun <T> Flow<T>.loadingAfter(graceMillis: Long = LOADING_GRACE_MILLIS, placeholder: () -> T): Flow<T> =
    channelFlow {
        val loading = launch {
            delay(graceMillis)
            send(placeholder())
        }
        collect { value ->
            loading.cancel()
            send(value)
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.withLastingSyncNotice(notice: (T) -> Int?, withoutNotice: (T) -> T): Flow<T> =
    transformLatest { value ->
        if (notice(value) != null) {
            emit(withoutNotice(value))
            delay(SYNC_NOTICE_DELAY_MILLIS)
        }
        emit(value)
    }
