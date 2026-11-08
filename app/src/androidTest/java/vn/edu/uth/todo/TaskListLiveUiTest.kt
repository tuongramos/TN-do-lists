package vn.edu.uth.todo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.model.*
import vn.edu.uth.todo.ui.TodoViewModel

/** Read-only UI verification with the existing Google/Facebook session and real database. */
class TaskListLiveUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun signedInListLoadsSearchesAndOpensRealSavedTasks() {
        lateinit var vm: TodoViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[TodoViewModel::class.java] }
        assumeTrue("Cần phiên đăng nhập thật trên thiết bị", vm.uiState.session?.isGuest == false)
        compose.waitUntil(timeoutMillis = 30_000) { !vm.uiState.taskStatus.loading }
        compose.runOnIdle {
            assertTrue(vm.uiState.taskStatus.cloudWritable)
            vm.searchTasks("")
            vm.filterTasks(TaskFilter.ALL)
            vm.selectNavigation(HomeAction.LIST)
        }
        compose.waitUntil(timeoutMillis = 30_000) { !vm.uiState.taskStatus.loading }
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        val tasks = vm.uiState.taskBoard.items
        compose.onNodeWithText("${tasks.count { !it.completed }} việc chưa xong").assertIsDisplayed()
        tasks.firstOrNull()?.let { task ->
            compose.onNodeWithTag("task_search").performTextReplacement(task.title)
            compose.onNodeWithTag("task_search").performImeAction()
            compose.onNodeWithTag("list_task_${task.id}").performScrollTo().performClick()
            compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
            compose.runOnIdle { assertEquals(AppScreen.TaskDetail(task.id), vm.uiState.screen) }
            compose.onNodeWithTag("task_detail_back").performScrollTo().performClick()
            compose.onNodeWithTag("task_search").performTextClearance()
            compose.onNodeWithTag("task_search").performImeAction()
        }
        val bitmap = compose.onNodeWithTag("task_list_screen").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "task_list_api.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.runOnIdle {
            assertEquals(TaskSource.FIREBASE, vm.uiState.taskStatus.source)
            assertTrue(vm.uiState.taskStatus.cloudWritable)
            assertEquals(tasks, vm.uiState.taskBoard.items)
        }
    }
}
