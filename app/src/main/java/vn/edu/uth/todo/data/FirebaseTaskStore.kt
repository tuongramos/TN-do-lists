package vn.edu.uth.todo.data

import com.google.firebase.database.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import vn.edu.uth.todo.model.*

interface TaskCloudStore {
    suspend fun read(uid: String): CloudTaskCollection
    suspend fun initialize(uid: String, tasks: List<TaskItem>): CloudTaskCollection
    fun observe(uid: String): Flow<CloudTaskCollection>
    suspend fun save(uid: String, task: TaskItem)
    suspend fun delete(uid: String, id: String)
}

object TaskFirebaseDatabase {
    private val instances = mutableMapOf<String, FirebaseDatabase>()
    @Synchronized fun instance(url: String): FirebaseDatabase {
        if (url.isBlank()) throw TaskDataException(TaskFailure.CONFIGURATION)
        return instances.getOrPut(url) { FirebaseDatabase.getInstance(url).apply { setPersistenceEnabled(true) } }
    }
}

class FirebaseTaskStore(private val database: () -> FirebaseDatabase) : TaskCloudStore {
    private fun reference(uid: String): DatabaseReference {
        require(uid.isNotBlank() && uid.none { it in ".#$[]/" })
        return database().getReference("users").child(uid).child("taskData")
    }
    private fun snapshot(snapshot: DataSnapshot): CloudTaskCollection = try {
        val initialized = snapshot.child("initialized").getValue(Boolean::class.java) == true
        val tasks = snapshot.child("tasks").children.map { child ->
            val dto = child.getValue(FirebaseTaskRecord::class.java) ?: error("task")
            require(dto.id == child.key)
            dto.toDomain()
        }.sortedBy { it.sortOrder }
        CloudTaskCollection(initialized, tasks)
    } catch (error: Exception) {
        throw TaskDataException(TaskFailure.INVALID_RESPONSE, error)
    }

    private fun failure(error: DatabaseError) = TaskDataException(when (error.code) {
        DatabaseError.PERMISSION_DENIED -> TaskFailure.PERMISSION
        DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR, DatabaseError.UNAVAILABLE -> TaskFailure.NETWORK
        else -> TaskFailure.UNKNOWN
    })

    private suspend fun <T : Any> request(block: suspend () -> T): T = withTimeoutOrNull(15_000) {
        try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: TaskDataException) { throw error }
        catch (error: Exception) {
            // DatabaseReference.get() may fail with a plain Exception("Permission denied").
            throw TaskDataException(if (error.message.orEmpty().contains("permission", true)) TaskFailure.PERMISSION else TaskFailure.NETWORK, error)
        }
    } ?: throw TaskDataException(TaskFailure.NETWORK)

    override suspend fun read(uid: String): CloudTaskCollection = request { snapshot(reference(uid).get().await()) }

    override suspend fun initialize(uid: String, tasks: List<TaskItem>): CloudTaskCollection = request {
        suspendCancellableCoroutine { continuation ->
            reference(uid).runTransaction(object : Transaction.Handler {
                override fun doTransaction(current: MutableData): Transaction.Result {
                    if (current.child("initialized").getValue(Boolean::class.java) == true) return Transaction.success(current)
                    current.value = mapOf("initialized" to true, "schemaVersion" to 1,
                        "source" to "https://share.tryapps.store/mock/teach/tasks", "seededAt" to ServerValue.TIMESTAMP,
                        "tasks" to tasks.associate { it.id to FirebaseTaskRecord.from(it) })
                    return Transaction.success(current)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                    if (!continuation.isActive) return
                    if (error != null) continuation.resumeWithException(failure(error))
                    else if (committed && data != null) {
                        try { continuation.resume(snapshot(data)) }
                        catch (error: Exception) { continuation.resumeWithException(error) }
                    } else continuation.resumeWithException(TaskDataException(TaskFailure.UNKNOWN))
                }
            }, false)
        }
    }

    override fun observe(uid: String): Flow<CloudTaskCollection> = callbackFlow {
        val ref = reference(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                try { trySend(snapshot(data)) } catch (error: Exception) { close(error) }
            }
            override fun onCancelled(error: DatabaseError) { close(failure(error)) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun save(uid: String, task: TaskItem) = request {
        require(task.id.matches(Regex("[A-Za-z0-9_-]+")))
        reference(uid).child("tasks").child(task.id).setValue(FirebaseTaskRecord.from(task)).await()
        Unit
    }

    override suspend fun delete(uid: String, id: String) = request {
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        reference(uid).child("tasks").child(id).removeValue().await()
        Unit
    }
}
