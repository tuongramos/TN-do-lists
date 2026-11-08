package vn.edu.uth.todo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.*
import vn.edu.uth.todo.model.*

class TaskSyncRepositoryTest {
    private val json = javaClass.getResourceAsStream("/tasks-api-sample.json")!!.bufferedReader().use { it.readText() }
    private val sample = MockTaskApi(TaskApiConfiguration("test")).decode(json)
    private fun readWriteApi(read: suspend () -> List<TaskItem>) = object : TaskApi {
        override suspend fun fetch() = read()
        override suspend fun updateTask(task: TaskItem) = Unit
        override suspend fun deleteTask(id: String) = Unit
    }

    private class Cache : TaskCache {
        val values = mutableMapOf<String, List<TaskItem>>()
        override fun read(owner: String) = values[owner]
        override fun write(owner: String, tasks: List<TaskItem>) { values[owner] = tasks }
    }
    private class Cloud : TaskCloudStore {
        val collections = mutableMapOf<String, CloudTaskCollection>()
        var error: TaskDataException? = null
        var seeds = 0
        var reads = 0
        var concurrentValue: CloudTaskCollection? = null
        override suspend fun read(uid: String): CloudTaskCollection {
            reads++
            error?.let { throw it }
            return collections[uid] ?: CloudTaskCollection(false, emptyList())
        }
        override suspend fun initialize(uid: String, tasks: List<TaskItem>): CloudTaskCollection {
            seeds++
            return (concurrentValue ?: collections[uid]?.takeIf { it.initialized }
                ?: CloudTaskCollection(true, tasks)).also { collections[uid] = it }
        }
        override fun observe(uid: String): Flow<CloudTaskCollection> = MutableStateFlow(collections.getValue(uid))
        override suspend fun save(uid: String, task: TaskItem) {
            val old = collections.getValue(uid)
            collections[uid] = old.copy(tasks = old.tasks.filterNot { it.id == task.id } + task)
        }
        override suspend fun delete(uid: String, id: String) {
            val old = collections.getValue(uid)
            collections[uid] = old.copy(tasks = old.tasks.filterNot { it.id == id })
        }
    }

    @Test fun firstOpenImportsApiOnceAndLaterLoadsKeepCloudEditsAndDeletions() = runBlocking {
        val cloud = Cloud(); val cache = Cache(); var apiCalls = 0
        val repository = ApiFirebaseTaskRepository(readWriteApi { apiCalls++; sample }, cloud, cache)
        assertEquals(6, repository.load("user-a").tasks.size)
        repository.save("user-a", sample.first().copy(title = "Tên đã sửa"))
        repository.delete("user-a", "1")
        val reopened = repository.load("user-a")
        assertEquals(1, apiCalls)
        assertEquals(1, cloud.seeds)
        assertTrue(reopened.cloudWritable)
        assertTrue(reopened.tasks.any { it.title == "Tên đã sửa" })
        assertFalse(reopened.tasks.any { it.id == "1" })
    }

    @Test fun deletingEveryTaskDoesNotTriggerAnotherApiImport() = runBlocking {
        val cloud = Cloud(); var apiCalls = 0
        val repository = ApiFirebaseTaskRepository(readWriteApi { apiCalls++; sample }, cloud, Cache())
        repository.load("user-a")
        sample.forEach { repository.delete("user-a", it.id) }
        assertTrue(repository.load("user-a").tasks.isEmpty())
        assertEquals(1, apiCalls)
    }

    @Test fun concurrentInitializationUsesTheWinningCloudCollection() = runBlocking {
        val cloud = Cloud().apply { concurrentValue = CloudTaskCollection(true, listOf(sample.first().copy(title = "Từ thiết bị khác"))) }
        val repository = ApiFirebaseTaskRepository(TaskApi { sample }, cloud, Cache())
        assertEquals("Từ thiết bị khác", repository.load("user-a").tasks.single().title)
    }

    @Test fun permissionFailureShowsApiDataWithoutPretendingItWasSaved() = runBlocking {
        val cloud = Cloud().apply { error = TaskDataException(TaskFailure.PERMISSION) }
        val repository = ApiFirebaseTaskRepository(TaskApi { sample }, cloud, Cache())
        val loaded = repository.load("user-a")
        assertEquals(TaskSource.API, loaded.source)
        assertFalse(loaded.cloudWritable)
        assertNotNull(loaded.warning)
        assertEquals(6, loaded.tasks.size)
        assertEquals(0, cloud.seeds)
    }

    @Test fun offlineCacheIsScopedToItsOwner() = runBlocking {
        val cloud = Cloud().apply { error = TaskDataException(TaskFailure.NETWORK) }
        val cache = Cache().apply { write("user-a", listOf(sample.first().copy(title = "Công việc riêng A"))) }
        val repository = ApiFirebaseTaskRepository(TaskApi { sample }, cloud, cache)
        assertEquals("Công việc riêng A", repository.load("user-a").tasks.single().title)
        assertFalse(repository.load("user-b").tasks.any { it.title == "Công việc riêng A" })
    }

    @Test fun guestEditsStayInLocalCacheAndNeverUseFirebase() = runBlocking {
        val cloud = Cloud(); val repository = ApiFirebaseTaskRepository(TaskApi { sample }, cloud, Cache())
        val loaded = repository.load(null)
        val changed = loaded.tasks.map { it.copy(completed = false) }
        repository.cache(null, changed)
        assertEquals(changed, repository.load(null).tasks)
        assertEquals(0, cloud.reads)
        assertEquals(0, cloud.seeds)
    }
}
