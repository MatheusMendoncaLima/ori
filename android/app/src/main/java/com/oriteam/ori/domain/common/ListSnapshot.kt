package com.oriteam.ori.domain.common

/** A bounded list read plus where it came from, so the screen can be honest about stale data. */
data class ListSnapshot<T>(
    val items: List<T>,
    /** True when the server could not be reached and the device cache answered. */
    val isFromCache: Boolean = false,
    /** True while some local change has not been confirmed by the server. */
    val hasPendingWrites: Boolean = false
)
