package vn.edu.uth.todo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

class TodoViewModel(
    private val savedState: SavedStateHandle,
    private val authRepository: AuthRepository,
    repository: TodoRepository
) : ViewModel() {
    constructor(savedState: SavedStateHandle) : this(savedState, UnconfiguredAuthRepository(), FigmaTodoRepository())

    val onboarding = FigmaOnboardingRepository().getContent()
    val content = repository.getContent()
    private val navigator = OnboardingNavigator(onboarding.pages.size)
    private val restoredGuest = if (savedState.get<Boolean>("guest") == true)
        content.profile.user.copy(id = "guest", provider = null, isGuest = true) else null
    private val restoredUser = authRepository.currentUser()
    private var providerRequestSequence = 0L

    var uiState by mutableStateOf(TodoUiState(
        screen = if (restoredUser != null) {
            if (savedState.get<String>("screen") == "profile") AppScreen.Profile else AppScreen.Home
        } else when (savedState.get<String>("screen")) {
            "onboarding" -> navigator.open(savedState["page"] ?: 0)
            "login" -> AppScreen.Login
            "home" -> if (restoredGuest != null) AppScreen.Home else AppScreen.Login
            "profile" -> if (restoredGuest != null) AppScreen.Profile else AppScreen.Login
            else -> AppScreen.Start
        },
        session = restoredUser ?: restoredGuest
    ))
        private set

    fun next() = navigate(navigator.next(uiState.screen))
    fun openPage(index: Int) = navigate(navigator.open(index))
    fun skip() = navigate(navigator.skip())
    fun completeOnboarding() = navigate(AppScreen.Login)

    fun previous() {
        if (uiState.loading) return
        navigate(when (uiState.screen) {
            AppScreen.Profile -> AppScreen.Home
            AppScreen.Login -> navigator.skip()
            AppScreen.Home -> AppScreen.Home
            else -> navigator.previous(uiState.screen)
        })
    }

    fun signIn(provider: AuthProvider) {
        if (uiState.loading || uiState.screen != AppScreen.Login) return
        uiState = uiState.copy(loading = true, notice = null)
        if (provider == AuthProvider.GOOGLE && authRepository.usesGoogleCredentialManager) {
            uiState = uiState.copy(googleSignInRequest = GoogleSignInRequest(++providerRequestSequence))
            return
        }
        if (provider == AuthProvider.FACEBOOK && authRepository.usesFacebookLogin) {
            uiState = uiState.copy(facebookSignInRequest = FacebookSignInRequest(++providerRequestSequence))
            return
        }
        viewModelScope.launch {
            applyAuthResult(safeAuth { authRepository.signIn(provider) })
        }
    }

    fun completeFacebookSignIn(requestId: Long, result: FacebookSignInResult) {
        if (uiState.facebookSignInRequest?.id != requestId || uiState.screen != AppScreen.Login) return
        uiState = uiState.copy(facebookSignInRequest = null)
        when (result) {
            is FacebookSignInResult.Token -> viewModelScope.launch {
                applyAuthResult(safeAuth { authRepository.signInWithFacebookAccessToken(result.accessToken) })
            }
            is FacebookSignInResult.Failure -> applyAuthResult(AuthResult.Failure(result.message))
            FacebookSignInResult.Cancelled -> applyAuthResult(AuthResult.Cancelled)
        }
    }

    fun completeGoogleSignIn(requestId: Long, result: GoogleSignInResult) {
        if (uiState.googleSignInRequest?.id != requestId || uiState.screen != AppScreen.Login) return
        // Claim the result once; stale callbacks and duplicate tokens are ignored.
        uiState = uiState.copy(googleSignInRequest = null)
        when (result) {
            is GoogleSignInResult.Token -> viewModelScope.launch {
                applyAuthResult(safeAuth { authRepository.signInWithGoogleIdToken(result.idToken) })
            }
            is GoogleSignInResult.Failure -> applyAuthResult(AuthResult.Failure(result.message))
            GoogleSignInResult.Cancelled -> applyAuthResult(AuthResult.Cancelled)
        }
    }

    private suspend fun safeAuth(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (cancelled: CancellationException) {
        uiState = uiState.copy(loading = false, googleSignInRequest = null, facebookSignInRequest = null)
        throw cancelled
    } catch (_: Exception) {
        AuthResult.Failure(content.labels.unexpectedError)
    }

    private fun applyAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                savedState["guest"] = false
                uiState = uiState.copy(session = result.user, loading = false, notice = null,
                    googleSignInRequest = null, facebookSignInRequest = null, announcement = content.labels.signedIn)
                navigate(AppScreen.Home)
            }
            is AuthResult.Failure -> uiState = uiState.copy(loading = false,
                googleSignInRequest = null, facebookSignInRequest = null,
                notice = UiNotice(content.labels.loginErrorTitle, result.message))
            AuthResult.Cancelled -> uiState = uiState.copy(loading = false,
                googleSignInRequest = null, facebookSignInRequest = null)
        }
    }

    fun tryAsGuest() {
        if (uiState.loading) return
        savedState["guest"] = true
        uiState = uiState.copy(session = content.profile.user.copy(id = "guest", provider = null, isGuest = true),
            notice = null, announcement = content.labels.guestMessage)
        navigate(AppScreen.Home)
    }

    fun openProfile() { if (uiState.session != null) navigate(AppScreen.Profile) }

    fun signOut() {
        if (uiState.loading) return
        uiState = uiState.copy(loading = true)
        viewModelScope.launch {
            try {
                authRepository.signOut()
                savedState["guest"] = false
                uiState = TodoUiState(screen = AppScreen.Login, announcement = content.labels.signedOut)
                navigate(AppScreen.Login)
            } catch (cancelled: CancellationException) {
                uiState = uiState.copy(loading = false)
                throw cancelled
            } catch (_: Exception) {
                uiState = uiState.copy(loading = false,
                    notice = UiNotice(content.labels.loginErrorTitle, content.labels.unexpectedError))
            }
        }
    }

    fun dismissNotice() { uiState = uiState.copy(notice = null) }
    fun consumeAnnouncement() { uiState = uiState.copy(announcement = null) }
    fun pendingFeature() { uiState = uiState.copy(announcement = content.labels.featurePending) }

    private fun navigate(screen: AppScreen) {
        uiState = uiState.copy(screen = screen)
        savedState["screen"] = when (screen) {
            AppScreen.Start -> "start"
            is AppScreen.Onboarding -> "onboarding"
            AppScreen.Login -> "login"
            AppScreen.Home -> "home"
            AppScreen.Profile -> "profile"
        }
        savedState["page"] = (screen as? AppScreen.Onboarding)?.pageIndex
    }

    companion object {
        fun factory(auth: AuthRepository, repository: TodoRepository = FigmaTodoRepository()): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TodoViewModel(createSavedStateHandle(), auth, repository) }
            }
    }
}
