package vn.edu.uth.todo

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.*

class TaskSyncUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val sample: List<TaskItem> get() {
        val json = InstrumentationRegistry.getInstrumentation().context.assets.open("tasks-api-sample.json").bufferedReader().use { it.readText() }
        return MockTaskApi(TaskApiConfiguration("test")).decode(json)
    }
    private val userA = UserProfile("user-a", "Nguyễn A", "a@example.test", "NA", AuthProvider.GOOGLE)
    private val userB = UserProfile("user-b", "Nguyễn B", "b@example.test", "NB", AuthProvider.GOOGLE)

    private class Auth(var user: UserProfile?, var next: UserProfile? = null) : AuthRepository {
        override fun currentUser() = user
        override suspend fun signIn(provider: AuthProvider): AuthResult = AuthResult.Success(requireNotNull(next).also { user = it })
        override suspend fun signOut() { user = null }
    }

    private class Repository(private val initial: List<TaskItem>) : TaskSyncRepository {
        val owners = mutableListOf<String?>()
        val cache = mutableMapOf<String, List<TaskItem>>()
        val flows = mutableMapOf<String, MutableStateFlow<List<TaskItem>>>()
        val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
        var loadFailure: TaskDataException? = null
        var saveFailure: TaskDataException? = null
        var saveGate: CompletableDeferred<Unit>? = null
        val saved = mutableListOf<Pair<String, TaskItem>>()
        fun state(uid: String) = flows.getOrPut(uid) { MutableStateFlow(initial) }
        override fun cached(uid: String?) = cache[uid ?: "guest"]
        override fun cache(uid: String?, tasks: List<TaskItem>) { cache[uid ?: "guest"] = tasks }
        override suspend fun load(uid: String?): TaskSnapshot {
            owners += uid
            gates[uid ?: "guest"]?.await()
            loadFailure?.let { throw it }
            val items = state(uid ?: "guest").value
            cache(uid, items)
            return TaskSnapshot(items, if (uid == null) TaskSource.API else TaskSource.FIREBASE, uid != null)
        }
        override fun observe(uid: String): Flow<List<TaskItem>> = state(uid)
        override suspend fun save(uid: String, task: TaskItem) {
            saved += uid to task
            saveGate?.await()
            saveFailure?.let { throw it }
            state(uid).value = state(uid).value.map { if (it.id == task.id) task else it }
        }
        override suspend fun delete(uid: String, id: String) {
            state(uid).value = state(uid).value.filterNot { it.id == id }
        }
    }

    private fun show(repository: Repository, auth: Auth = Auth(userA), guest: Boolean = false,
        screen: String = "home"): TodoViewModel {
        lateinit var vm: TodoViewModel
        compose.runOnUiThread {
            vm = TodoViewModel(SavedStateHandle(mapOf("screen" to screen, "guest" to guest)), auth,
                FigmaTodoRepository(), taskSyncRepository = repository)
            compose.activity.setContent { TodoTheme { TodoApp({}, vm) } }
        }
        return vm
    }

    @Test fun loadingThenRealDataRendersTwoOfFourAndPersistsCheckboxChanges() {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository(sample).apply { gates["user-a"] = gate }
        val vm = show(repository)
        compose.onNodeWithTag("task_data_status").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.uiState.taskStatus.loading); assertTrue(vm.uiState.taskBoard.items.isEmpty()); gate.complete(Unit) }
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()
        val saveGate = CompletableDeferred<Unit>()
        repository.saveGate = saveGate
        compose.onNodeWithTag("home_toggle_2").performClick()
        compose.onNodeWithText(TaskDataMessages.SAVING).assertIsDisplayed()
        compose.runOnIdle { saveGate.complete(Unit) }
        compose.onNodeWithText("3/4 việc").assertIsDisplayed()
        compose.runOnIdle { assertEquals("user-a", repository.saved.single().first); assertTrue(repository.saved.single().second.completed) }
    }

    @Test fun realtimeUpdatesChangeHomeAndProfileStatistics() {
        val repository = Repository(sample)
        val vm = show(repository)
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        compose.runOnIdle { repository.state("user-a").value = sample.map { if (it.id == "2") it.copy(completed = true) else it } }
        compose.onNodeWithText("3/4 việc").assertIsDisplayed()
        compose.onNodeWithTag("home_avatar").performClick()
        compose.onNodeWithText("50%").assertIsDisplayed()
        compose.runOnIdle { assertEquals(3, vm.profileFor(userA).statistics.first().value.toInt()) }
    }

    @Test fun serverFailureShowsRetryWithoutFakeTasksAndRetryLoadsData() {
        val repository = Repository(sample).apply { loadFailure = TaskDataException(TaskFailure.SERVER) }
        val vm = show(repository)
        compose.onNodeWithTag("task_retry").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.uiState.taskBoard.items.isEmpty()); repository.loadFailure = null }
        compose.onNodeWithTag("task_retry").performClick()
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        compose.onNodeWithTag("task_retry").assertDoesNotExist()
    }

    @Test fun rejectedWriteDoesNotPretendTheTaskWasSaved() {
        val repository = Repository(sample).apply { saveFailure = TaskDataException(TaskFailure.PERMISSION) }
        val vm = show(repository)
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        compose.onNodeWithTag("home_toggle_2").performClick()
        compose.onNodeWithText(TaskDataMessages.SAVE_ERROR).assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(vm.uiState.taskBoard.find("2")!!.completed)
            assertFalse(vm.uiState.taskStatus.saving)
            assertFalse(vm.uiState.taskStatus.cloudWritable)
            vm.dismissNotice()
            repository.saveFailure = null
        }
        compose.onNodeWithTag("task_retry").performClick()
        compose.runOnIdle { assertTrue(vm.uiState.taskStatus.cloudWritable) }
    }

    @Test fun guestUsesApiAndLocalCacheWithoutCloudWrites() {
        val repository = Repository(sample)
        val vm = show(repository, Auth(null), guest = true)
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        compose.onNodeWithTag("home_toggle_2").performClick()
        compose.onNodeWithText("3/4 việc").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(repository.owners.all { it == null })
            assertTrue(repository.saved.isEmpty())
            assertTrue(repository.cache.getValue("guest").first { it.id == "2" }.completed)
            assertTrue(vm.uiState.session!!.isGuest)
        }
    }

    @Test fun signOutAndAccountSwitchCannotShowPreviousOwnersTasks() {
        val repository = Repository(sample)
        val auth = Auth(userA, userB)
        val vm = show(repository, auth)
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        val nextGate = CompletableDeferred<Unit>()
        repository.gates["user-b"] = nextGate
        repository.state("user-b").value = listOf(sample.first().copy(title = "Công việc riêng B"))
        compose.runOnIdle { vm.signOut() }
        compose.onNodeWithTag("login_screen").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.uiState.taskBoard.items.isEmpty()); vm.signIn(AuthProvider.GOOGLE) }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(vm.uiState.taskBoard.items.isEmpty())
            repository.state("user-a").value = sample.map { it.copy(title = "Dữ liệu cũ A") }
            nextGate.complete(Unit)
        }
        compose.onNodeWithText("Công việc riêng B").assertIsDisplayed()
        compose.onNodeWithText("Dữ liệu cũ A").assertDoesNotExist()
    }

    @Test fun emptyServerCollectionIsRenderedAsZeroOfZero() {
        val vm = show(Repository(emptyList()))
        compose.onNodeWithText("0/0 việc").assertIsDisplayed()
        compose.onNodeWithTag("home_empty").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.uiState.taskStatus.loaded); assertTrue(vm.uiState.taskBoard.items.isEmpty()) }
    }

    @Test fun listUsesApiIdsAndOpensTheSelectedObject() {
        val vm = show(Repository(sample), screen = "tasks")
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
        for (id in listOf("2", "3", "6", "5")) compose.onNodeWithTag("task_toggle_$id").assertIsOff()
        for (id in listOf("4", "1")) compose.onNodeWithTag("task_toggle_$id").assertIsOn()
        compose.onNodeWithTag("list_task_mobile").assertDoesNotExist()
        compose.onNodeWithTag("list_task_3").performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(AppScreen.TaskDetail("3"), vm.uiState.screen)
            assertEquals(sample.first { it.id == "3" }, vm.uiState.taskBoard.find("3"))
        }
    }

    @Test fun listDistinguishesLoadingErrorRetryAndEmptyCollection() {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository(sample).apply {
            gates["user-a"] = gate
            loadFailure = TaskDataException(TaskFailure.SERVER)
        }
        show(repository, screen = "tasks")
        compose.onNodeWithTag("task_list_loading").assertIsDisplayed()
        compose.onNodeWithTag("task_list_empty").assertDoesNotExist()
        compose.onNodeWithTag("list_task_1").assertDoesNotExist()
        compose.runOnIdle { gate.complete(Unit) }
        compose.onNodeWithTag("task_list_error").assertIsDisplayed()
        compose.onNodeWithTag("task_retry").assertIsDisplayed()
        compose.runOnIdle {
            repository.loadFailure = null
            repository.state("user-a").value = emptyList()
        }
        compose.onNodeWithTag("task_retry").performClick()
        compose.onNodeWithTag("task_list_empty").assertIsDisplayed()
        compose.onNodeWithText(TaskListMessages.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag("task_list_no_results").assertDoesNotExist()
    }

    @Test fun listCombinesSearchAndCategoryAndClearsBothWithoutAnotherRequest() {
        val repository = Repository(sample)
        show(repository, screen = "tasks")
        compose.onNodeWithTag("task_filter_life").performClick()
        compose.onNodeWithTag("task_search").performTextInput("DI CHO")
        compose.onNodeWithTag("task_search").performImeAction()
        compose.onNodeWithTag("list_task_3").assertIsDisplayed()
        compose.onNodeWithTag("list_task_6").assertDoesNotExist()
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("task_filter_study").performClick()
        compose.onNodeWithTag("task_list_no_results").assertIsDisplayed()
        compose.onNodeWithTag("task_list_clear_filters").performClick()
        compose.onNodeWithTag("task_filter_all").assertIsSelected()
        compose.onNodeWithTag("task_search").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("list_task_2").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repository.owners.size) }
    }

    @Test fun openingListRefreshesDataAndKeepsTheSearchAndFilterOnDetailReturn() {
        val repository = Repository(sample)
        val vm = show(repository)
        compose.onNodeWithText("2/4 việc").assertIsDisplayed()
        val gate = CompletableDeferred<Unit>()
        compose.runOnIdle { repository.gates["user-a"] = gate }
        compose.onNodeWithTag("nav_list").performClick()
        compose.onNodeWithTag("task_toggle_2").assertIsNotEnabled()
        compose.onNodeWithTag("task_filter_study").performClick()
        compose.onNodeWithTag("task_search").performTextInput("CAU TRUC")
        compose.onNodeWithTag("task_search").performImeAction()
        compose.runOnIdle {
            repository.state("user-a").value = sample.map { if (it.id == "2") it.copy(title = "Ôn cấu trúc mới") else it }
            gate.complete(Unit)
        }
        compose.onNodeWithText("Ôn cấu trúc mới").assertIsDisplayed()
        compose.onNodeWithTag("task_toggle_2").assertIsEnabled()
        compose.onNodeWithTag("list_task_2").performClick()
        compose.onNodeWithTag("task_detail_back").performClick()
        compose.onNodeWithTag("task_filter_study").assertIsSelected()
        compose.onNodeWithTag("task_search").assertTextEquals("CAU TRUC")
        compose.runOnIdle {
            assertEquals(2, repository.owners.size)
            assertEquals("CAU TRUC", vm.uiState.taskBoard.query)
        }
    }

    @Test fun listWaitsForSaveThenMovesTheTaskAndUpdatesHomeStatistics() {
        val repository = Repository(sample).apply { saveGate = CompletableDeferred() }
        show(repository, screen = "tasks")
        compose.onNodeWithTag("task_toggle_2").performClick()
        compose.onNodeWithText(TaskDataMessages.SAVING).assertIsDisplayed()
        compose.onNodeWithTag("task_toggle_3").assertIsNotEnabled()
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
        compose.runOnIdle { repository.saveGate!!.complete(Unit) }
        compose.onNodeWithText("3 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("task_toggle_2").assertIsOn()
        compose.onNodeWithTag("tasks_nav_home").performClick()
        compose.onNodeWithText("3/4 việc").assertIsDisplayed()
        compose.onNodeWithTag("nav_list").performClick()
        compose.onNodeWithTag("task_toggle_2").performClick()
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("task_toggle_2").assertIsOff()
        compose.runOnIdle { assertEquals(listOf("user-a", "user-a"), repository.saved.map { it.first }) }
    }

    @Test fun failedListWriteKeepsTheOriginalTaskAndRetryEnablesEditingAgain() {
        val repository = Repository(sample).apply { saveFailure = TaskDataException(TaskFailure.PERMISSION) }
        val vm = show(repository, screen = "tasks")
        compose.onNodeWithTag("task_toggle_2").performClick()
        compose.onNodeWithText(TaskDataMessages.SAVE_ERROR).assertIsDisplayed()
        compose.runOnIdle { vm.dismissNotice(); repository.saveFailure = null }
        compose.onNodeWithTag("task_toggle_2").assertIsOff().assertIsNotEnabled()
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("task_retry").performClick()
        compose.onNodeWithTag("task_toggle_2").assertIsEnabled()
    }

    @Test fun longListScrollsAndChangingCategoryReturnsToTheFirstMatch() {
        val tasks = (0..29).map { index -> sample[index % sample.size].copy(id = "extra_$index",
            title = "Công việc số $index", completed = false,
            category = if (index % 2 == 0) TaskCategory.STUDY else TaskCategory.LIFE, sortOrder = index) }
        show(Repository(tasks), screen = "tasks")
        compose.onNodeWithTag("list_task_extra_29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("tasks_nav_home").assertIsDisplayed()
        compose.onNodeWithTag("task_filter_life").performClick()
        compose.onNodeWithTag("list_task_extra_1").assertIsDisplayed()
        compose.onNodeWithTag("list_task_extra_0").assertDoesNotExist()
        compose.onNodeWithText("30 việc chưa xong").assertIsDisplayed()
    }
}
