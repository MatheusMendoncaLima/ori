package com.oriteam.ori.data.account

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.oriteam.ori.domain.account.UserFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

private const val TAG = "UserFileWriter"

/** The [UserFileWriter] of Android: the content resolver opens the chosen file. */
class ContentResolverFileWriter @Inject constructor(
    @ApplicationContext private val context: Context
) : UserFileWriter {
    override suspend fun write(uri: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri.toUri(), "wt")?.use { it.write(text.toByteArray()) } != null
        } catch (exception: IOException) {
            Log.w(TAG, "Could not write the export: ${exception.javaClass.simpleName}")
            false
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not write the export: ${exception.javaClass.simpleName}")
            false
        }
    }
}
