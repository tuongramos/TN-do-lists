package vn.edu.uth.todo

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.*

class TaskScreensTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val content = FigmaTaskRepository().getContent()

    private fun model(screen: String = "tasks", id: String = "mobile"): TodoViewModel = TodoViewModel(
        SavedStateHandle(mapOf("screen" to screen, "guest" to true, "task_id" to id)),
        UnconfiguredAuthRepository(), FigmaTodoRepository())

    private fun show(vm: TodoViewModel) = setTestContent { TodoTheme { TodoApp({}, vm) } }

    @Test fun homeNavigationAndEveryTaskOpenTheCorrectDetail() {
        val vm = model("home")
        show(vm)
        compose.onNodeWithTag("nav_list").performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        for (task in content.tasks) {
            compose.onNodeWithTag("list_task_${task.id}").performClick()
            compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
            compose.onAllNodesWithText(task.title).onFirst().assertIsDisplayed()
            compose.onNodeWithText(task.deadline).assertIsDisplayed()
            compose.runOnIdle { assertEquals(AppScreen.TaskDetail(task.id), vm.uiState.screen) }
            compose.onNodeWithTag("task_detail_back").performClick()
        }
        compose.onNodeWithTag("tasks_nav_home").performClick()
        compose.onNodeWithTag("task_mobile").performClick()
        compose.runOnIdle { assertEquals(AppScreen.TaskDetail("mobile"), vm.uiState.screen) }
    }

    @Test fun searchAndCategoryFiltersWorkTogether() {
        val vm = model()
        show(vm)
        compose.onNodeWithTag("task_filter_life").performClick().assertIsSelected()
        compose.onNodeWithTag("list_task_mobile").assertDoesNotExist()
        compose.onNodeWithTag("task_search").performTextInput("DI CHO")
        compose.onNodeWithTag("task_search").performImeAction()
        compose.onNodeWithTag("list_task_shopping").assertIsDisplayed()
        compose.onNodeWithTag("list_task_exercise").assertDoesNotExist()
        compose.onNodeWithTag("task_filter_study").performClick()
        compose.onNodeWithText(content.list.empty).assertIsDisplayed()
        compose.onNodeWithTag("task_search").performTextClearance()
        compose.onNodeWithTag("task_search").performImeAction()
        compose.onNodeWithTag("list_task_mobile").assertIsDisplayed()
    }

    @Test fun completeAndReopenUpdateBothTheListAndDetail() {
        val vm = model()
        show(vm)
        compose.onNodeWithTag("task_toggle_mobile").performClick()
        compose.onNodeWithText("3 việc chưa xong").assertIsDisplayed()
        compose.onNodeWithTag("task_toggle_mobile").assertIsOn()
        compose.onNodeWithTag("list_task_mobile").performClick()
        compose.onNodeWithTag("task_complete").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(vm.uiState.taskBoard.find("mobile")!!.completed) }
        compose.onNodeWithTag("task_detail_back").performScrollTo().performClick()
        compose.onNodeWithText("4 việc chưa xong").assertIsDisplayed()
    }

    @Test fun subtaskProgressAndAddingEditingDeletingSubtasksWork() {
        val vm = model("task_detail")
        show(vm)
        compose.onNodeWithTag("subtask_toggle_submit").performClick()
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithTag("subtask_input").performScrollTo().performTextInput("Kiểm tra file trước khi nộp")
        compose.onNodeWithTag("subtask_input").performImeAction()
        compose.runOnIdle { assertEquals(4, vm.uiState.taskBoard.find("mobile")!!.subtasks.size) }
        compose.onNodeWithText("3/4").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("subtask_edit_submit").performScrollTo().performClick()
        compose.onNodeWithTag("subtask_edit_title").performTextReplacement("Nộp bài lên hệ thống")
        compose.onNodeWithText(content.detail.save).performClick()
        compose.runOnIdle { assertEquals("Nộp bài lên hệ thống", vm.uiState.taskBoard.find("mobile")!!.subtasks.first { it.id == "submit" }.title) }
        compose.onNodeWithTag("subtask_delete_submit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3, vm.uiState.taskBoard.find("mobile")!!.subtasks.size) }
    }

    @Test fun taskEditingAndDeleteConfirmationUpdateTheCollection() {
        val vm = model("task_detail")
        show(vm)
        compose.onNodeWithTag("task_edit").performClick()
        compose.onNodeWithTag("task_edit_title").performTextReplacement("Bài tập Kotlin")
        compose.onNodeWithTag("task_edit_notes").performTextReplacement("Ghi chú mới")
        compose.onNodeWithText(content.detail.save).performClick()
        compose.onNodeWithText("Bài tập Kotlin").assertIsDisplayed()
        compose.onNodeWithText("Ghi chú mới").assertIsDisplayed()
        compose.onNodeWithTag("task_delete").performScrollTo().performClick()
        compose.onNodeWithText(content.detail.cancel).performClick()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.onNodeWithTag("task_delete").performScrollTo().performClick()
        compose.onNodeWithText(content.detail.confirmDelete).performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        compose.onNodeWithTag("list_task_mobile").assertDoesNotExist()
        compose.onNodeWithText("3 việc chưa xong").assertIsDisplayed()
    }

    @Test fun savedStateRestoresSelectedTaskAndItsChangesAndRejectsMissingIds() {
        val saved = SavedStateHandle(mapOf("screen" to "tasks", "guest" to true))
        val vm = TodoViewModel(saved, UnconfiguredAuthRepository(), FigmaTodoRepository())
        compose.runOnUiThread {
            vm.openTask("mobile")
            vm.toggleSubtask("mobile", "submit")
            vm.editTask("mobile", "Bài tập Kotlin", "Ghi chú đã sửa")
            val restored = TodoViewModel(saved, UnconfiguredAuthRepository(), FigmaTodoRepository())
            assertEquals(AppScreen.TaskDetail("mobile"), restored.uiState.screen)
            assertEquals("3/3", restored.uiState.taskBoard.find("mobile")!!.subtaskProgressLabel)
            assertEquals("Bài tập Kotlin", restored.uiState.taskBoard.find("mobile")!!.title)
            restored.deleteTask("mobile")
            assertEquals(AppScreen.TaskList, restored.uiState.screen)
            restored.openTask("missing")
            assertEquals(AppScreen.TaskList, restored.uiState.screen)
        }
    }

    @Test fun actualActivityKeepsTaskDetailAfterRecreation() {
        lateinit var actual: TodoViewModel
        compose.runOnUiThread {
            actual = ViewModelProvider(compose.activity)[TodoViewModel::class.java]
            if (actual.uiState.session == null) actual.tryAsGuest()
        }
        compose.waitUntil(timeoutMillis = 30_000) { !actual.uiState.taskStatus.loading }
        assumeTrue("Cần dữ liệu API hoặc cache để kiểm tra Activity thật", actual.uiState.taskBoard.items.isNotEmpty())
        val selected = actual.uiState.taskBoard.items.first()
        compose.runOnUiThread { actual.openTask(selected.id) }
        compose.onAllNodesWithText(selected.title).onFirst().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.onAllNodesWithText(selected.title).onFirst().assertIsDisplayed()
        compose.onNodeWithTag("task_detail_back").performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
    }

    @Test fun captureTaskList() {
        show(model())
        capture("task_list_screen", "task_list")
    }

    @Test fun captureTaskDetail() {
        show(model("task_detail"))
        capture("task_detail_screen", "task_detail")
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        compose.runOnUiThread { compose.activity.setContent(content = content) }
    }

    private fun capture(tag: String, name: String) {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
