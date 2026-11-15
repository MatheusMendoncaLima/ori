package com.oriteam.ori.domain.account

import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult

/**
 * A copy of everything the app keeps about the person (LGPD art. 18, access and portability): one JSON text with a
 * section per kind of data. AGENTS.md (section 14) asks every new kind of data to appear here.
 */
interface AccountDataExporter {
    suspend fun export(ownerUid: String): DataResult<String>
}

/** What deleting the account gave. */
sealed interface AccountDeletionResult {
    /** Everything was deleted, and the account signed out by itself. */
    data object Deleted : AccountDeletionResult

    /** For safety, Firebase asks for a recent sign-in before deleting an account: nothing was deleted. */
    data object NeedsRecentSignIn : AccountDeletionResult

    data class Failed(val error: DataError) : AccountDeletionResult
}

/**
 * Deletes the account and all its data (LGPD art. 18, Google Play): the routine, the boards, the saved phrases, the
 * photos, the preferences, the links, the consent, the profile and the sign-in, in that order, so a failure in the
 * middle never leaves a profile without its data the person could no longer reach.
 */
interface AccountDeletion {
    suspend fun deleteAccount(ownerUid: String): AccountDeletionResult
}

/**
 * The export as JSON, without a library: strings, numbers, booleans, null, lists and maps; anything else is written
 * as its text. Keys keep their order, so the file reads in the order of [sections].
 */
fun accountExportJson(sections: Map<String, Any?>): String = StringBuilder().also { it.appendJson(sections) }.toString()

private fun StringBuilder.appendJson(value: Any?) {
    when (value) {
        null -> append("null")
        is Boolean, is Int, is Long, is Double, is Float -> append(value.toString())
        is Map<*, *> -> {
            append('{')
            value.entries.forEachIndexed { index, (key, item) ->
                if (index > 0) append(',')
                appendString(key.toString())
                append(':')
                appendJson(item)
            }
            append('}')
        }
        is Iterable<*> -> {
            append('[')
            value.forEachIndexed { index, item ->
                if (index > 0) append(',')
                appendJson(item)
            }
            append(']')
        }
        else -> appendString(value.toString())
    }
}

private const val FIRST_PRINTABLE = 0x20

private fun StringBuilder.appendString(text: String) {
    append('"')
    text.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char.code < FIRST_PRINTABLE) append("\\u%04x".format(char.code)) else append(char)
        }
    }
    append('"')
}
