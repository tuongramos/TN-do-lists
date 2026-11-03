package vn.edu.uth.todo.data

import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.AuthResult
import vn.edu.uth.todo.model.GoogleAuthMessages
import vn.edu.uth.todo.model.FacebookAuthMessages
import vn.edu.uth.todo.model.UserProfile

/** Provider UI obtains tokens; the repository verifies them and owns the Firebase session. */
interface AuthRepository {
    val usesGoogleCredentialManager: Boolean get() = false
    val usesFacebookLogin: Boolean get() = false
    suspend fun signIn(provider: AuthProvider): AuthResult
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult =
        AuthResult.Failure(GoogleAuthMessages.NOT_CONFIGURED)
    suspend fun signInWithFacebookAccessToken(accessToken: String): AuthResult =
        AuthResult.Failure(FacebookAuthMessages.NOT_CONFIGURED)
    fun currentUser(): UserProfile? = null
    suspend fun signOut()
}

/** The UI build must never pretend that a provider authenticated a real user. */
class UnconfiguredAuthRepository : AuthRepository {
    override suspend fun signIn(provider: AuthProvider): AuthResult = AuthResult.Failure(
        "Chưa cấu hình Firebase Authentication cho ${provider.displayName}. " +
            "Bạn có thể chọn “Dùng thử không cần tài khoản” để xem trang chủ và hồ sơ."
    )

    override suspend fun signOut() = Unit
}
