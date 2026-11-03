package vn.edu.uth.todo.auth

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import vn.edu.uth.todo.model.GoogleAuthMessages
import vn.edu.uth.todo.model.GoogleSignInResult

/** Only application context is retained. The current Activity is passed per request. */
class GoogleCredentialManagerClient(context: Context, private val webClientId: String) {
    private val manager = CredentialManager.create(context.applicationContext)

    suspend fun signIn(activity: Activity): GoogleSignInResult {
        if (webClientId.isBlank()) return GoogleSignInResult.Failure(GoogleAuthMessages.NOT_CONFIGURED)
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = manager.getCredential(MutableContextWrapper(activity), request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return GoogleSignInResult.Failure(GoogleAuthMessages.INVALID_RESPONSE)
            }
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            if (token.isBlank()) GoogleSignInResult.Failure(GoogleAuthMessages.INVALID_RESPONSE)
            else GoogleSignInResult.Token(token)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (cancelled: CancellationException) {
            // Activity recreation restarts the pending UI request in the new composition.
            throw cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failure(GoogleAuthMessages.NO_ACCOUNT)
        } catch (_: GetCredentialProviderConfigurationException) {
            GoogleSignInResult.Failure(GoogleAuthMessages.PROVIDER_UNAVAILABLE)
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(GoogleAuthMessages.INVALID_RESPONSE)
        } catch (_: Exception) {
            GoogleSignInResult.Failure(GoogleAuthMessages.UNEXPECTED)
        }
    }

    suspend fun clearSession() = manager.clearCredentialState(ClearCredentialStateRequest())
}
