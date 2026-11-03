package vn.edu.uth.todo

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.data.AuthRepository
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.data.AuthUserMapper
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.TodoTheme
import vn.edu.uth.todo.ui.TodoViewModel

class GoogleSignInUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val content = FigmaTodoRepository().getContent()
    private val googleUser = AuthUserMapper.map("real-user-uid", "Nguyễn Văn A", "student@example.test", AuthProvider.GOOGLE)

    private inner class FakeGoogleRepository(
        var response: AuthResult = AuthResult.Success(googleUser),
        var cachedUser: UserProfile? = null
    ) : AuthRepository {
        override val usesGoogleCredentialManager = true
        val tokens = mutableListOf<String>()
        override fun currentUser() = cachedUser
        override suspend fun signIn(provider: AuthProvider): AuthResult = error("Google must use the token exchange")
        override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
            tokens += idToken
            if (response is AuthResult.Success) cachedUser = (response as AuthResult.Success).user
            return response
        }
        override suspend fun signOut() { cachedUser = null }
    }

    @Test fun googleTokenExchangeOpensHomeAndShowsActualProfileThenLogsOut() {
        val auth = FakeGoogleRepository()
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())
        val announcements = mutableListOf<String>()
        setTestContent { TodoTheme {
            TodoApp({ announcements += it }, vm, onGoogleSignIn = { GoogleSignInResult.Token("google-test-token") })
        } }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("google-test-token"), auth.tokens)
            assertEquals(googleUser, vm.uiState.session)
            assertTrue(announcements.contains(content.labels.signedIn))
        }
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithText(googleUser.displayName).assertIsDisplayed()
        compose.onNodeWithText(googleUser.email).assertIsDisplayed()
        compose.onNodeWithText("Đã liên kết tài khoản Google").assertIsDisplayed()
        compose.onNodeWithTag("logout_button").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.runOnIdle { assertNull(auth.cachedUser); assertNull(vm.uiState.session) }
    }

    @Test fun cancellingTheGooglePickerClearsLoadingAndAllowsRetry() {
        val auth = FakeGoogleRepository()
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())
        var attempts = 0
        setTestContent { TodoTheme {
            TodoApp({}, vm, onGoogleSignIn = {
                if (++attempts == 1) GoogleSignInResult.Cancelled else GoogleSignInResult.Token("retry-token")
            })
        } }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithTag("login_google").assertIsEnabled()
        compose.runOnIdle {
            assertNull(vm.uiState.notice)
            assertNull(vm.uiState.googleSignInRequest)
            assertFalse(vm.uiState.loading)
            assertTrue(auth.tokens.isEmpty())
        }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test fun credentialManagerErrorDoesNotCallFirebaseAndCanBeDismissed() {
        val auth = FakeGoogleRepository()
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())
        setTestContent { TodoTheme {
            TodoApp({}, vm, onGoogleSignIn = { GoogleSignInResult.Failure(GoogleAuthMessages.NO_ACCOUNT) })
        } }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithText(GoogleAuthMessages.NO_ACCOUNT).assertIsDisplayed()
        compose.onNodeWithText(content.labels.acknowledge).performClick()
        compose.onNodeWithTag("login_google").assertIsEnabled()
        compose.runOnIdle { assertNull(vm.uiState.session); assertTrue(auth.tokens.isEmpty()) }
    }

    @Test fun firebaseErrorAfterReceivingATokenDoesNotAuthenticate() {
        val auth = FakeGoogleRepository(AuthResult.Failure(GoogleAuthMessages.NETWORK))
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())
        setTestContent { TodoTheme {
            TodoApp({}, vm, onGoogleSignIn = { GoogleSignInResult.Token("test-token") })
        } }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithText(GoogleAuthMessages.NETWORK).assertIsDisplayed()
        compose.runOnIdle { assertNull(vm.uiState.session); assertFalse(vm.uiState.loading) }
    }

    @Test fun persistedFirebaseIdentityTakesPrecedenceOverDemoAndRestoresHome() {
        val auth = FakeGoogleRepository(cachedUser = googleUser)
        val saved = SavedStateHandle(mapOf("screen" to "login", "guest" to true))
        val vm = TodoViewModel(saved, auth, FigmaTodoRepository())
        setTestContent { TodoTheme { TodoApp({}, vm) } }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.runOnIdle { assertEquals(googleUser, vm.uiState.session) }
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithTag("logout_button").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.runOnIdle {
            val restored = TodoViewModel(saved, auth, FigmaTodoRepository())
            assertNull(restored.uiState.session)
            assertEquals(AppScreen.Login, restored.uiState.screen)
        }
    }

    @Test fun duplicateAndStaleGoogleResultsCannotCreateExtraSessions() {
        val auth = FakeGoogleRepository()
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())
        compose.runOnUiThread {
            vm.signIn(AuthProvider.GOOGLE)
            val request = requireNotNull(vm.uiState.googleSignInRequest)
            vm.signIn(AuthProvider.GOOGLE)
            assertEquals(request, vm.uiState.googleSignInRequest)
            vm.completeGoogleSignIn(request.id + 1, GoogleSignInResult.Token("stale-token"))
            assertEquals(request, vm.uiState.googleSignInRequest)
            vm.completeGoogleSignIn(request.id, GoogleSignInResult.Token("accepted-token"))
            vm.completeGoogleSignIn(request.id, GoogleSignInResult.Token("duplicate-token"))
        }
        compose.runOnIdle { assertEquals(listOf("accepted-token"), auth.tokens) }
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        compose.runOnUiThread { compose.activity.setContent(content = content) }
    }
}
