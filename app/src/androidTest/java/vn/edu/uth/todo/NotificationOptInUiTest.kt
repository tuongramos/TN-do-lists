package vn.edu.uth.todo

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import vn.edu.uth.todo.data.AuthRepository
import vn.edu.uth.todo.data.DeviceNotificationSettings
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.AuthResult
import vn.edu.uth.todo.model.NotificationCopy
import vn.edu.uth.todo.model.UserProfile
import vn.edu.uth.todo.ui.TodoTheme
import vn.edu.uth.todo.ui.TodoViewModel

class NotificationOptInUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val announcements = CopyOnWriteArrayList<String>()

    private fun show(owner: String): DeviceNotificationSettings {
        val settings = DeviceNotificationSettings(compose.activity.applicationContext)
        val profile = UserProfile(owner, "Tài khoản kiểm thử", "test@example.test", "TT", AuthProvider.GOOGLE)
        val auth = object : AuthRepository {
            override fun currentUser() = profile
            override suspend fun signIn(provider: AuthProvider) = AuthResult.Success(profile)
            override suspend fun signOut() = Unit
        }
        compose.runOnUiThread {
            val vm = TodoViewModel(SavedStateHandle(), auth, FigmaTodoRepository(), notificationSettings = settings)
            compose.activity.setContent { TodoTheme { TodoApp({ announcements.add(it) }, vm) } }
        }
        return settings
    }

    @Test fun laterLeavesNotificationsOffAndEachBellCanReopenThePrompt() {
        val owner = UUID.randomUUID().toString()
        val settings = show(owner)
        compose.onNodeWithText(NotificationCopy.PROMPT).assertDoesNotExist()
        compose.onNodeWithTag("home_notifications").performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertIsDisplayed()
        compose.onNodeWithText(NotificationCopy.LATER).performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertDoesNotExist()
        assertFalse(settings.isEnabled("account:$owner"))

        compose.onNodeWithTag("nav_notifications").performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertIsDisplayed()
        compose.onNodeWithText(NotificationCopy.LATER).performClick()
        compose.onNodeWithTag("nav_list").performClick()
        compose.onNodeWithTag("tasks_nav_notifications").performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertIsDisplayed()
        assertFalse(settings.isEnabled("account:$owner"))
    }

    @Test fun systemApprovalPersistsForTheCurrentAccountOnly() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        val owner = UUID.randomUUID().toString()
        val settings = show(owner)
        compose.onNodeWithTag("nav_notifications").performClick()

        if (!settings.canPostNotifications()) {
            compose.onNodeWithText(NotificationCopy.ALLOW).performClick()
            clickSystemPermissionButton("permission_deny_button")
            compose.waitUntil(timeoutMillis = 10_000) { NotificationCopy.SYSTEM_BLOCKED in announcements }
            assertFalse(settings.isEnabled("account:$owner"))
            compose.onNodeWithTag("nav_notifications").performClick()
            compose.onNodeWithText(NotificationCopy.PROMPT).assertIsDisplayed()
        }

        val needsSystemPermission = !settings.canPostNotifications()
        compose.onNodeWithText(NotificationCopy.ALLOW).performClick()
        if (needsSystemPermission) clickSystemPermissionButton("permission_allow_button")
        compose.waitUntil(timeoutMillis = 10_000) { settings.isEnabled("account:$owner") }
        assertTrue(settings.canPostNotifications())

        show(owner)
        compose.onNodeWithTag("nav_notifications").performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertDoesNotExist()
        show(UUID.randomUUID().toString())
        compose.onNodeWithTag("nav_notifications").performClick()
        compose.onNodeWithText(NotificationCopy.PROMPT).assertIsDisplayed()
    }

    private fun clickSystemPermissionButton(viewId: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        compose.waitUntil(timeoutMillis = 20_000) {
            val root = automation.rootInActiveWindow ?: return@waitUntil false
            // Google builds can use the AOSP resource namespace in a differently named APK.
            listOf("com.android.permissioncontroller", root.packageName.toString()).distinct().any { namespace ->
                root.findAccessibilityNodeInfosByViewId("$namespace:id/$viewId").any {
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }
    }
}
