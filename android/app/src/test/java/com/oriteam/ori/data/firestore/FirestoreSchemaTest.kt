package com.oriteam.ori.data.firestore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.oriteam.ori.data.routine.routineTaskFrom
import com.oriteam.ori.domain.carelink.CareLinkPermissions
import com.oriteam.ori.domain.carelink.CareLinkStatus
import com.oriteam.ori.domain.common.DataError
import com.oriteam.ori.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/** Robolectric: the Firebase error code enums need Android classes to initialize. */
@RunWith(AndroidJUnit4::class)
class FirestoreSchemaTest {

    @Test
    fun `a profile needs a name, and the role of older profiles is ignored`() {
        assertEquals("Cara", profileFrom("u", mapOf("displayName" to "Cara"))?.displayName)
        assertEquals("Cara", profileFrom("u", mapOf("role" to "caregiver", "displayName" to "Cara"))?.displayName)
        assertNull(profileFrom("u", mapOf("role" to "primary_user")))
    }

    @Test
    fun `activities are read with date, time, status and author`() {
        val task = routineTaskFrom(
            id = "t1",
            ownerUid = "pat",
            data = mapOf(
                "title" to "Café",
                "date" to "2026-10-29",
                "scheduledTime" to "07:30",
                "status" to "hidden_from_caregiver",
                "ownerCanComplete" to true,
                "isDone" to true,
                "createdBy" to "pat"
            )
        )

        assertEquals(LocalDate.of(2026, 10, 29), task?.date)
        assertEquals(LocalTime.of(7, 30), task?.scheduledTime)
        assertEquals(TaskStatus.HIDDEN_FROM_CAREGIVER, task?.status)
        assertEquals(true, task?.isCreatedByOwner)
    }

    @Test
    fun `activities with missing or malformed required fields are skipped`() {
        val valid = mapOf("title" to "Café", "date" to "2026-10-29", "status" to "active", "createdBy" to "pat")

        assertNull(routineTaskFrom("t", "pat", valid - "date"))
        assertNull(routineTaskFrom("t", "pat", valid + ("date" to "29/10/2026")))
        assertNull(routineTaskFrom("t", "pat", valid + ("status" to "unknown")))
        assertNull(routineTaskFrom("t", "pat", valid - "createdBy"))
        // A malformed optional time does not hide the activity; it just has no time.
        assertNull(routineTaskFrom("t", "pat", valid + ("scheduledTime" to "7h30"))?.scheduledTime)
    }

    @Test
    fun `missing link permissions mean no permission`() {
        val link = careLinkFrom(
            id = "cara_pat",
            data = mapOf("caregiverUid" to "cara", "primaryUserUid" to "pat", "status" to "active")
        )

        assertEquals(CareLinkStatus.ACTIVE, link?.status)
        assertEquals(CareLinkPermissions(viewRoutines = false, manageRoutines = false), link?.permissions)
        assertNull(careLinkFrom("x", mapOf("caregiverUid" to "cara", "status" to "active")))
    }

    @Test
    fun `stored values match the strings used by firestore rules`() {
        assertEquals("cara_pat", FirestoreSchema.linkId("cara", "pat"))
    }

    @Test
    fun `permission errors are never reported as other failures`() {
        assertEquals(DataError.PERMISSION_DENIED, dataErrorFor(FirebaseFirestoreException.Code.PERMISSION_DENIED))
        assertEquals(DataError.NETWORK_UNAVAILABLE, dataErrorFor(FirebaseFirestoreException.Code.UNAVAILABLE))
        assertEquals(DataError.CONFLICT, dataErrorFor(FirebaseFunctionsException.Code.ALREADY_EXISTS))
        assertEquals(DataError.PRECONDITION_FAILED, dataErrorFor(FirebaseFunctionsException.Code.FAILED_PRECONDITION))
        assertEquals(DataError.UNKNOWN, dataErrorFor(FirebaseFirestoreException.Code.INTERNAL))
    }
}
