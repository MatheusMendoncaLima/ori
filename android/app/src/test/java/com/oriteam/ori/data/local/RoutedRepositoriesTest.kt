package com.oriteam.ori.data.local

import com.oriteam.ori.domain.aac.AacBoardRepository
import com.oriteam.ori.domain.aac.SavedMessageRepository
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.domain.routine.RoutineRepository
import com.oriteam.ori.domain.routine.RoutineSeriesRepository
import com.oriteam.ori.testing.assertForwardsEveryMember
import com.oriteam.ori.testing.unusedStub
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val ACCOUNT_UID = "account-uid"

/**
 * Each `Routed*` repository sends every operation to the account or to this device, by the owner. Members
 * with a default body in the interface are included on purpose: `getDays` once ran its default here, so
 * reports read one day per query instead of the whole period at once.
 */
class RoutedRepositoriesTest {

    @Test
    fun `every routine operation reaches the account or the device, reading several days included`() = runTest {
        val contract = RoutineRepository::class.java
        assertForwardsEveryMember(contract, ACCOUNT_UID) {
            RoutedRoutineRepository(account = it, local = unusedStub(contract))
        }
        assertForwardsEveryMember(contract, LOCAL_OWNER_UID) {
            RoutedRoutineRepository(account = unusedStub(contract), local = it)
        }
    }

    @Test
    fun `every operation on repeating activities reaches the account or the device`() = runTest {
        val contract = RoutineSeriesRepository::class.java
        assertForwardsEveryMember(contract, ACCOUNT_UID) {
            RoutedRoutineSeriesRepository(account = it, local = unusedStub(contract))
        }
        assertForwardsEveryMember(contract, LOCAL_OWNER_UID) {
            RoutedRoutineSeriesRepository(account = unusedStub(contract), local = it)
        }
    }

    @Test
    fun `every board operation reaches the account or the device`() = runTest {
        val contract = AacBoardRepository::class.java
        assertForwardsEveryMember(contract, ACCOUNT_UID) {
            RoutedAacBoardRepository(account = it, local = unusedStub(contract))
        }
        assertForwardsEveryMember(contract, LOCAL_OWNER_UID) {
            RoutedAacBoardRepository(account = unusedStub(contract), local = it)
        }
    }

    @Test
    fun `every saved message operation reaches the account or the device`() = runTest {
        val contract = SavedMessageRepository::class.java
        assertForwardsEveryMember(contract, ACCOUNT_UID) {
            RoutedSavedMessageRepository(account = it, local = unusedStub(contract))
        }
        assertForwardsEveryMember(contract, LOCAL_OWNER_UID) {
            RoutedSavedMessageRepository(account = unusedStub(contract), local = it)
        }
    }
}
