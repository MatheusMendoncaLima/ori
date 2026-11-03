package com.oriteam.ori.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.photos.MAX_PHOTO_BYTES
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.testing.FakeCloudPhotoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/** Uses real files and real image decoding (Robolectric native graphics). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DevicePhotoStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cloud = FakeCloudPhotoRepository()
    private val store = DevicePhotoStore(context, cloud)

    private fun pictureFile(width: Int, height: Int): Uri {
        val file = File(context.cacheDir, "source-${width}x$height.png")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun `a large picture is stored as a small copy`() = runTest {
        val result = store.importPhoto(pictureFile(3000, 2000).toString())

        val photo = (result as PhotoImportResult.Imported).photo
        val path = store.pathOf(photo)!!
        val stored = BitmapFactory.decodeFile(path)
        assertEquals(MAX_PHOTO_SIDE_PX, maxOf(stored.width, stored.height))
        assertTrue(File(path).length() < 200_000)
    }

    private fun noise(side: Int): Bitmap {
        val random = Random(1)
        val pixels = IntArray(side * side) { random.nextInt() or 0xFF000000.toInt() }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `even the busiest picture stays within the limit of the account`() {
        val bytes = compressedWithinLimit(noise(MAX_PHOTO_SIDE_PX))

        assertTrue("${bytes.size} bytes", bytes.size <= MAX_PHOTO_BYTES)
        assertTrue(BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null)
    }

    @Test
    fun `with an account a photo goes there, and without one it stays here`() = runTest {
        val photo = (store.importPhoto(pictureFile(100, 100).toString()) as PhotoImportResult.Imported).photo

        store.upload("account", photo)
        assertTrue(("account" to photo.fileName) in cloud.photos.value.keys)

        cloud.photos.value = emptyMap()
        store.upload(LOCAL_OWNER_UID, photo)
        assertTrue(cloud.photos.value.isEmpty())
    }

    @Test
    fun `another device brings the photo it does not have, once`() = runTest {
        val missing = StoredPhoto("from-other-device.jpg")
        cloud.photos.value = mapOf(("account" to missing.fileName) to byteArrayOf(1, 2, 3))

        assertTrue(store.fetchMissing("account", listOf(missing)))
        assertTrue(store.pathOf(missing) != null)
        assertFalse(store.fetchMissing("account", listOf(missing)))
    }

    @Test
    fun `without an account nothing is asked of the network, and a file name from outside is refused`() = runTest {
        val missing = StoredPhoto("a.jpg")
        cloud.photos.value = mapOf((LOCAL_OWNER_UID to missing.fileName) to byteArrayOf(1))

        assertFalse(store.fetchMissing(LOCAL_OWNER_UID, listOf(missing)))
        assertFalse(store.saveBytes(StoredPhoto("../outside.jpg"), byteArrayOf(1)))
    }

    @Test
    fun `deleting removes the photo from the device and from the account`() = runTest {
        val photo = (store.importPhoto(pictureFile(100, 100).toString()) as PhotoImportResult.Imported).photo
        store.upload("account", photo)

        store.deleteEverywhere("account", photo)

        assertTrue(store.pathOf(photo) == null)
        assertTrue(cloud.photos.value.isEmpty())
    }

    @Test
    fun `a file that is not an image is reported as unreadable`() = runTest {
        val text = File(context.cacheDir, "notes.txt").apply { writeText("not a picture") }

        assertEquals(
            PhotoImportResult.Failed(PhotoImportError.UNREADABLE),
            store.importPhoto(Uri.fromFile(text).toString())
        )
        assertEquals(
            PhotoImportResult.Failed(PhotoImportError.UNREADABLE),
            store.importPhoto(Uri.fromFile(File(context.cacheDir, "missing.png")).toString())
        )
    }

    @Test
    fun `a storage failure is reported and leaves no file behind`() = runTest {
        // A plain file where the photo folder should be makes every write fail.
        val folder = File(context.filesDir, "aac_photos")
        folder.deleteRecursively()
        folder.writeText("blocked")

        val result = store.importPhoto(pictureFile(100, 100).toString())

        assertEquals(PhotoImportResult.Failed(PhotoImportError.STORAGE_FAILED), result)
        assertTrue(folder.isFile)
        folder.delete()
    }

    @Test
    fun `deleting removes the file and unsafe names are never resolved`() = runTest {
        val photo = (store.importPhoto(pictureFile(10, 10).toString()) as PhotoImportResult.Imported).photo

        store.delete(photo)

        assertNull(store.pathOf(photo))
        assertNull(store.pathOf(StoredPhoto("../shared_prefs/x.jpg")))
        assertEquals(1, sampleSizeFor(1000, 800, maxSide = 512))
        assertEquals(4, sampleSizeFor(4000, 3000, maxSide = 512))
    }
}
