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
import kotlinx.coroutines.Job
import java.util.UUID
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

class TodoViewModel(
    private val savedState: SavedStateHandle,
    private val authRepository: AuthRepository,
    repository: TodoRepository,
    taskRepository: TaskRepository = FigmaTaskRepository(),
    private val taskSyncRepository: TaskSyncRepository? = null
) : ViewModel() {
    constructor(savedState: SavedStateHandle) : this(savedState, UnconfiguredAuthRepository(), FigmaTodoRepository())

    val onboarding = FigmaOnboardingRepository().getContent()
    val content = repository.getContent()
    val taskContent = taskRepository.getContent()
    private val navigator = OnboardingNavigator(onboarding.pages.size)
    private val restoredGuest = if (savedState.get<Boolean>("guest") == true)
        content.profile.user.copy(id = "guest", provider = null, isGuest = true) else null
    private val restoredUser = authRepository.currentUser()
    private val restoredSession = restoredUser ?: restoredGuest
    private val restoredOwner = restoredSession?.let { if (it.isGuest) "guest" else it.id }
    private val restoredBoard = if (taskSyncRepository == null) {
        savedState.get<TaskBoard>("task_board") ?: TaskBoard(taskContent.tasks)
    } else if (restoredOwner != null && savedState.get<String>("task_owner") == restoredOwner) {
        savedState.get<TaskBoard>("task_board") ?: TaskBoard(emptyList())
    } else TaskBoard(emptyList())
    private var providerRequestSequence = 0L
    private var taskGeneration = 0L
    private var taskReadJob: Job? = null
    private var taskObserveJob: Job? = null
    private var detailReadJob: Job? = null
    private var detailGeneration = 0L
    private var pendingTaskSnapshot: List<TaskItem>? = null

    var uiState by mutableStateOf(TodoUiState(
        screen = if (restoredUser != null) {
            restoreProtectedScreen()
        } else when (savedState.get<String>("screen")) {
            "onboarding" -> navigator.open(savedState["page"] ?: 0)
            "login" -> AppScreen.Login
            "home" -> if (restoredGuest != null) AppScreen.Home else AppScreen.Login
            "profile" -> if (restoredGuest != null) AppScreen.Profile else AppScreen.Login
            "tasks", "task_detail" -> if (restoredGuest != null) restoreProtectedScreen() else AppScreen.Login
            else -> AppScreen.Start
        },
        session = restoredUser ?: restoredGuest,
        taskBoard = restoredBoard
    ))
        private set

    init {
        if (taskSyncRepository != null && uiState.session != null) {
            refreshTasks()
            if (uiState.screen is AppScreen.TaskDetail) refreshTaskDetail()
        }
    }

    val dashboard: DashboardContent get() {
        val statistics = TaskStatisticsCalculator.calculate(uiState.taskBoard.items)
        return content.dashboard.copy(
            date = TaskDates.heading(statistics.date),
            progressTitle = if (statistics.isToday) content.dashboard.progressTitle else "Tiến độ ngày ${TaskDates.shortDate(statistics.date)}",
            todayTitle = if (statistics.isToday) content.dashboard.todayTitle else "Việc ngày ${TaskDates.shortDate(statistics.date)}",
            progress = DailyProgress(statistics.completedInDay, statistics.dayTasks.size),
            encouragement = when {
                statistics.dayTasks.isEmpty() -> "Bắt đầu từ một công việc nhỏ nhé."
                statistics.completedInDay == statistics.dayTasks.size -> "Bạn đã hoàn thành công việc trong ngày!"
                statistics.completedInDay * 2 >= statistics.dayTasks.size -> "Đã hơn nửa chặng, cố lên!"
                else -> "Bắt đầu từ việc nhỏ nhất nhé."
            },
            categories = content.dashboard.categories.map { category -> category.copy(remaining =
                if (category.category == TaskCategory.STUDY) statistics.remainingStudy else statistics.remainingLife) },
            tasks = statistics.dayTasks.map(TaskItem::toSummary)
        )
    }

    fun profileFor(user: UserProfile): ProfileContent {
        val statistics = TaskStatisticsCalculator.calculate(uiState.taskBoard.items)
        return content.profile.copy(user = user, statistics = listOf(
            ProfileStatistic(statistics.completedTotal.toString(), "Việc đã xong"),
            ProfileStatistic(statistics.remainingTotal.toString(), "Đang chờ"),
            ProfileStatistic("${statistics.completionPercent}%", "Hoàn thành")
        ), week = statistics.week, weeklyProgress = "${statistics.week.sumOf { it.completed }} việc")
    }

    val taskChangesEnabled: Boolean get() = uiState.session != null && (taskSyncRepository == null ||
        (!uiState.taskStatus.loading && !uiState.taskStatus.saving &&
            (uiState.session?.isGuest == true || uiState.taskStatus.cloudWritable)))

    val detailChangesEnabled: Boolean get() = taskChangesEnabled && (taskSyncRepository == null ||
        (uiState.detailStatus.loaded && !uiState.detailStatus.loading &&
            !uiState.detailStatus.notFound && uiState.detailStatus.warning == null))

    fun refreshTaskDetail() {
        val repository = taskSyncRepository ?: return
        val screen = uiState.screen as? AppScreen.TaskDetail ?: return
        val user = uiState.session ?: return
        if (uiState.taskStatus.saving) return
        detailReadJob?.cancel()
        val ticket = ++detailGeneration
        val uid = if (user.isGuest) null else user.id
        uiState = uiState.copy(detailStatus = TaskDetailStatus(screen.taskId, loading = true))
        detailReadJob = viewModelScope.launch {
            try {
                val result = repository.detail(uid, screen.taskId)
                if (ticket != detailGeneration || uiState.screen != screen) return@launch
                updateBoard { it.update(screen.taskId) { result.task } }
                uiState = uiState.copy(detailStatus = TaskDetailStatus(screen.taskId, loaded = true, warning = result.warning),
                    taskStatus = uiState.taskStatus.copy(source = result.source, cloudWritable = result.cloudWritable,
                        warning = result.warning))
                if (uid != null && result.cloudWritable && taskObserveJob?.isActive != true) observeTasks(uid, taskGeneration)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (ticket == detailGeneration && uiState.screen == screen)
                    uiState = uiState.copy(detailStatus = TaskDetailStatus(screen.taskId,
                        warning = taskError(error), notFound = (error as? TaskDataException)?.reason == TaskFailure.NOT_FOUND))
            }
        }
    }

    fun refreshTasks() {
        val repository = taskSyncRepository ?: return
        val user = uiState.session ?: return
        if (uiState.taskStatus.saving) return
        val uid = if (user.isGuest) null else user.id
        val owner = uid ?: "guest"
        val sameOwner = savedState.get<String>("task_owner") == owner
        taskReadJob?.cancel()
        taskObserveJob?.cancel()
        val ticket = ++taskGeneration
        val cached = repository.cached(uid)
        val board = if (sameOwner) uiState.taskBoard.copy(items = cached ?: uiState.taskBoard.items)
            else TaskBoard(cached.orEmpty())
        savedState["task_owner"] = owner
        uiState = uiState.copy(taskBoard = board, taskStatus = TaskDataStatus(loading = true,
            loaded = cached != null, source = if (cached == null) TaskSource.LOCAL else TaskSource.CACHE))
        taskReadJob = viewModelScope.launch {
            try {
                val snapshot = repository.load(uid)
                if (ticket != taskGeneration) return@launch
                updateBoard { it.copy(items = snapshot.tasks) }
                uiState = uiState.copy(taskStatus = TaskDataStatus(loaded = true,
                    cloudWritable = snapshot.cloudWritable, source = snapshot.source, warning = snapshot.warning))
                if (uid != null && snapshot.cloudWritable) observeTasks(uid, ticket)
                keepSelectedTaskValid()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (ticket == taskGeneration) uiState = uiState.copy(taskStatus = uiState.taskStatus.copy(
                    loading = false, warning = taskError(error)))
            }
        }
    }

    private fun observeTasks(uid: String, ticket: Long) {
        val repository = taskSyncRepository ?: return
        taskObserveJob = viewModelScope.launch {
            try {
                repository.observe(uid).collect { tasks ->
                    if (ticket == taskGeneration) {
                        // RTDB emits local writes before the server ACK. Keep the current screen/data
                        // until save/delete succeeds, especially when deleting the selected task.
                        if (uiState.taskStatus.saving) { pendingTaskSnapshot = tasks; return@collect }
                        repository.cache(uid, tasks)
                        updateBoard { it.copy(items = tasks) }
                        uiState = uiState.copy(taskStatus = uiState.taskStatus.copy(loaded = true,
                            source = TaskSource.FIREBASE, cloudWritable = true))
                        keepSelectedTaskValid()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (ticket == taskGeneration) uiState = uiState.copy(taskStatus = uiState.taskStatus.copy(
                    warning = taskError(error), cloudWritable = false))
            }
        }
    }

    private fun keepSelectedTaskValid() {
        val screen = uiState.screen as? AppScreen.TaskDetail ?: return
        if (uiState.taskStatus.loaded && uiState.taskBoard.find(screen.taskId) == null) navigate(AppScreen.TaskList)
    }

    private fun taskError(error: Exception): String =
        (error as? TaskDataException)?.message ?: TaskDataMessages.forFailure(TaskFailure.UNKNOWN)

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
            AppScreen.TaskList -> AppScreen.Home
            is AppScreen.TaskDetail -> AppScreen.TaskList
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
                refreshTasks()
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
        refreshTasks()
    }

    fun openProfile() { if (uiState.session != null) navigate(AppScreen.Profile) }

    fun selectNavigation(action: HomeAction) {
        if (uiState.session == null) return
        when (action) {
            HomeAction.HOME -> { navigate(AppScreen.Home); refreshTasks() }
            HomeAction.LIST -> {
                navigate(AppScreen.TaskList)
                if (!uiState.taskStatus.loading) refreshTasks()
            }
            HomeAction.PROFILE -> openProfile()
            else -> pendingFeature()
        }
    }

    fun openTask(id: String) {
        if (uiState.session != null && !uiState.taskStatus.saving && uiState.taskBoard.find(id) != null) {
            navigate(AppScreen.TaskDetail(id))
            refreshTaskDetail()
        }
    }

    fun searchTasks(query: String) = updateBoard { it.copy(query = query) }
    fun filterTasks(filter: TaskFilter) = updateBoard { it.copy(filter = filter) }
    fun toggleTask(id: String) = changeTask(id) { it.copy(completed = !it.completed) }

    fun deleteTask(id: String) {
        persistTask(id, null)
    }

    fun editTask(id: String, title: String, notes: String) {
        if (title.isBlank()) return
        changeTask(id) { task -> task.copy(title = title.trim(), notes = notes.trim()) }
    }

    fun toggleSubtask(taskId: String, subtaskId: String) = changeSubtasks(taskId) { subtasks ->
        subtasks.map { if (it.id == subtaskId) it.copy(completed = !it.completed) else it }
    }

    fun addSubtask(taskId: String, title: String) {
        if (title.isBlank()) return
        changeSubtasks(taskId) { it + Subtask(UUID.randomUUID().toString(), title.trim()) }
    }

    fun editSubtask(taskId: String, subtaskId: String, title: String) {
        if (title.isBlank()) return
        changeSubtasks(taskId) { items -> items.map { if (it.id == subtaskId) it.copy(title = title.trim()) else it } }
    }

    fun deleteSubtask(taskId: String, subtaskId: String) =
        changeSubtasks(taskId) { items -> items.filterNot { it.id == subtaskId } }

    private fun changeSubtasks(taskId: String, change: (List<Subtask>) -> List<Subtask>) =
        changeTask(taskId) { task -> task.copy(subtasks = change(task.subtasks)) }

    private fun changeTask(id: String, change: (TaskItem) -> TaskItem) {
        uiState.taskBoard.find(id)?.let { persistTask(id, change(it)) }
    }

    private fun persistTask(id: String, task: TaskItem?) {
        if (uiState.taskBoard.find(id) == null) return
        val user = uiState.session ?: return
        val repository = taskSyncRepository
        if (repository != null && (uiState.taskStatus.loading || uiState.taskStatus.saving)) return
        if (repository != null && uiState.screen is AppScreen.TaskDetail && !detailChangesEnabled) return
        fun applyChange() {
            updateBoard { board -> if (task == null) board.copy(items = board.items.filterNot { it.id == id })
                else board.update(id) { task } }
            repository?.cache(if (user.isGuest) null else user.id, uiState.taskBoard.items)
            keepSelectedTaskValid()
            if (task == null && (uiState.screen as? AppScreen.TaskDetail)?.taskId == id) navigate(AppScreen.TaskList)
        }
        if (repository == null) { applyChange(); return }
        if (!user.isGuest && !uiState.taskStatus.cloudWritable) {
            uiState = uiState.copy(notice = UiNotice(TaskDataMessages.SAVE_ERROR,
                uiState.taskStatus.warning ?: TaskDataMessages.forFailure(TaskFailure.NETWORK)))
            return
        }
        val ticket = taskGeneration
        pendingTaskSnapshot = null
        uiState = uiState.copy(taskStatus = uiState.taskStatus.copy(saving = true, warning = null))
        viewModelScope.launch {
            try {
                if (user.isGuest) {
                    if (task == null) repository.deleteGuest(id) else repository.saveGuest(task)
                } else {
                    if (task == null) repository.delete(user.id, id) else repository.save(user.id, task)
                }
                if (ticket == taskGeneration) {
                    pendingTaskSnapshot?.let { tasks -> updateBoard { it.copy(items = tasks) } }
                    pendingTaskSnapshot = null
                    applyChange()
                    uiState = uiState.copy(taskStatus = uiState.taskStatus.copy(saving = false, warning = null))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (ticket == taskGeneration) {
                  pendingTaskSnapshot = null
                  uiState = uiState.copy(
                    taskStatus = uiState.taskStatus.copy(saving = false, warning = taskError(error),
                        cloudWritable = uiState.taskStatus.cloudWritable &&
                            (error as? TaskDataException)?.reason != TaskFailure.PERMISSION),
                    notice = UiNotice(TaskDataMessages.SAVE_ERROR, taskError(error)))
                }
            }
        }
    }

    private fun updateBoard(change: (TaskBoard) -> TaskBoard) {
        val board = change(uiState.taskBoard)
        uiState = uiState.copy(taskBoard = board)
        savedState["task_board"] = board
    }

    private fun restoreProtectedScreen(): AppScreen = when (savedState.get<String>("screen")) {
        "profile" -> AppScreen.Profile
        "tasks" -> AppScreen.TaskList
        "task_detail" -> savedState.get<String>("task_id")?.takeIf { restoredBoard.find(it) != null }
            ?.let { AppScreen.TaskDetail(it) } ?: AppScreen.TaskList
        else -> AppScreen.Home
    }

    fun signOut() {
        if (uiState.loading || uiState.taskStatus.saving) return
        uiState = uiState.copy(loading = true)
        viewModelScope.launch {
            try {
                authRepository.signOut()
                ++taskGeneration
                taskReadJob?.cancel()
                taskObserveJob?.cancel()
                savedState["guest"] = false
                uiState = TodoUiState(screen = AppScreen.Login, announcement = content.labels.signedOut,
                    taskBoard = if (taskSyncRepository == null) uiState.taskBoard else TaskBoard(emptyList()))
                if (taskSyncRepository != null) {
                    savedState.remove<TaskBoard>("task_board")
                    savedState.remove<String>("task_owner")
                }
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
        if (screen !is AppScreen.TaskDetail) {
            ++detailGeneration
            detailReadJob?.cancel()
            uiState = uiState.copy(detailStatus = TaskDetailStatus())
        }
        uiState = uiState.copy(screen = screen)
        savedState["screen"] = when (screen) {
            AppScreen.Start -> "start"
            is AppScreen.Onboarding -> "onboarding"
            AppScreen.Login -> "login"
            AppScreen.Home -> "home"
            AppScreen.Profile -> "profile"
            AppScreen.TaskList -> "tasks"
            is AppScreen.TaskDetail -> "task_detail"
        }
        savedState["page"] = (screen as? AppScreen.Onboarding)?.pageIndex
        savedState["task_id"] = (screen as? AppScreen.TaskDetail)?.taskId
    }

    companion object {
        fun factory(auth: AuthRepository, repository: TodoRepository = FigmaTodoRepository(),
            sync: TaskSyncRepository? = null): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TodoViewModel(createSavedStateHandle(), auth, repository, taskSyncRepository = sync) }
            }
    }
}
