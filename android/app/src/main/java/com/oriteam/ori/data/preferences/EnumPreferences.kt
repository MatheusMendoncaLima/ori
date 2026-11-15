package com.oriteam.ori.data.preferences

/**
 * Preferences are stored by enum name. An unknown name (after a downgrade, or a value removed) falls back to the
 * default instead of crashing.
 */
internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
