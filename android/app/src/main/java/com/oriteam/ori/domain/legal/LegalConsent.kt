package com.oriteam.ori.domain.legal

import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus

/**
 * The version of the privacy policy and the terms (web/app/legal/content.ts has the same number). Raise it when a
 * change needs the person's consent again: everyone sees the consent screen on the next opening.
 */
const val LEGAL_DOCUMENTS_VERSION = 1

/**
 * What the person agreed to, on the first use: the terms and the policy, and the use of sensitive data (routine and
 * communication can reveal health or disability, LGPD art. 11). Under 18, a legal guardian confirms (art. 14).
 */
data class LegalConsent(
    val version: Int,
    val isAdult: Boolean,
    val hasGuardianConsent: Boolean,
    val acceptedAtMillis: Long
)

/** The consent that lets the app go on: of the current version, and from a guardian for someone under 18. */
val LegalConsent?.isCurrent: Boolean
    get() = this != null && version >= LEGAL_DOCUMENTS_VERSION && (isAdult || hasGuardianConsent)

/**
 * Where the consent is kept: on the device (so the app opens offline) and, with an account, also in it, as the
 * record of when and which version was accepted.
 */
interface ConsentRepository {
    /** Null when the person never agreed (or the device has no copy and the account has none). */
    suspend fun getConsent(ownerUid: String): DataResult<LegalConsent?>

    suspend fun recordConsent(ownerUid: String, consent: LegalConsent): DataResult<WriteStatus>

    /** Withdrawing: the app asks again. Used when the data is erased. */
    suspend fun clearConsent(ownerUid: String): DataResult<WriteStatus>
}
