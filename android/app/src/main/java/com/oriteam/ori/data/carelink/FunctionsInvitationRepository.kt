package com.oriteam.ori.data.carelink

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.oriteam.ori.data.firestore.runDataOperation
import com.oriteam.ori.domain.carelink.CareInvitation
import com.oriteam.ori.domain.carelink.InvitationRepository
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Callables answer in under a second; a short timeout turns a dead connection into a clear error. */
private const val CALLABLE_TIMEOUT_SECONDS = 15L

/**
 * Calls the callable Cloud Functions in `functions/functions/src/index.ts`.
 *
 * Online only: there is no offline queue, because a link must be validated by the server.
 * The server operations are idempotent, so the user can safely tap "try again" after a timeout.
 */
class FunctionsInvitationRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : InvitationRepository {

    private fun callable(name: String): HttpsCallableReference =
        functions.getHttpsCallable(name).apply { setTimeout(CALLABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS) }

    override suspend fun createInvitation(): DataResult<CareInvitation> {
        val result = runDataOperation {
            callable("createCareInvitation").call().await().getData() as? Map<*, *>
        }
        return result.mapResponse { data ->
            val code = data["code"] as? String
            val expiresAt = (data["expiresAtMillis"] as? Number)?.toLong()
            if (code != null && expiresAt != null) CareInvitation(code, expiresAt) else null
        }
    }

    override suspend fun acceptInvitation(code: String): DataResult<String> {
        val result = runDataOperation {
            callable("acceptCareInvitation").call(mapOf("code" to code)).await().getData() as? Map<*, *>
        }
        return result.mapResponse { data -> data["caregiverName"] as? String }
    }

    /** An unexpected response shape is reported as an error, never as a silent success. */
    private fun <T> DataResult<Map<*, *>?>.mapResponse(parse: (Map<*, *>) -> T?): DataResult<T> = when (this) {
        is DataResult.Failure -> this
        is DataResult.Success -> value?.let(parse)?.let { DataResult.Success(it) }
            ?: DataResult.Failure(DataError.UNKNOWN)
    }
}
