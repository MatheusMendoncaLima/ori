package com.oriteam.ori.ui.components.photo

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A photo kept by the app (see `DevicePhotoStore`), cropped to [shape] in a [size] square. Stored photos
 * are small, so decoding is quick; it still runs off the main thread. Until it is decoded, or when this
 * device does not have the file, [placeholder] is drawn instead. Decorative: the text next to it says
 * what it is.
 */
@Composable
fun StoredPhotoImage(
    path: String?,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit
) {
    // A photo decoded a moment ago (a grid scrolled back, the same photo elsewhere) shows at once.
    val bitmap by produceState(initialValue = path?.let(DecodedPhotos::get), path) {
        if (value == null && path != null) value = withContext(Dispatchers.IO) { DecodedPhotos.decode(path) }
    }
    val loaded = bitmap
    if (loaded == null) {
        placeholder()
    } else {
        Image(
            bitmap = loaded,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    }
}

/**
 * Photos decoded recently, kept by path. Stored photos are at most 512 px, about 1 MB decoded; the cache
 * keeps at most a sixteenth of the memory the app may use and drops the least recent first. A path never
 * changes content: symbol photos have unique names, and the profile copy is named after its content.
 */
internal object DecodedPhotos {
    private const val SHARE_OF_MEMORY = 16
    private const val BYTES_PER_PIXEL = 4

    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / SHARE_OF_MEMORY).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * BYTES_PER_PIXEL
    }

    fun get(path: String): ImageBitmap? = cache.get(path)

    /** Decodes off the main thread (the caller chooses the dispatcher); a missing file gives null. */
    fun decode(path: String): ImageBitmap? =
        BitmapFactory.decodeFile(path)?.asImageBitmap()?.also { cache.put(path, it) }
}
