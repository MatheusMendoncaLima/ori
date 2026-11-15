package com.oriteam.ori.data.launcher

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.launcher.WallpaperImport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * A background picture is kept as a reduced copy, read back, and deleted; names it did not make are refused. Real
 * image decoding (native graphics): the default fake one keeps the file open, and Windows then refuses to delete it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeviceWallpaperStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val store = DeviceWallpaperStore(ApplicationProvider.getApplicationContext())

    private fun picture(width: Int, height: Int): String {
        val file = File(folder.root, "big.png")
        file.outputStream().use {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return Uri.fromFile(file).toString()
    }

    @Test
    fun `a big picture is kept reduced, read back, and deleted`() = runBlocking {
        val result = store.importPhoto(picture(width = 4000, height = 3000))

        val fileName = (result as WallpaperImport.Imported).fileName
        val loaded = store.load(fileName)
        assertNotNull(loaded)
        loaded!!
        assertTrue("${loaded.width}x${loaded.height}", maxOf(loaded.width, loaded.height) <= MAX_WALLPAPER_SIDE_PX)

        store.delete(fileName)
        assertNull(store.load(fileName))
    }

    @Test
    fun `something that is not a picture fails, and a strange name is never read`() = runBlocking {
        val text = File(folder.root, "note.txt").apply { writeText("not a picture") }

        assertEquals(WallpaperImport.Failed, store.importPhoto(Uri.fromFile(text).toString()))
        assertNull(store.load("../../shared_prefs/secret.xml"))
    }
}
