package vn.edu.uth.todo

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

class TaskApiAndStatisticsTest {
    private val json = javaClass.getResourceAsStream("/tasks-api-sample.json")!!.bufferedReader().use { it.readText() }
    private fun api(status: Int = 200, body: String = json) = MockTaskApi(TaskApiConfiguration("test-token"),
        TaskHttpTransport { TaskHttpResponse(status, body) })

    @Test fun actualApiSchemaMapsAllSixRecordsWithoutLosingFields() = runBlocking {
        val tasks = api().fetch()
        assertEquals(listOf("4", "1", "2", "3", "6", "5"), tasks.map { it.id })
        assertEquals(TaskPriority.LOW, tasks.first { it.id == "3" }.priority)
        assertFalse(tasks.first { it.id == "3" }.remind)
        assertEquals("Không", tasks.first { it.id == "3" }.reminder)
        assertEquals(3, tasks.first { it.id == "1" }.completedSubtasks)
        assertEquals("2026-10-20", tasks.first().date)
        assertTrue(tasks.first().subtasks.isEmpty())
    }

    @Test fun firebaseRecordRoundTripRetainsEveryApiFieldAndStableSubtaskIds() = runBlocking {
        val tasks = api().fetch()
        assertEquals(tasks, tasks.map { FirebaseTaskRecord.from(it).toDomain() })
        assertNotNull(FirebaseTaskRecord::class.java.getDeclaredConstructor())
    }

    @Test fun homeAndProfileStatisticsAreCalculatedFromApiData() = runBlocking {
        val stats = TaskStatisticsCalculator.calculate(api().fetch(), "2026-10-20")
        assertEquals(4, stats.dayTasks.size)
        assertEquals(2, stats.completedInDay)
        assertEquals(50, DailyProgress(stats.completedInDay, stats.dayTasks.size).percent)
        assertEquals(2, stats.remainingStudy)
        assertEquals(2, stats.remainingLife)
        assertEquals(2, stats.completedTotal)
        assertEquals(4, stats.remainingTotal)
        assertEquals(33, stats.completionPercent)
        assertEquals(2, stats.week.sumOf { it.completed })
    }

    @Test fun historicalMockDateIsDisplayedAsItsOwnDate() = runBlocking {
        val stats = TaskStatisticsCalculator.calculate(api().fetch(), "2026-11-08")
        assertEquals("2026-10-20", stats.date)
        assertFalse(stats.isToday)
        assertEquals(4, stats.dayTasks.size)
    }

    @Test fun emptyApiIsAValidEmptyStateAndNeverDividesByZero() = runBlocking {
        val tasks = api(body = "[]").fetch()
        val stats = TaskStatisticsCalculator.calculate(tasks, "2026-11-08")
        assertTrue(tasks.isEmpty())
        assertEquals(0, stats.completionPercent)
        assertEquals(0, DailyProgress(0, 0).percent)
        assertEquals("0/0 việc", DailyProgress(0, 0).label)
    }

    @Test fun completingATaskImmediatelyChangesAllRelatedStatistics() = runBlocking {
        val changed = api().fetch().map { if (it.id == "2") it.copy(completed = true) else it }
        val stats = TaskStatisticsCalculator.calculate(changed, "2026-10-20")
        assertEquals(3, stats.completedInDay)
        assertEquals(1, stats.remainingStudy)
        assertEquals(3, stats.completedTotal)
        assertEquals(50, stats.completionPercent)
    }

    @Test fun authenticationServerNetworkAndSchemaFailuresRemainDistinct() = runBlocking {
        suspend fun reason(block: suspend () -> Unit): TaskFailure {
            try { block() } catch (error: TaskDataException) { return error.reason }
            error("Expected a failure")
        }
        assertEquals(TaskFailure.AUTHENTICATION, reason { api(status = 401).fetch() })
        assertEquals(TaskFailure.SERVER, reason { api(status = 500).fetch() })
        assertEquals(TaskFailure.INVALID_RESPONSE, reason { api(body = "{\"tasks\":[]}").fetch() })
        assertEquals(TaskFailure.INVALID_RESPONSE, reason { api(body = "[{\"id\":1}]").fetch() })
        assertEquals(TaskFailure.NETWORK, reason {
            MockTaskApi(TaskApiConfiguration("test"), TaskHttpTransport { throw IOException("offline") }).fetch()
        })
    }

    @Test fun configurationNeverPrintsItsBearerToken() {
        assertFalse(TaskApiConfiguration("private-test-value").toString().contains("private-test-value"))
    }
}
