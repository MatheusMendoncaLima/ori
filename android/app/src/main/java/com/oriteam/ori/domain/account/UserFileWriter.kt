package com.oriteam.ori.domain.account

/** Writes a text into the file the person chose in the system "save as" (a content Uri), off the main thread. */
interface UserFileWriter {
    /** False when the file could not be written (full storage, removed card): the screen says so. */
    suspend fun write(uri: String, text: String): Boolean
}
