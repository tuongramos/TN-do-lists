package vn.edu.uth.todo

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.*

class TaskDetailFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val sample get() = InstrumentationRegistry.getInstrumentation().context.assets
        .open("tasks-api-sample.json").bufferedReader().use { MockTaskApi(TaskApiConfiguration("test")).decode(it.readText()) }
    private val user = UserProfile("detail-owner", "Tài khoản thử", "test@example.test", "TT", AuthProvider.GOOGLE)
    private class Repository(tasks: List<TaskItem>) : TaskSyncRepository {
        val items = MutableStateFlow(tasks)
        var local = tasks
        val details = mutableListOf<String>()
        val writes = mutableListOf<TaskItem>()
        val deletes = mutableListOf<String>()
        val detailGates = mutableMapOf<String, CompletableDeferred<Unit>>()
        var detailFailure: TaskDataException? = null
        var writeFailure: TaskDataException? = null
        var writeGate: CompletableDeferred<Unit>? = null
        var optimistic = false
        override fun cached(uid: String?) = local
        override fun cache(uid: String?, tasks: List<TaskItem>) { local = tasks }
        override suspend fun load(uid: String?) = TaskSnapshot(items.value, TaskSource.FIREBASE, true)
        override fun observe(uid: String) = items
        override suspend fun detail(uid: String?, id: String): TaskDetailSnapshot {
            details += id
            detailGates[id]?.await()
            detailFailure?.let { throw it }
            return TaskDetailSnapshot(items.value.first { it.id == id }, TaskSource.FIREBASE, true)
        }
        override suspend fun save(uid: String, task: TaskItem) {
            writes += task
            if (optimistic) items.value = items.value.map { if (it.id == task.id) task else it }
            writeGate?.await()
            writeFailure?.let { throw it }
            items.value = items.value.map { if (it.id == task.id) task else it }
        }
        override suspend fun delete(uid: String, id: String) {
            deletes += id
            if (optimistic) items.value = items.value.filterNot { it.id == id }
            writeGate?.await()
            writeFailure?.let { throw it }
            items.value = items.value.filterNot { it.id == id }
        }
    }
    private fun show(repository: Repository): TodoViewModel {
        lateinit var vm: TodoViewModel
        compose.runOnUiThread {
            val auth = object : AuthRepository {
                override fun currentUser() = user
                override suspend fun signIn(provider: AuthProvider) = AuthResult.Success(user)
                override suspend fun signOut() = Unit
            }
            vm = TodoViewModel(SavedStateHandle(mapOf("screen" to "home")), auth, FigmaTodoRepository(), taskSyncRepository = repository)
            compose.activity.setContent { TodoTheme { TodoApp({}, vm) } }
        }
        compose.onNodeWithTag("home_screen").assertIsDisplayed()
        return vm
    }

    @Test fun backButtonFromHomeDetailAlwaysReturnsToListEvenWhileLoading() {
        val repository = Repository(sample).apply { detailGates["2"] = CompletableDeferred() }
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithText("Danh sách").assertIsDisplayed()
        compose.onNodeWithTag("task_edit").assertIsNotEnabled()
        compose.onNodeWithTag("task_detail_back").performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        compose.runOnIdle { repository.detailGates.getValue("2").complete(Unit); assertEquals(AppScreen.TaskList, vm.uiState.screen) }
    }

    @Test fun androidBackAlsoReturnsFromDetailToList() {
        val vm = show(Repository(sample))
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppScreen.TaskList, vm.uiState.screen) }
    }

    @Test fun missingApiDetailShowsRetryAndKeepsSavedDataUntilTheUserReturns() {
        val repository = Repository(sample).apply { detailFailure = TaskDataException(TaskFailure.NOT_FOUND) }
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_detail_unavailable").assertIsDisplayed()
        compose.onNodeWithText(TaskDataMessages.forFailure(TaskFailure.NOT_FOUND)).assertIsDisplayed()
        compose.runOnIdle { assertNotNull(vm.uiState.taskBoard.find("2")); repository.detailFailure = null }
        compose.onNodeWithTag("task_retry").performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("task_edit").assertIsEnabled()
        compose.onNodeWithTag("task_detail_back").performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
    }

    @Test fun failedGetLeavesCachedDetailReadOnlyAndRetryRestoresEditing() {
        val repository = Repository(sample).apply { detailFailure = TaskDataException(TaskFailure.SERVER) }
        show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("task_edit").assertIsNotEnabled()
        compose.onNodeWithTag("task_retry").assertIsDisplayed()
        compose.runOnIdle { repository.detailFailure = null }
        compose.onNodeWithTag("task_retry").performClick()
        compose.onNodeWithTag("task_edit").assertIsEnabled()
    }

    @Test fun editingAndSubtaskCrudPersistTheRequestedTaskAndUpdateItsProgress() {
        val repository = Repository(sample)
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_edit").performClick()
        compose.onNodeWithTag("task_edit_title").performTextReplacement("Bài ôn Kotlin")
        compose.onNodeWithTag("task_edit_notes").performTextReplacement("Ghi chú đã cập nhật")
        compose.onNodeWithText("Lưu").performClick()
        compose.onNodeWithText("Bài ôn Kotlin").assertIsDisplayed()
        compose.onNodeWithText("Ghi chú đã cập nhật").assertIsDisplayed()
        compose.onNodeWithTag("subtask_toggle_2_1").performScrollTo().performClick()
        compose.onNodeWithText("2/2").assertIsDisplayed()
        compose.onNodeWithTag("subtask_input").performScrollTo().performTextInput("Việc nhỏ mới")
        compose.onNodeWithTag("subtask_input").performImeAction()
        compose.onNodeWithText("2/3").performScrollTo().assertIsDisplayed()
        val added = vm.uiState.taskBoard.find("2")!!.subtasks.last().id
        compose.onNodeWithTag("subtask_edit_$added").performScrollTo().performClick()
        compose.onNodeWithTag("subtask_edit_title").performTextReplacement("Việc nhỏ đã sửa")
        compose.onNodeWithText("Lưu").performClick()
        compose.onNodeWithTag("subtask_delete_$added").performScrollTo().performClick()
        compose.onNodeWithText("2/2").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(5, repository.writes.size)
            assertTrue(repository.writes.all { it.id == "2" })
            assertEquals(2, vm.uiState.taskBoard.find("2")!!.subtasks.size)
        }
    }

    @Test fun completionWaitsForAcknowledgementAndUpdatesListAndHome() {
        val repository = Repository(sample).apply { writeGate = CompletableDeferred(); optimistic = true }
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_complete").performScrollTo().performClick()
        compose.onNodeWithTag("task_complete").assertIsNotEnabled()
        compose.runOnIdle { assertFalse(vm.uiState.taskBoard.find("2")!!.completed); repository.writeGate!!.complete(Unit) }
        compose.onNodeWithText("Đánh dấu chưa hoàn thành").assertIsDisplayed()
        compose.onNodeWithTag("task_detail_back").performScrollTo().performClick()
        compose.onNodeWithText("3 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("tasks_nav_home").performClick()
        compose.onNodeWithText("3/4 việc").assertIsDisplayed()
    }

    @Test fun deleteRequiresConfirmationAndOnlyLeavesDetailAfterSuccessfulSave() {
        val repository = Repository(sample).apply { writeGate = CompletableDeferred(); optimistic = true }
        val vm = show(repository)
        compose.runOnIdle { vm.filterTasks(TaskFilter.STUDY); vm.searchTasks("CAU TRUC") }
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_delete").performScrollTo().performClick()
        compose.onNodeWithText("Hủy").performClick()
        compose.runOnIdle { assertTrue(repository.deletes.isEmpty()) }
        compose.onNodeWithTag("task_delete").performClick()
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.runOnIdle { assertNotNull(vm.uiState.taskBoard.find("2")); repository.writeGate!!.complete(Unit) }
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        compose.onNodeWithTag("task_filter_study").assertIsSelected()
        compose.onNodeWithTag("task_list_no_results").assertIsDisplayed()
        compose.runOnIdle { assertNull(vm.uiState.taskBoard.find("2")); assertEquals(listOf("2"), repository.deletes) }
    }

    @Test fun rejectedDeleteStaysOnDetailAndKeepsTheTask() {
        val repository = Repository(sample).apply { writeFailure = TaskDataException(TaskFailure.SERVER) }
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_delete").performScrollTo().performClick()
        compose.onNodeWithText("Xóa").performClick()
        compose.onNodeWithText(TaskDataMessages.SAVE_ERROR).assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppScreen.TaskDetail("2"), vm.uiState.screen); assertNotNull(vm.uiState.taskBoard.find("2")) }
    }

    @Test fun lateDetailResultCannotSwitchTheNextSelectedTask() {
        val repository = Repository(sample).apply { detailGates["2"] = CompletableDeferred() }
        val vm = show(repository)
        compose.onNodeWithTag("task_2").performClick()
        compose.onNodeWithTag("task_detail_back").performClick()
        compose.onNodeWithTag("list_task_3").performClick()
        compose.runOnIdle { repository.detailGates.getValue("2").complete(Unit) }
        compose.onNodeWithText("Đi chợ mua đồ cho tuần").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppScreen.TaskDetail("3"), vm.uiState.screen); assertEquals("3", vm.uiState.detailStatus.taskId) }
    }
}
