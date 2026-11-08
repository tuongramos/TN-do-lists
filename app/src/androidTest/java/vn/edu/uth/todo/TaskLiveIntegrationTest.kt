package vn.edu.uth.todo

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

/** Explicit live checks: use the existing signed-in user; never create/sign out an account. */
class TaskLiveIntegrationTest {
    private fun api() = MockTaskApi(TaskApiConfiguration(BuildConfig.TASKS_API_TOKEN))
    private fun store() = FirebaseTaskStore { TaskFirebaseDatabase.instance(BuildConfig.FIREBASE_DATABASE_URL) }

    @Test fun realTeachingApiReturnsTypedTasksAndStatistics() = runBlocking {
        val tasks = api().fetch()
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.all { it.id.isNotBlank() && TaskDates.isIsoDate(it.date) })
        val statistics = TaskStatisticsCalculator.calculate(tasks)
        assertTrue(statistics.completedInDay <= statistics.dayTasks.size)
    }

    @Test fun realDetailApiGetPatchAndDeleteUseTheProvidedEndpointAndToken() = runBlocking {
        val client = api()
        val original = client.fetchTask("1")
        client.updateTask(original) // No-op: send the current values; preserve the original tasks.
        assertEquals(original, client.fetchTask("1"))
        client.deleteTask("999999") // Missing sentinel only; never delete a user's actual API task.
        try { client.fetchTask("999999"); fail("Expected 404") }
        catch (error: TaskDataException) { assertEquals(TaskFailure.NOT_FOUND, error.reason) }
    }

    @Test fun signedInUserCanSaveReadAndRemoveOnlyItsTemporaryRecord() = runBlocking {
        val user = FirebaseAuth.getInstance().currentUser
        assumeTrue("Cần đăng nhập Google/Facebook trên thiết bị để kiểm tra Firebase thật", user != null)
        val uid = user!!.uid
        val cloud = store()
        var original = cloud.read(uid)
        if (!original.initialized) original = cloud.initialize(uid, api().fetch())
        val template = original.tasks.firstOrNull() ?: api().fetch().first()
        val id = "verification_${UUID.randomUUID()}"
        val temporary = template.copy(id = id, title = "Kiểm tra kết nối Realtime Database", sortOrder = Int.MAX_VALUE)
        var created = false
        try {
            cloud.save(uid, temporary)
            created = true
            assertEquals(temporary, cloud.read(uid).tasks.first { it.id == id })
        } finally {
            if (created) cloud.delete(uid, id)
        }
        val after = cloud.read(uid)
        assertTrue(after.initialized)
        assertFalse(after.tasks.any { it.id == id })
        assertEquals(original.tasks.map { it.id }.toSet(), after.tasks.map { it.id }.toSet())
    }

    @Test fun databaseRulesDenyReadingADifferentUsersPath() = runBlocking {
        assumeTrue("Cần phiên đăng nhập để kiểm tra cách ly UID", FirebaseAuth.getInstance().currentUser != null)
        try {
            store().read("verification_foreign_${UUID.randomUUID()}")
            fail("Không được đọc dữ liệu ngoài UID đang đăng nhập")
        } catch (error: TaskDataException) { assertEquals(TaskFailure.PERMISSION, error.reason) }
    }
}
