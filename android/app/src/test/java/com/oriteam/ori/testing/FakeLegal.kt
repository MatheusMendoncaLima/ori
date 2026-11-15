package com.oriteam.ori.testing

import com.oriteam.ori.domain.account.AccountDataExporter
import com.oriteam.ori.domain.account.AccountDeletion
import com.oriteam.ori.domain.account.AccountDeletionResult
import com.oriteam.ori.domain.account.UserFileWriter
import com.oriteam.ori.domain.common.DataResult
import com.oriteam.ori.domain.common.WriteStatus
import com.oriteam.ori.domain.legal.ConsentRepository
import com.oriteam.ori.domain.legal.LegalConsent

class FakeConsentRepository(var stored: Map<String, LegalConsent> = emptyMap()) : ConsentRepository {
    var readResult: DataResult<LegalConsent?>? = null
    var writeResult: DataResult<WriteStatus> = DataResult.Success(WriteStatus.SYNCED)

    override suspend fun getConsent(ownerUid: String) = readResult ?: DataResult.Success(stored[ownerUid])

    override suspend fun recordConsent(ownerUid: String, consent: LegalConsent): DataResult<WriteStatus> {
        if (writeResult is DataResult.Success) stored = stored + (ownerUid to consent)
        return writeResult
    }

    override suspend fun clearConsent(ownerUid: String): DataResult<WriteStatus> {
        stored = stored - ownerUid
        return DataResult.Success(WriteStatus.SYNCED)
    }
}

class FakeAccountDataExporter(var result: DataResult<String> = DataResult.Success("{}")) : AccountDataExporter {
    val exported = mutableListOf<String>()

    override suspend fun export(ownerUid: String): DataResult<String> {
        exported += ownerUid
        return result
    }
}

class FakeAccountDeletion(var result: AccountDeletionResult = AccountDeletionResult.Deleted) : AccountDeletion {
    val deleted = mutableListOf<String>()

    override suspend fun deleteAccount(ownerUid: String): AccountDeletionResult {
        deleted += ownerUid
        return result
    }
}

class FakeUserFileWriter(var succeeds: Boolean = true) : UserFileWriter {
    val written = mutableMapOf<String, String>()

    override suspend fun write(uri: String, text: String): Boolean {
        if (succeeds) written[uri] = text
        return succeeds
    }
}
