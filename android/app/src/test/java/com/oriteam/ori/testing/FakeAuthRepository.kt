package com.oriteam.ori.testing

import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.domain.auth.AuthenticatedUser
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [AuthRepository] that returns [nextResult] and records what it received. */
class FakeAuthRepository(var nextResult: AuthResult = AuthResult.Success) : AuthRepository {
    val signInCalls = mutableListOf<Pair<String, String>>()
    val createAccountCalls = mutableListOf<Triple<String, String, String>>()
    val passwordResetCalls = mutableListOf<String>()

    override val signedInUser = MutableStateFlow<AuthenticatedUser?>(null)

    override fun currentUser(): AuthenticatedUser? = signedInUser.value

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += email to password
        return nextResult
    }

    /** Like Firebase, a successful creation also signs the new account in. */
    override suspend fun createAccount(name: String, email: String, password: String): AuthResult {
        createAccountCalls += Triple(name, email, password)
        if (nextResult == AuthResult.Success) {
            signedInUser.value = AuthenticatedUser(uid = "new-uid", displayName = name)
        }
        return nextResult
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        passwordResetCalls += email
        return nextResult
    }

    override fun signOut() {
        signedInUser.value = null
    }
}
