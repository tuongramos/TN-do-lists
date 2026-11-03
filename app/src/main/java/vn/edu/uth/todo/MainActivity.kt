package vn.edu.uth.todo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import vn.edu.uth.todo.auth.GoogleCredentialManagerClient
import vn.edu.uth.todo.auth.FacebookSignInClient
import com.facebook.login.LoginManager
import vn.edu.uth.todo.data.FirebaseAuthRepository
import vn.edu.uth.todo.model.AppScreen
import vn.edu.uth.todo.model.HomeAction
import vn.edu.uth.todo.model.GoogleAuthMessages
import vn.edu.uth.todo.model.GoogleSignInResult
import vn.edu.uth.todo.model.FacebookAuthMessages
import vn.edu.uth.todo.model.FacebookSignInRequest
import vn.edu.uth.todo.model.FacebookSignInResult
import vn.edu.uth.todo.ui.HomeScreen
import vn.edu.uth.todo.ui.LoginScreen
import vn.edu.uth.todo.ui.ProfileScreen
import vn.edu.uth.todo.ui.OnboardingScreen
import vn.edu.uth.todo.ui.TodoViewModel
import vn.edu.uth.todo.ui.StartScreen
import vn.edu.uth.todo.ui.TodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        val googleClient = GoogleCredentialManagerClient(applicationContext, getString(R.string.default_web_client_id))
        val authRepository = FirebaseAuthRepository(
            clearGoogleSession = googleClient::clearSession,
            clearFacebookSession = { LoginManager.getInstance().logOut() }
        )
        val factory = TodoViewModel.factory(authRepository)
        val todoViewModel = ViewModelProvider(this, factory)[TodoViewModel::class.java]
        val facebookClient = FacebookSignInClient(this, todoViewModel.uiState.facebookSignInRequest?.id,
            todoViewModel::completeFacebookSignIn)
        setContent {
            TodoTheme {
                TodoApp(
                    onFinished = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() },
                    viewModel = todoViewModel,
                    onGoogleSignIn = { googleClient.signIn(this@MainActivity) },
                    onFacebookSignIn = facebookClient::launch
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun TodoApp(
    onFinished: (String) -> Unit,
    viewModel: TodoViewModel = viewModel(),
    onGoogleSignIn: suspend () -> GoogleSignInResult = {
        GoogleSignInResult.Failure(GoogleAuthMessages.NOT_CONFIGURED)
    },
    onFacebookSignIn: ((FacebookSignInRequest) -> Unit)? = null
) {
    val state = viewModel.uiState
    LaunchedEffect(state.screen) {
        if (state.screen == AppScreen.Start) {
            delay(2_400)
            viewModel.next()
        }
    }
    LaunchedEffect(state.announcement) {
        state.announcement?.let {
            onFinished(it)
            viewModel.consumeAnnouncement()
        }
    }
    LaunchedEffect(state.googleSignInRequest?.id) {
        state.googleSignInRequest?.let { request ->
            val result = try {
                onGoogleSignIn()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                GoogleSignInResult.Failure(GoogleAuthMessages.UNEXPECTED)
            }
            viewModel.completeGoogleSignIn(request.id, result)
        }
    }
    LaunchedEffect(state.facebookSignInRequest?.id) {
        state.facebookSignInRequest?.let { request ->
            try {
                if (onFacebookSignIn != null) onFacebookSignIn(request)
                else viewModel.completeFacebookSignIn(request.id,
                    FacebookSignInResult.Failure(FacebookAuthMessages.NOT_CONFIGURED))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewModel.completeFacebookSignIn(request.id,
                    FacebookSignInResult.Failure(FacebookAuthMessages.UNEXPECTED))
            }
        }
    }
    BackHandler(enabled = state.screen != AppScreen.Start && state.screen != AppScreen.Home) { viewModel.previous() }
    when (val screen = state.screen) {
        AppScreen.Start -> StartScreen(viewModel.onboarding.brand, viewModel::next)
        is AppScreen.Onboarding -> OnboardingScreen(
            content = viewModel.onboarding,
            pageIndex = screen.pageIndex,
            onNext = viewModel::next,
            onSkip = {
                if (screen.pageIndex == viewModel.onboarding.pages.lastIndex) {
                    viewModel.completeOnboarding()
                } else {
                    viewModel.skip()
                }
            },
            onPageSelected = viewModel::openPage,
            onComplete = viewModel::completeOnboarding
        )
        AppScreen.Login -> LoginScreen(viewModel.content.login, state.loading,
            viewModel::signIn, viewModel::tryAsGuest, viewModel::previous, viewModel::pendingFeature)
        AppScreen.Home -> state.session?.let { user ->
            HomeScreen(viewModel.content.dashboard, user, viewModel::openProfile) { action ->
                when (action) {
                    HomeAction.HOME -> Unit
                    HomeAction.PROFILE -> viewModel.openProfile()
                    else -> viewModel.pendingFeature()
                }
            }
        }
        AppScreen.Profile -> state.session?.let { user ->
            ProfileScreen(viewModel.content.profile.copy(user = user), state.loading,
                viewModel::previous, viewModel::signOut, viewModel::pendingFeature)
        }
    }
    state.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            title = { Text(notice.title) },
            text = { Text(notice.message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) { Text(viewModel.content.labels.acknowledge) }
            }
        )
    }
}
