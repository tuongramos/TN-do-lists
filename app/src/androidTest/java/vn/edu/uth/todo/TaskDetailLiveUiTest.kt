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

class TaskDetailLiveUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realDetailGetKeepsSavedFieldsAndBackReturnsToList() {
        lateinit var vm: TodoViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[TodoViewModel::class.java] }
        assumeTrue("Cần phiên Google/Facebook thật", vm.uiState.session?.isGuest == false)
        compose.waitUntil(timeoutMillis = 30_000) { !vm.uiState.taskStatus.loading }
        val original = vm.uiState.taskBoard.find("1")
        assumeTrue("Cần công việc API ID 1 trong tài khoản", original != null)
        compose.runOnIdle { vm.openTask("1") }
        compose.waitUntil(timeoutMillis = 30_000) { !vm.uiState.detailStatus.loading }
        compose.onNodeWithTag("task_detail_screen").assertIsDisplayed()
        compose.onNodeWithText("Danh sách").assertIsDisplayed()
        compose.onNodeWithTag("task_edit").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(vm.uiState.detailStatus.loaded)
            assertNull(vm.uiState.detailStatus.warning)
            assertEquals(original, vm.uiState.taskBoard.find("1"))
        }
        val bitmap = compose.onNodeWithTag("task_detail_screen").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "task_detail_api.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithTag("task_detail_back").performScrollTo().performClick()
        compose.onNodeWithTag("task_list_screen").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppScreen.TaskList, vm.uiState.screen) }
    }
}
