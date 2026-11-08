package vn.edu.uth.todo

import com.google.gson.JsonParser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

class TaskDetailApiTest {
    private val json = javaClass.getResourceAsStream("/tasks-api-sample.json")!!.bufferedReader().use { it.readText() }
    private val sample = MockTaskApi(TaskApiConfiguration("test")).decode(json)
    private val detailJson = JsonParser.parseString(json).asJsonArray[1].toString()

    private class Cache : TaskCache {
        val items = mutableMapOf<String, List<TaskItem>>()
        override fun read(owner: String) = items[owner]
        override fun write(owner: String, tasks: List<TaskItem>) { items[owner] = tasks }
    }
    private class Cloud(var collection: CloudTaskCollection) : TaskCloudStore {
        var writes = 0
        var failWrite = false
        override suspend fun read(uid: String) = collection
        override suspend fun initialize(uid: String, tasks: List<TaskItem>) = collection
        override fun observe(uid: String) = flowOf(collection)
        override suspend fun save(uid: String, task: TaskItem) {
            writes++
            if (failWrite) throw TaskDataException(TaskFailure.PERMISSION)
            collection = collection.copy(tasks = collection.tasks.map { if (it.id == task.id) task else it })
        }
        override suspend fun delete(uid: String, id: String) {
            writes++
            if (failWrite) throw TaskDataException(TaskFailure.PERMISSION)
            collection = collection.copy(tasks = collection.tasks.filterNot { it.id == id })
        }
    }
    private class Api(val items: List<TaskItem>) : TaskApi {
        val detailIds = mutableListOf<String>()
        val updates = mutableListOf<TaskItem>()
        val deletes = mutableListOf<String>()
        var reject = false
        override suspend fun fetch() = items
        override suspend fun fetchTask(id: String): TaskItem {
            detailIds += id
            return items.first { it.id == id }
        }
        override suspend fun updateTask(task: TaskItem) {
            if (reject) throw TaskDataException(TaskFailure.SERVER)
            updates += task
        }
        override suspend fun deleteTask(id: String) {
            if (reject) throw TaskDataException(TaskFailure.SERVER)
            deletes += id
        }
    }

    @Test fun getDetailUsesTheExactIdAndRejectsAMismatchingResponse() = runBlocking {
        val requests = mutableListOf<TaskHttpRequest>()
        val api = MockTaskApi(TaskApiConfiguration("private-test"), TaskHttpTransport {
            requests += it; TaskHttpResponse(200, detailJson)
        })
        assertEquals("1", api.fetchTask("1").id)
        assertEquals("https://share.tryapps.store/mock/teach/tasks/1", requests.single().configuration.endpoint)
        assertEquals(TaskHttpMethod.GET, requests.single().method)
        try { api.fetchTask("2"); fail() }
        catch (error: TaskDataException) { assertEquals(TaskFailure.INVALID_RESPONSE, error.reason) }
        assertFalse(requests.first().toString().contains("private-test"))
    }

    @Test fun patchSerializesTheRequestedTaskAndIgnoresTheMocksOtherId() = runBlocking {
        var request: TaskHttpRequest? = null
        val api = MockTaskApi(TaskApiConfiguration("test"), TaskHttpTransport {
            request = it
            TaskHttpResponse(200, "{\"id\":2,\"title\":\"Fixed mock response\"}")
        })
        val task = sample.first { it.id == "1" }.copy(notes = "Ghi chú \"Kotlin\"\nDòng 2", subtasks = emptyList(), completed = false)
        api.updateTask(task)
        val sent = requireNotNull(request)
        assertEquals(TaskHttpMethod.PATCH, sent.method)
        assertTrue(sent.configuration.endpoint.endsWith("/1"))
        val body = JsonParser.parseString(sent.body).asJsonObject
        assertEquals(task.notes, body["note"].asString)
        assertFalse(body["done"].asBoolean)
        assertTrue(body["subtasks"].asJsonArray.isEmpty)
        assertFalse(body.has("id"))
    }

    @Test fun deleteAcceptsNoContentAndRepeatedNotFoundButGetReportsNotFound() = runBlocking {
        var status = 204
        val api = MockTaskApi(TaskApiConfiguration("test"), TaskHttpTransport { TaskHttpResponse(status, "") })
        api.deleteTask("1")
        status = 404
        api.deleteTask("1")
        try { api.fetchTask("1"); fail() }
        catch (error: TaskDataException) { assertEquals(TaskFailure.NOT_FOUND, error.reason) }
        try { api.fetchTask("../1"); fail() }
        catch (error: TaskDataException) { assertEquals(TaskFailure.NOT_FOUND, error.reason) }
    }

    @Test fun detailCallsApiButRetainsTheOwnersFirebaseEditsAndSubtaskIds() = runBlocking {
        val api = Api(sample)
        val edited = sample.first { it.id == "1" }.copy(title = "Tên riêng đã lưu", subtasks = listOf(Subtask("stable-uuid", "Việc riêng")))
        val cloud = Cloud(CloudTaskCollection(true, listOf(edited)))
        val repository = ApiFirebaseTaskRepository(api, cloud, Cache())
        val result = repository.detail("owner", "1")
        assertEquals(listOf("1"), api.detailIds)
        assertEquals(edited, result.task)
        assertTrue(result.cloudWritable)
        assertEquals(listOf(edited), repository.cached("owner"))
    }

    @Test fun rejectedPatchOrDeleteNeverChangesFirebase() = runBlocking {
        val api = Api(sample).apply { reject = true }
        val cloud = Cloud(CloudTaskCollection(true, sample))
        val repository = ApiFirebaseTaskRepository(api, cloud, Cache())
        try { repository.save("owner", sample.first().copy(completed = false)); fail() }
        catch (error: TaskDataException) { assertEquals(TaskFailure.SERVER, error.reason) }
        try { repository.delete("owner", "1"); fail() }
        catch (error: TaskDataException) { assertEquals(TaskFailure.SERVER, error.reason) }
        assertEquals(0, cloud.writes)
        assertEquals(sample, cloud.collection.tasks)
    }

    @Test fun successfulPatchKeepsTheRequestedObjectEvenWhenTheMockReturnsAnother() = runBlocking {
        val api = MockTaskApi(TaskApiConfiguration("test"), TaskHttpTransport {
            TaskHttpResponse(200, "{\"id\":2,\"done\":true}")
        })
        val cloud = Cloud(CloudTaskCollection(true, sample))
        val repository = ApiFirebaseTaskRepository(api, cloud, Cache())
        val changed = sample.first { it.id == "1" }.copy(title = "Tên gửi lên", completed = false)
        repository.save("owner", changed)
        assertEquals(changed, cloud.collection.tasks.first { it.id == "1" })
        assertEquals(sample.first { it.id == "2" }, cloud.collection.tasks.first { it.id == "2" })
    }

    @Test fun partialFirebaseFailureIsExplicitAndDeleteCanBeRetried() = runBlocking {
        val api = Api(sample)
        val cloud = Cloud(CloudTaskCollection(true, sample)).apply { failWrite = true }
        val repository = ApiFirebaseTaskRepository(api, cloud, Cache())
        try { repository.delete("owner", "1"); fail() }
        catch (error: TaskDataException) {
            assertEquals(TaskFailure.PERMISSION, error.reason)
            assertEquals(TaskDetailMessages.FIREBASE_PENDING, error.message)
        }
        assertEquals(sample, cloud.collection.tasks)
        cloud.failWrite = false
        repository.delete("owner", "1")
        assertEquals(listOf("1", "1"), api.deletes)
        assertFalse(cloud.collection.tasks.any { it.id == "1" })
    }

    @Test fun guestDetailAndMutationsUseTheApiAndLocalCacheOnly() = runBlocking {
        val api = Api(sample)
        val cloud = Cloud(CloudTaskCollection(true, sample))
        val cache = Cache().apply { write("guest", sample) }
        val repository = ApiFirebaseTaskRepository(api, cloud, cache)
        val changed = sample.first { it.id == "1" }.copy(notes = "Bản dùng thử")
        repository.saveGuest(changed)
        assertEquals(changed, repository.detail(null, "1").task)
        repository.deleteGuest("1")
        assertFalse(repository.cached(null)!!.any { it.id == "1" })
        assertEquals(listOf(changed), api.updates)
        assertEquals(listOf("1"), api.deletes)
        assertEquals(0, cloud.writes)
    }
}
