package com.oriteam.ori.data.profile

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.photos.PROFILE_PHOTO_ID
import com.oriteam.ori.domain.photos.PhotoImportError
import com.oriteam.ori.domain.photos.PhotoImportResult
import com.oriteam.ori.domain.photos.StoredPhoto
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.testing.FakeCloudPhotoRepository
import com.oriteam.ori.testing.FakePhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

private const val TIMEOUT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class DeviceProfilePhotoStoreTest {
    private val photos = FakePhotoStore()
    private val cloud = FakeCloudPhotoRepository()
    private val store = DeviceProfilePhotoStore(
        photos,
        cloud,
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        }
    )

    /** Waits for the copy of the account picture to arrive on this device (and be different from [other]). */
    private suspend fun DeviceProfilePhotoStore.copyOf(uid: String, other: String? = null): String? =
        withTimeout(TIMEOUT_MILLIS) { observePhoto(uid).first { it != null && it != other } }

    private fun importing(name: String) {
        photos.importResult = PhotoImportResult.Imported(StoredPhoto(name))
    }

    @Test
    fun `without an account the picture stays on the device`() = runBlocking {
        importing("a.jpg")
        store.setPhoto(LOCAL_OWNER_UID, "content://a")

        assertEquals("/photos/a.jpg", store.observePhoto(LOCAL_OWNER_UID).first())
        assertTrue(cloud.photos.value.isEmpty())
    }

    @Test
    fun `with an account it goes to the account and comes back as a copy`() = runBlocking {
        importing("a.jpg")
        store.setPhoto("ana", "content://a")

        assertNotNull(cloud.photos.value["ana" to PROFILE_PHOTO_ID])
        assertNotNull(store.copyOf("ana"))
        assertEquals("the imported file was only a step", listOf(StoredPhoto("a.jpg")), photos.deleted)
    }

    @Test
    fun `another device of the account gets the picture, and each account has its own`() = runBlocking {
        cloud.photos.value = mapOf(("ana" to PROFILE_PHOTO_ID) to byteArrayOf(1, 2))

        assertNotNull(store.copyOf("ana"))
        assertNull(store.observePhoto("bia").first())
    }

    @Test
    fun `a new picture has a new path, so the screen draws it again`() = runBlocking {
        cloud.photos.value = mapOf(("ana" to PROFILE_PHOTO_ID) to byteArrayOf(1))
        val first = store.copyOf("ana")

        cloud.photos.value = mapOf(("ana" to PROFILE_PHOTO_ID) to byteArrayOf(2))
        val second = store.copyOf("ana", other = first)

        assertTrue(first != second)
    }

    @Test
    fun `a picture the account refused is reported`() = runBlocking {
        importing("a.jpg")
        cloud.uploadResult = DataResult.Failure(DataError.PERMISSION_DENIED)

        assertEquals(PhotoImportError.UPLOAD_FAILED, store.setPhoto("ana", "content://a"))
    }

    @Test
    fun `a picture that cannot be read is reported`() = runBlocking {
        photos.importResult = PhotoImportResult.Failed(PhotoImportError.UNREADABLE)

        assertEquals(PhotoImportError.UNREADABLE, store.setPhoto("ana", "content://bad"))
    }

    @Test
    fun `removing takes it from the device and from the account`() = runBlocking {
        cloud.photos.value = mapOf(("ana" to PROFILE_PHOTO_ID) to byteArrayOf(1))
        store.copyOf("ana")

        store.removePhoto("ana")

        assertTrue(cloud.photos.value.isEmpty())
        assertNull(store.observePhoto("ana").first())
    }

    @Test
    fun `the picture follows the person to the account, without replacing one it already has`() = runBlocking {
        importing("local.jpg")
        store.setPhoto(LOCAL_OWNER_UID, "content://a")

        store.movePhoto(LOCAL_OWNER_UID, "account")
        assertNotNull(cloud.photos.value["account" to PROFILE_PHOTO_ID])
        assertNull(store.observePhoto(LOCAL_OWNER_UID).first())

        val kept = cloud.photos.value.getValue("account" to PROFILE_PHOTO_ID)
        importing("again.jpg")
        store.setPhoto(LOCAL_OWNER_UID, "content://b")
        store.movePhoto(LOCAL_OWNER_UID, "account")
        assertTrue(kept.contentEquals(cloud.photos.value.getValue("account" to PROFILE_PHOTO_ID)))
        assertNull(store.observePhoto(LOCAL_OWNER_UID).first())
    }
}
