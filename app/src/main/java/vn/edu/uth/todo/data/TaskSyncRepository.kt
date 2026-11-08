package vn.edu.uth.todo.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.uth.todo.model.*

interface TaskSyncRepository {
    suspend fun load(uid: String?): TaskSnapshot
    fun cached(uid: String?): List<TaskItem>?
    fun observe(uid: String): Flow<List<TaskItem>>
    suspend fun save(uid: String, task: TaskItem)
    suspend fun delete(uid: String, id: String)
    fun cache(uid: String?, tasks: List<TaskItem>)
    suspend fun detail(uid: String?, id: String): TaskDetailSnapshot {
        val task = cached(uid)?.firstOrNull { it.id == id } ?: throw TaskDataException(TaskFailure.NOT_FOUND)
        return TaskDetailSnapshot(task, if (uid == null) TaskSource.CACHE else TaskSource.FIREBASE, uid != null)
    }
    suspend fun saveGuest(task: TaskItem) {
        cache(null, cached(null).orEmpty().map { if (it.id == task.id) task else it })
    }
    suspend fun deleteGuest(id: String) { cache(null, cached(null).orEmpty().filterNot { it.id == id }) }
}

class ApiFirebaseTaskRepository(
    private val api: TaskApi, private val cloud: TaskCloudStore, private val local: TaskCache
) : TaskSyncRepository {
    private fun owner(uid: String?) = uid ?: "guest"
    override fun cached(uid: String?) = local.read(owner(uid))
    override fun cache(uid: String?, tasks: List<TaskItem>) = local.write(owner(uid), tasks)

    override suspend fun load(uid: String?): TaskSnapshot {
        if (uid == null) {
            cached(null)?.let { return TaskSnapshot(it, TaskSource.CACHE) }
            val tasks = api.fetch()
            cache(null, tasks)
            return TaskSnapshot(tasks, TaskSource.API)
        }
        return try {
            var collection = cloud.read(uid)
            if (!collection.initialized) collection = cloud.initialize(uid, api.fetch())
            cache(uid, collection.tasks)
            TaskSnapshot(collection.tasks, TaskSource.FIREBASE, cloudWritable = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val warning = (error as? TaskDataException)?.message ?: TaskDataMessages.forFailure(TaskFailure.UNKNOWN)
            cached(uid)?.let { return TaskSnapshot(it, TaskSource.CACHE, warning = warning) }
            val tasks = api.fetch()
            cache(uid, tasks)
            TaskSnapshot(tasks, TaskSource.API, warning = warning)
        }
    }

    override fun observe(uid: String): Flow<List<TaskItem>> = cloud.observe(uid).map { collection ->
        if (!collection.initialized) throw TaskDataException(TaskFailure.INVALID_RESPONSE)
        collection.tasks // The ViewModel caches committed updates after any pending write is acknowledged.
    }

    override suspend fun detail(uid: String?, id: String): TaskDetailSnapshot {
        val remote = api.fetchTask(id)
        val cached = cached(uid)?.firstOrNull { it.id == id }
        if (uid == null) return TaskDetailSnapshot(cached ?: remote, TaskSource.CACHE)
        return try {
            val collection = cloud.read(uid)
            if (!collection.initialized) throw TaskDataException(TaskFailure.CONFIGURATION)
            val saved = collection.tasks.firstOrNull { it.id == id } ?: throw TaskDataException(TaskFailure.NOT_FOUND)
            cache(uid, collection.tasks)
            TaskDetailSnapshot(saved, TaskSource.FIREBASE, cloudWritable = true)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if ((error as? TaskDataException)?.reason == TaskFailure.NOT_FOUND) throw error
            TaskDetailSnapshot(cached ?: remote, if (cached == null) TaskSource.API else TaskSource.CACHE,
                warning = error.message ?: TaskDataMessages.forFailure(TaskFailure.UNKNOWN))
        }
    }

    private suspend fun persistAfterApi(block: suspend () -> Unit) {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            throw TaskDataException((error as? TaskDataException)?.reason ?: TaskFailure.UNKNOWN, error,
                TaskDetailMessages.FIREBASE_PENDING)
        }
    }

    override suspend fun save(uid: String, task: TaskItem) {
        api.updateTask(task)
        persistAfterApi { cloud.save(uid, task) }
    }
    override suspend fun delete(uid: String, id: String) {
        api.deleteTask(id)
        persistAfterApi { cloud.delete(uid, id) }
    }
    override suspend fun saveGuest(task: TaskItem) {
        api.updateTask(task)
        super<TaskSyncRepository>.saveGuest(task)
    }
    override suspend fun deleteGuest(id: String) {
        api.deleteTask(id)
        super<TaskSyncRepository>.deleteGuest(id)
    }
}
