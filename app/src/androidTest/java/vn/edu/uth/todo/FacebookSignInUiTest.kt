package vn.edu.uth.todo

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.data.AuthRepository
import vn.edu.uth.todo.data.AuthUserMapper
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.TodoTheme
import vn.edu.uth.todo.ui.TodoViewModel

class FacebookSignInUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val content = FigmaTodoRepository().getContent()
    private val facebookUser = AuthUserMapper.map("facebook-firebase-uid", "Nguyễn Minh Bình",
        "student@example.test", AuthProvider.FACEBOOK)

    private inner class FakeFacebookRepository(
        var response: AuthResult = AuthResult.Success(facebookUser),
        var cachedUser: UserProfile? = null,
        val gate: CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        override val usesGoogleCredentialManager = true
        override val usesFacebookLogin = true
        val tokens = mutableListOf<String>()
        override fun currentUser() = cachedUser
        override suspend fun signIn(provider: AuthProvider): AuthResult = error("Use provider token exchange")
        override suspend fun signInWithFacebookAccessToken(accessToken: String): AuthResult {
            tokens += accessToken
            gate?.await()
            if (response is AuthResult.Success) cachedUser = (response as AuthResult.Success).user
            return response
        }
        override suspend fun signOut() { cachedUser = null }
    }

    private fun viewModel(auth: AuthRepository) = TodoViewModel(
        SavedStateHandle(mapOf("screen" to "login")), auth, FigmaTodoRepository())

    @Test fun facebookTokenExchangeShowsActualIdentityAndLogsOut() {
        val auth = FakeFacebookRepository()
        val vm = viewModel(auth)
        val announcements = mutableListOf<String>()
        setTestContent { TodoTheme {
            TodoApp({ announcements += it }, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("facebook-test-token"))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithText(facebookUser.displayName).assertIsDisplayed()
        compose.onNodeWithText(facebookUser.email).assertIsDisplayed()
        compose.onNodeWithText("Đã liên kết tài khoản Facebook").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("facebook-test-token"), auth.tokens)
            assertTrue(announcements.contains(content.labels.signedIn))
        }
        compose.onNodeWithTag("logout_button").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.runOnIdle { assertNull(auth.cachedUser); assertNull(vm.uiState.session) }
    }

    @Test fun cancelledFacebookLoginCanBeRetriedWithoutError() {
        val auth = FakeFacebookRepository()
        val vm = viewModel(auth)
        var attempts = 0
        setTestContent { TodoTheme {
            TodoApp({}, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, if (++attempts == 1) FacebookSignInResult.Cancelled
                    else FacebookSignInResult.Token("facebook-retry-token"))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("login_google").assertIsEnabled()
        compose.onNodeWithTag("login_facebook").assertIsEnabled()
        compose.runOnIdle { assertNull(vm.uiState.notice); assertTrue(auth.tokens.isEmpty()) }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test fun facebookWithoutEmailStillOpensHomeAndShowsAnHonestProfile() {
        val user = AuthUserMapper.map("facebook-no-email-uid", "Nguyễn Minh Bình", null, AuthProvider.FACEBOOK)
        val auth = FakeFacebookRepository(AuthResult.Success(user))
        val vm = viewModel(auth)
        setTestContent { TodoTheme {
            TodoApp({}, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("profile-only-test-token"))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithText(user.displayName).assertIsDisplayed()
        compose.onNodeWithText(content.profile.missingFacebookEmail).assertIsDisplayed()
        compose.onNodeWithText(content.profile.user.email).assertDoesNotExist()
        compose.runOnIdle { assertEquals("", vm.uiState.session?.email); assertFalse(vm.uiState.session!!.isGuest) }
    }

    @Test fun sdkErrorDoesNotSendATokenToFirebase() {
        val auth = FakeFacebookRepository()
        val vm = viewModel(auth)
        setTestContent { TodoTheme {
            TodoApp({}, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, FacebookSignInResult.Failure(FacebookAuthMessages.UNEXPECTED))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithText(FacebookAuthMessages.UNEXPECTED).assertIsDisplayed()
        compose.onNodeWithText(content.labels.acknowledge).performClick()
        compose.onNodeWithTag("login_facebook").assertIsEnabled()
        compose.runOnIdle { assertTrue(auth.tokens.isEmpty()); assertNull(vm.uiState.session) }
    }

    @Test fun firebaseAccountConflictKeepsLoginAndAllowsRetry() {
        val auth = FakeFacebookRepository(AuthResult.Failure(GoogleAuthMessages.ACCOUNT_CONFLICT))
        val vm = viewModel(auth)
        setTestContent { TodoTheme {
            TodoApp({}, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("conflict-test-token"))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithText(GoogleAuthMessages.ACCOUNT_CONFLICT).assertIsDisplayed()
        compose.runOnIdle { assertNull(vm.uiState.session); assertFalse(vm.uiState.loading) }
    }

    @Test fun firebaseExchangeMustFinishBeforeOpeningHomeAndCannotOverlapGoogle() {
        val gate = CompletableDeferred<Unit>()
        val auth = FakeFacebookRepository(gate = gate)
        val vm = viewModel(auth)
        setTestContent { TodoTheme {
            TodoApp({}, vm, onFacebookSignIn = { request ->
                vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("pending-test-token"))
            })
        } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.onNodeWithTag("login_google").assertIsNotEnabled()
        compose.runOnIdle {
            vm.signIn(AuthProvider.GOOGLE)
            assertNull(vm.uiState.googleSignInRequest)
            assertNull(vm.uiState.session)
            assertTrue(vm.uiState.loading)
            gate.complete(Unit)
        }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test fun staleAndDuplicateFacebookCallbacksAreIgnored() {
        val auth = FakeFacebookRepository()
        val vm = viewModel(auth)
        compose.runOnUiThread {
            vm.signIn(AuthProvider.FACEBOOK)
            val request = requireNotNull(vm.uiState.facebookSignInRequest)
            vm.signIn(AuthProvider.GOOGLE)
            assertNull(vm.uiState.googleSignInRequest)
            vm.completeFacebookSignIn(request.id + 1, FacebookSignInResult.Token("stale-token"))
            assertEquals(request, vm.uiState.facebookSignInRequest)
            vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("accepted-token"))
            vm.completeFacebookSignIn(request.id, FacebookSignInResult.Token("duplicate-token"))
        }
        compose.runOnIdle { assertEquals(listOf("accepted-token"), auth.tokens) }
    }

    @Test fun facebookSessionRestoresProfileInNewViewModel() {
        val auth = FakeFacebookRepository(cachedUser = facebookUser)
        val saved = SavedStateHandle(mapOf("screen" to "profile"))
        val vm = TodoViewModel(saved, auth, FigmaTodoRepository())
        setTestContent { TodoTheme { TodoApp({}, vm) } }
        compose.onNodeWithText(facebookUser.email).assertIsDisplayed()
        compose.runOnIdle {
            val restored = TodoViewModel(saved, auth, FigmaTodoRepository())
            assertEquals(AppScreen.Profile, restored.uiState.screen)
            assertEquals(facebookUser, restored.uiState.session)
        }
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        compose.runOnUiThread { compose.activity.setContent(content = content) }
    }
}
