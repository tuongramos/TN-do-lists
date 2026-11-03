package vn.edu.uth.todo

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import vn.edu.uth.todo.data.AuthRepository
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.*

class AuthUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repository = FigmaTodoRepository()
    private val content = repository.getContent()

    @Test fun actualGuestFlowSupportsProfileBackAndLogout() {
        compose.onNodeWithTag("splash_logo").performClick()
        compose.onNodeWithTag("page_dot_3").performClick()
        compose.onNodeWithTag("next_button").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.onNodeWithTag("guest_button").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithTag("profile_screen").assertIsDisplayed()
        compose.onNodeWithText(content.profile.guestBadge).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("profile_screen").assertIsDisplayed()
        compose.onNodeWithTag("profile_back").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithTag("logout_button").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("login_screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.onNodeWithTag("login_google").assertIsEnabled()
    }

    @Test fun successfulAuthContractAnnouncesSuccessAndDisplaysTheReturnedIdentity() {
        val announcements = mutableListOf<String>()
        val fake = object : AuthRepository {
            override suspend fun signIn(provider: AuthProvider) = AuthResult.Success(content.profile.user.copy(provider = provider))
            override suspend fun signOut() = Unit
        }
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), fake, repository)
        setTestContent { TodoTheme { TodoApp({ announcements += it }, vm) } }
        compose.onNodeWithTag("login_facebook").performClick()
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.runOnIdle { assertTrue(announcements.contains(content.labels.signedIn)) }
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithText("Đã liên kết tài khoản Facebook").assertIsDisplayed()
        compose.onNodeWithTag("logout_button").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.runOnIdle { assertNull(vm.uiState.session) }
    }

    @Test fun cancelledAuthKeepsLoginUsableWithoutAnError() {
        val fake = object : AuthRepository {
            override suspend fun signIn(provider: AuthProvider) = AuthResult.Cancelled
            override suspend fun signOut() = Unit
        }
        val vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "login")), fake, repository)
        setTestContent { TodoTheme { TodoApp({}, vm) } }
        compose.onNodeWithTag("login_google").performClick()
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.onNodeWithTag("login_facebook").assertIsEnabled()
        compose.runOnIdle { assertNull(vm.uiState.notice); assertNull(vm.uiState.session) }
    }

    @Test fun captureLogin() {
        setTestContent { TodoTheme { LoginScreen(content.login, false, {}, {}, {}, {}) } }
        capture("login_screen", "login")
    }

    @Test fun captureHome() {
        setTestContent { TodoTheme { HomeScreen(content.dashboard, content.profile.user.copy(isGuest = true), {}, {}) } }
        capture("home_screen", "home")
    }

    @Test fun captureProfile() {
        setTestContent { TodoTheme { ProfileScreen(content.profile, false, {}, {}, {}) } }
        capture("profile_screen", "profile")
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        compose.runOnUiThread { compose.activity.setContent(content = content) }
    }

    private fun capture(tag: String, name: String) {
        compose.waitForIdle()
        val image = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
