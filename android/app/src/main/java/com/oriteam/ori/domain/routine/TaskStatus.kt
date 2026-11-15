package com.oriteam.ori.domain.routine

/**
 * How an activity is stored (`status` in Firestore, the same name in the local database).
 *
 * [ARCHIVED] is reserved: nothing in the app archives an activity today, but the rules accept it and the day lists
 * skip it ([forDay]), so an archived activity (from an old version or a future "archive" action) never shows up as
 * a normal one. Removing it would change stored data and the rules, for no gain now.
 */
enum class TaskStatus {
    ACTIVE,
    HIDDEN_FROM_CAREGIVER,
    ARCHIVED
}
