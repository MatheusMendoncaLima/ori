package com.oriteam.ori.data.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.oriteam.ori.domain.auth.AuthError
import com.oriteam.ori.domain.auth.AuthRepository
import com.oriteam.ori.domain.auth.AuthResult
import com.oriteam.ori.domain.auth.AuthenticatedUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "FirebaseAuthRepository"

/** [AuthRepository] backed by Firebase Authentication (e-mail and password). */
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val signedInUser: Flow<AuthenticatedUser?> = callbackFlow {
        // The SDK calls the listener immediately with the restored session (or null).
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.toDomain()) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged { old, new -> old?.uid == new?.uid }

    override fun currentUser(): AuthenticatedUser? = firebaseAuth.currentUser?.toDomain()

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun signIn(email: String, password: String): AuthResult = runAuthOperation {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun createAccount(name: String, email: String, password: String): AuthResult {
        val creation = runAuthOperation {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        }
        val user = firebaseAuth.currentUser
        return when {
            creation is AuthResult.Failure -> creation
            user == null -> AuthResult.Failure(AuthError.UNKNOWN)
            else -> updateDisplayName(user, name)
        }
    }

    private suspend fun updateDisplayName(user: FirebaseUser, name: String): AuthResult {
        val profileUpdate = runAuthOperation {
            val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(request).await()
        }
        return if (profileUpdate is AuthResult.Failure) {
            AuthResult.Failure(AuthError.PROFILE_UPDATE_FAILED)
        } else {
            AuthResult.Success
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        val result = runAuthOperation { firebaseAuth.sendPasswordResetEmail(email).await() }
        // An unknown address is reported as success so this screen cannot be used to discover accounts.
        return if (result == AuthResult.Failure(AuthError.INVALID_CREDENTIALS)) AuthResult.Success else result
    }

    private fun FirebaseUser.toDomain() = AuthenticatedUser(uid = uid, displayName = displayName)

    /**
     * Converts SDK exceptions into [AuthResult]. Only error codes and class names are logged:
     * exception messages may contain the user's e-mail.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runAuthOperation(operation: suspend () -> Unit): AuthResult = try {
        operation()
        AuthResult.Success
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: FirebaseAuthException) {
        Log.w(TAG, "Auth operation failed with code ${exception.errorCode}")
        AuthResult.Failure(authErrorFromFirebaseCode(exception.errorCode))
    } catch (exception: FirebaseNetworkException) {
        Log.w(TAG, "Auth operation failed: ${exception.javaClass.simpleName}")
        AuthResult.Failure(AuthError.NETWORK_UNAVAILABLE)
    } catch (exception: FirebaseTooManyRequestsException) {
        Log.w(TAG, "Auth operation failed: ${exception.javaClass.simpleName}")
        AuthResult.Failure(AuthError.TOO_MANY_REQUESTS)
    } catch (exception: Exception) {
        // Any other SDK failure must still reach the screen as an error instead of crashing the app.
        Log.w(TAG, "Unexpected auth failure: ${exception.javaClass.simpleName}")
        AuthResult.Failure(AuthError.UNKNOWN)
    }
}
