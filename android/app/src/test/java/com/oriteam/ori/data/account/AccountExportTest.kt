package com.oriteam.ori.data.account

import com.oriteam.ori.data.legal.consentFromText
import com.oriteam.ori.data.legal.toText
import com.oriteam.ori.domain.aac.SavedMessage
import com.oriteam.ori.domain.aac.SymbolColor
import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.accountExportJson
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.legal.LegalConsent
import com.oriteam.ori.domain.local.LocalBoard
import com.oriteam.ori.domain.local.LocalDataSnapshot
import com.oriteam.ori.domain.profile.LOCAL_OWNER_UID
import com.oriteam.ori.testing.FakeConsentRepository
import com.oriteam.ori.testing.FakeDeviceProfileRepository
import com.oriteam.ori.testing.FakeLocalDataStore
import com.oriteam.ori.testing.activity
import com.oriteam.ori.testing.assertForwardsEveryMember
import com.oriteam.ori.testing.board
import com.oriteam.ori.testing.symbol
import com.oriteam.ori.testing.unusedStub
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** The copy of the data (J5): readable JSON, with every kind of data of the device. */
class AccountExportTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `the JSON escapes text and keeps the order of the sections`() {
        val json = accountExportJson(
            linkedMapOf(
                "name" to "Ana \"A\"\nB",
                "count" to 3,
                "done" to true,
                "none" to null,
                "list" to listOf(1, "x")
            )
        )

        assertEquals("""{"name":"Ana \"A\"\nB","count":3,"done":true,"none":null,"list":[1,"x"]}""", json)
    }

    @Test
    fun `a device export has the routine, the boards with their symbols, the phrases and the consent`() = runTest {
        val consents = FakeConsentRepository(mapOf(LOCAL_OWNER_UID to LegalConsent(1, false, true, 0)))
        val data = LocalDataSnapshot(
            tasks = listOf(activity("t1", title = "Tomar remédio")),
            boards = listOf(
                LocalBoard(board("b1", "Básico"), listOf(symbol("s1", "Água").copy(color = SymbolColor.PINK)))
            ),
            savedMessages = listOf(SavedMessage("m1", "Bom dia", emoji = "☀️", position = 4))
        )
        val exporter = LocalAccountDataExporter(
            FakeLocalDataStore(data),
            FakeDeviceProfileRepository(hasLocalProfile = true),
            consents,
            clock
        )

        val json = (exporter.export(LOCAL_OWNER_UID) as DataResult.Success).value

        listOf(
            "Tomar remédio", "Básico", "Água", "pink", "Bom dia", "☀️", "\"position\":4", "\"guardianConsent\":true",
            "2026-11-09"
        )
            .forEach { assertTrue(it, it in json) }
    }

    @Test
    fun `the consent kept on the device reads back, and a broken text is no consent`() {
        val consent = LegalConsent(1, isAdult = false, hasGuardianConsent = true, acceptedAtMillis = 42)

        assertEquals(consent, consentFromText(consent.toText()))
        assertNull(consentFromText("x|true|false|1"))
        assertNull(consentFromText("1|true"))
    }

    @Test
    fun `the export of the account or of the device goes to the right one`() = runTest {
        val contract = AccountDataExporter::class.java
        assertForwardsEveryMember(contract, "account-uid") {
            RoutedAccountDataExporter(account = it, local = unusedStub(contract))
        }
        assertForwardsEveryMember(contract, LOCAL_OWNER_UID) {
            RoutedAccountDataExporter(account = unusedStub(contract), local = it)
        }
    }
}
