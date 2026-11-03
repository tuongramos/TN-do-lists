package vn.edu.uth.todo.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.AuthResult
import vn.edu.uth.todo.model.GoogleAuthMessages
import vn.edu.uth.todo.model.FacebookAuthMessages
import vn.edu.uth.todo.model.UserProfile

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val clearGoogleSession: suspend () -> Unit = {},
    private val clearFacebookSession: () -> Unit = {}
) : AuthRepository {
    override val usesGoogleCredentialManager = true
    override val usesFacebookLogin = true

    override suspend fun signIn(provider: AuthProvider): AuthResult = when (provider) {
        AuthProvider.GOOGLE -> AuthResult.Failure(GoogleAuthMessages.INVALID_RESPONSE)
        AuthProvider.FACEBOOK -> AuthResult.Failure(FacebookAuthMessages.INVALID_RESPONSE)
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        if (idToken.isBlank()) return AuthResult.Failure(GoogleAuthMessages.INVALID_RESPONSE)
        return authenticate(GoogleAuthProvider.getCredential(idToken, null), AuthProvider.GOOGLE)
    }

    override suspend fun signInWithFacebookAccessToken(accessToken: String): AuthResult {
        if (accessToken.isBlank()) return AuthResult.Failure(FacebookAuthMessages.INVALID_RESPONSE)
        val result = authenticate(FacebookAuthProvider.getCredential(accessToken), AuthProvider.FACEBOOK)
        if (result is AuthResult.Failure) clearFacebookState()
        return result
    }

    private suspend fun authenticate(credential: AuthCredential, provider: AuthProvider): AuthResult {
        val invalidResponse = if (provider == AuthProvider.GOOGLE) GoogleAuthMessages.INVALID_RESPONSE
            else FacebookAuthMessages.INVALID_RESPONSE
        val invalidCredential = if (provider == AuthProvider.GOOGLE) GoogleAuthMessages.INVALID_CREDENTIAL
            else FacebookAuthMessages.INVALID_CREDENTIAL
        val unexpected = if (provider == AuthProvider.GOOGLE) GoogleAuthMessages.UNEXPECTED
            else FacebookAuthMessages.UNEXPECTED
        return try {
            val user = auth.signInWithCredential(credential).await().user
                ?: return AuthResult.Failure(invalidResponse)
            AuthResult.Success(user.toProfile())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: FirebaseNetworkException) {
            AuthResult.Failure(GoogleAuthMessages.NETWORK)
        } catch (_: FirebaseAuthUserCollisionException) {
            AuthResult.Failure(GoogleAuthMessages.ACCOUNT_CONFLICT)
        } catch (_: FirebaseTooManyRequestsException) {
            AuthResult.Failure(GoogleAuthMessages.TOO_MANY_REQUESTS)
        } catch (error: FirebaseAuthInvalidCredentialsException) {
            Log.w(TAG, "${provider.name} credential rejected: ${error.errorCode}")
            AuthResult.Failure(invalidCredential)
        } catch (error: FirebaseAuthException) {
            Log.w(TAG, "Firebase authentication failed: ${error.errorCode}")
            AuthResult.Failure(if (error.errorCode == "ERROR_OPERATION_NOT_ALLOWED")
                FacebookAuthMessages.PROVIDER_DISABLED else unexpected)
        } catch (_: Exception) {
            AuthResult.Failure(unexpected)
        }
    }

    override fun currentUser(): UserProfile? = auth.currentUser?.toProfile()

    override suspend fun signOut() {
        auth.signOut()
        clearFacebookState()
        // Clearing the provider's account preference must not undo local logout.
        try {
            withTimeoutOrNull(3_000) { clearGoogleSession() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Firebase signed out; Google credential state could not be cleared")
        }
    }

    private fun clearFacebookState() {
        try {
            clearFacebookSession()
        } catch (_: Exception) {
            Log.w(TAG, "Facebook credential state could not be cleared")
        }
    }

    private fun FirebaseUser.toProfile(): UserProfile {
        val provider = when {
            providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } -> AuthProvider.GOOGLE
            providerData.any { it.providerId == FacebookAuthProvider.PROVIDER_ID } -> AuthProvider.FACEBOOK
            else -> null
        }
        return AuthUserMapper.map(uid, displayName, email, provider)
    }

    private companion object { const val TAG = "UTHTodoAuth" }
}
