package vn.edu.uth.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import vn.edu.uth.todo.auth.GoogleCredentialManagerClient
import vn.edu.uth.todo.auth.FacebookSignInClient
import com.facebook.login.LoginManager
import vn.edu.uth.todo.data.FirebaseAuthRepository
import vn.edu.uth.todo.data.DeviceNotificationSettings
import vn.edu.uth.todo.model.AppScreen
import vn.edu.uth.todo.model.HomeAction
import vn.edu.uth.todo.model.GoogleAuthMessages
import vn.edu.uth.todo.model.GoogleSignInResult
import vn.edu.uth.todo.model.FacebookAuthMessages
import vn.edu.uth.todo.model.FacebookSignInRequest
import vn.edu.uth.todo.model.FacebookSignInResult
import vn.edu.uth.todo.ui.HomeScreen
import vn.edu.uth.todo.ui.LoginScreen
import vn.edu.uth.todo.ui.NotificationPermissionDialog
import vn.edu.uth.todo.ui.ProfileScreen
import vn.edu.uth.todo.ui.OnboardingScreen
import vn.edu.uth.todo.ui.TodoViewModel
import vn.edu.uth.todo.ui.StartScreen
import vn.edu.uth.todo.ui.TodoTheme
import vn.edu.uth.todo.ui.TaskListScreen
import vn.edu.uth.todo.ui.TaskDetailScreen
import vn.edu.uth.todo.ui.TaskDetailUnavailableScreen
import vn.edu.uth.todo.ui.TaskStatusBanner
import vn.edu.uth.todo.data.ApiFirebaseTaskRepository
import vn.edu.uth.todo.data.MockTaskApi
import vn.edu.uth.todo.data.TaskApiConfiguration
import vn.edu.uth.todo.data.FirebaseTaskStore
import vn.edu.uth.todo.data.TaskFirebaseDatabase
import vn.edu.uth.todo.data.SharedPreferencesTaskCache
import vn.edu.uth.todo.model.TaskDataMessages
import vn.edu.uth.todo.model.TaskFilter
import vn.edu.uth.todo.model.TaskCategory

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
        val tasks = ApiFirebaseTaskRepository(MockTaskApi(TaskApiConfiguration(BuildConfig.TASKS_API_TOKEN)),
            FirebaseTaskStore { TaskFirebaseDatabase.instance(BuildConfig.FIREBASE_DATABASE_URL) },
            SharedPreferencesTaskCache(applicationContext))
        val factory = TodoViewModel.factory(authRepository, sync = tasks,
            notifications = DeviceNotificationSettings(applicationContext))
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
    val context = LocalContext.current
    var pendingNotificationOwner by rememberSaveable { mutableStateOf<String?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val owner = pendingNotificationOwner
        pendingNotificationOwner = null
        if (owner != null) viewModel.completeNotificationOptIn(owner, granted)
    }
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
    Box(Modifier.fillMaxSize()) {
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
            HomeScreen(viewModel.dashboard, user, viewModel::openProfile, viewModel::selectNavigation,
                viewModel::openTask, viewModel::toggleTask,
                { category ->
                    viewModel.filterTasks(if (category == TaskCategory.STUDY) TaskFilter.STUDY else TaskFilter.LIFE)
                    viewModel.selectNavigation(HomeAction.LIST)
                }, if (state.taskStatus.loading) TaskDataMessages.LOADING else TaskDataMessages.EMPTY)
        }
        AppScreen.Profile -> state.session?.let { user ->
            ProfileScreen(viewModel.profileFor(user), state.loading || state.taskStatus.saving,
                viewModel::previous, viewModel::signOut, viewModel::pendingFeature)
        }
        AppScreen.TaskList -> TaskListScreen(viewModel.taskContent.list, state.taskBoard,
            viewModel.content.dashboard.navigation, viewModel::searchTasks, viewModel::filterTasks,
            viewModel::openTask, viewModel::toggleTask, viewModel::selectNavigation,
            state.taskStatus, viewModel.taskChangesEnabled, viewModel::refreshTasks)
        is AppScreen.TaskDetail -> {
          val task = state.taskBoard.find(screen.taskId)
          if (task != null && !state.detailStatus.notFound) {
            TaskDetailScreen(task, viewModel.taskContent.detail, viewModel::previous,
                { viewModel.toggleTask(task.id) }, { viewModel.toggleSubtask(task.id, it) },
                { viewModel.addSubtask(task.id, it) }, { title, notes -> viewModel.editTask(task.id, title, notes) },
                { viewModel.deleteTask(task.id) }, { id, title -> viewModel.editSubtask(task.id, id, title) },
                { viewModel.deleteSubtask(task.id, it) }, viewModel.detailChangesEnabled)
          } else TaskDetailUnavailableScreen(viewModel.taskContent.detail.back,
              state.detailStatus.warning ?: TaskDataMessages.forFailure(vn.edu.uth.todo.model.TaskFailure.NOT_FOUND),
              viewModel::previous, viewModel::refreshTaskDetail)
        }
      }
      if ((state.screen == AppScreen.Home || state.screen == AppScreen.TaskList ||
          state.screen is AppScreen.TaskDetail || state.screen == AppScreen.Profile) &&
          (state.screen != AppScreen.TaskList || state.taskBoard.items.isNotEmpty()) &&
          !(state.screen is AppScreen.TaskDetail && state.detailStatus.notFound)) {
          val status = when {
              state.taskStatus.saving -> state.taskStatus
              state.screen is AppScreen.TaskDetail && state.detailStatus.loading -> state.taskStatus.copy(loading = true, warning = null)
              state.screen is AppScreen.TaskDetail && state.detailStatus.warning != null -> state.taskStatus.copy(loading = false, warning = state.detailStatus.warning)
              else -> state.taskStatus
          }
          TaskStatusBanner(status, if (state.screen is AppScreen.TaskDetail) viewModel::refreshTaskDetail else viewModel::refreshTasks)
      }
    }
    if (state.showNotificationPrompt) {
        NotificationPermissionDialog(
            onAllow = allow@{
                if (pendingNotificationOwner != null) return@allow
                val owner = viewModel.beginNotificationOptIn() ?: return@allow
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingNotificationOwner = owner
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.completeNotificationOptIn(owner, granted = false)
                }
            },
            onLater = viewModel::dismissNotificationPrompt
        )
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
