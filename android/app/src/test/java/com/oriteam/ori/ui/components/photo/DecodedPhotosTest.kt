package com.oriteam.ori.ui.components.photo

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DecodedPhotosTest {

    private fun photoFile(): String {
        val file = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "cached.png")
        file.outputStream().use {
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file.absolutePath
    }

    @Test
    fun `a decoded photo is there the next time, so scrolling back shows it at once`() {
        val path = photoFile()

        val decoded = DecodedPhotos.decode(path)

        assertNotNull(decoded)
        assertSame(decoded, DecodedPhotos.get(path))
    }

    @Test
    fun `a missing file decodes to nothing and is not kept`() {
        assertNull(DecodedPhotos.decode("/nowhere/missing.jpg"))
        assertNull(DecodedPhotos.get("/nowhere/missing.jpg"))
    }
}
