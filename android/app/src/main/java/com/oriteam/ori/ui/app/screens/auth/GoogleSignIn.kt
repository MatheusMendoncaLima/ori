package com.oriteam.ori.ui.app.screens.auth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.oriteam.ori.R
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.form.LoadingButton
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.LocalOriColors
import com.oriteam.ori.ui.theme.OriSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignIn"

/** What asking Android for the Google account gave: a token, nothing (the person closed the sheet) or a reason. */
sealed interface GoogleCredentialResult {
    data class Token(val idToken: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data class Failed(@StringRes val messageRes: Int) : GoogleCredentialResult
}

/**
 * The web client id that Firebase made when Google sign-in was turned on; the google-services plugin writes it as
 * `default_web_client_id`. The dev build uses the emulator project, which has none: there the button is not shown.
 */
@SuppressLint("DiscouragedApi") // The resource exists only in builds whose Firebase project has Google sign-in.
private fun Context.googleWebClientId(): String? =
    resources.getIdentifier("default_web_client_id", "string", packageName)
        .takeIf { it != 0 }
        ?.let(::getString)

/**
 * Asks Android for a Google account with the Credential Manager. The person chooses one in the system sheet; the
 * app never sees a password. Only the reason of a failure is logged, never account data.
 */
private suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleCredentialResult {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        val isGoogle = credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        if (credential is CustomCredential && isGoogle) {
            GoogleCredentialResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleCredentialResult.Failed(R.string.error_unknown)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: GetCredentialCancellationException) {
        GoogleCredentialResult.Cancelled
    } catch (_: NoCredentialException) {
        GoogleCredentialResult.Failed(R.string.auth_google_no_account)
    } catch (exception: GetCredentialException) {
        Log.w(TAG, "Google credential failed: ${exception.type}")
        GoogleCredentialResult.Failed(R.string.error_unknown)
    }
}

/**
 * "ou" and "Continuar com o Google", under the e-mail form of the login and of the registration. A new account goes
 * on to "Complete seu perfil", as one made with e-mail does; the session (OriApp) changes the screen by itself.
 */
@Composable
fun GoogleSignInSection() {
    val context = LocalContext.current
    // Checked before the ViewModel exists: without Google sign-in nothing of it is created.
    val webClientId = remember(context) { context.googleWebClientId() } ?: return
    GoogleSignInButton(webClientId)
}

@Composable
private fun GoogleSignInButton(webClientId: String, viewModel: GoogleSignInViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Spacer(modifier = Modifier.height(OriSpacing.Medium))
    Text(
        text = stringResource(R.string.auth_or),
        style = MaterialTheme.typography.bodySmall,
        color = LocalOriColors.current.subtleText,
        textAlign = TextAlign.Center,
        modifier = Modifier
    )
    Spacer(modifier = Modifier.height(OriSpacing.Small))
    LoadingButton(
        text = stringResource(R.string.auth_google_continue),
        isLoading = state.isLoading,
        onClick = {
            if (!state.isLoading) {
                viewModel.onRequestStarted()
                scope.launch { viewModel.onCredential(requestGoogleIdToken(context, webClientId)) }
            }
        }
    )
    FormMessage(messageRes = state.errorMessageRes, type = FormMessageType.ERROR)
}
