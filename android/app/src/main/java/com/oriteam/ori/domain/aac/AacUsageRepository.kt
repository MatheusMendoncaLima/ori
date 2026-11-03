package com.oriteam.ori.domain.aac

import java.time.LocalDate

/**
 * Counts how many times the person communicated a message, for the reports: "Falar" (with the voice,
 * or in large letters when there is none), "Mostrar", a ready phrase, or a symbol in the mode that says
 * each symbol. The voice that repeats each symbol while a message is built is feedback, not counted.
 *
 * Only the number per day is kept, never what was said, and it stays on the device: no account or
 * caregiver can read it. So two devices of the same person count separately, and the report says so.
 */
interface AacUsageRepository {
    suspend fun recordCommunication(date: LocalDate)

    suspend fun countBetween(start: LocalDate, end: LocalDate): Int
}
