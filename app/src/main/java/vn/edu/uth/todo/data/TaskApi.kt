package vn.edu.uth.todo.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import vn.edu.uth.todo.model.*

data class ApiSubtaskDto(val title: String? = null, val done: Boolean? = null)
data class ApiTaskDto(
    val id: Long? = null, val title: String? = null, val note: String? = null,
    val category: String? = null, val date: String? = null, val time: String? = null,
    val priority: String? = null, val remind: Boolean? = null, val done: Boolean? = null,
    val subtasks: List<ApiSubtaskDto>? = null
)

class TaskApiConfiguration(val token: String, val endpoint: String = "https://share.tryapps.store/mock/teach/tasks") {
    override fun toString() = "TaskApiConfiguration(endpoint=$endpoint, token=[redacted])"
}

data class TaskHttpResponse(val status: Int, val body: String)
enum class TaskHttpMethod { GET, PATCH, DELETE }
class TaskHttpRequest(val configuration: TaskApiConfiguration, val method: TaskHttpMethod, val body: String? = null) {
    override fun toString() = "TaskHttpRequest(method=$method, configuration=$configuration)"
}
fun interface TaskHttpTransport { suspend fun execute(request: TaskHttpRequest): TaskHttpResponse }
fun interface TaskApi {
    suspend fun fetch(): List<TaskItem>
    suspend fun fetchTask(id: String): TaskItem = fetch().firstOrNull { it.id == id }
        ?: throw TaskDataException(TaskFailure.NOT_FOUND)
    suspend fun updateTask(task: TaskItem) { throw TaskDataException(TaskFailure.CONFIGURATION) }
    suspend fun deleteTask(id: String) { throw TaskDataException(TaskFailure.CONFIGURATION) }
}

data class ApiTaskUpdate(
    val title: String, val note: String, val category: String, val date: String, val time: String,
    val priority: String, val remind: Boolean, val done: Boolean, val subtasks: List<ApiSubtaskDto>
) {
    companion object {
        fun from(task: TaskItem) = ApiTaskUpdate(task.title, task.notes,
            if (task.category == TaskCategory.STUDY) "study" else "life", task.date, task.time,
            when (task.priority) { TaskPriority.HIGH -> "high"; TaskPriority.LOW -> "low"; else -> "medium" },
            task.remind, task.completed, task.subtasks.map { ApiSubtaskDto(it.title, it.completed) })
    }
}

class UrlConnectionTaskTransport : TaskHttpTransport {
    override suspend fun execute(request: TaskHttpRequest): TaskHttpResponse = withContext(Dispatchers.IO) {
        val url = URL(request.configuration.endpoint)
        require(url.protocol == "https")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method.name
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${request.configuration.token}")
            request.body?.let { body ->
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            TaskHttpResponse(status, body)
        } finally {
            connection.disconnect()
        }
    }
}

class MockTaskApi(
    private val configuration: TaskApiConfiguration,
    private val transport: TaskHttpTransport = UrlConnectionTaskTransport(),
    private val gson: Gson = Gson()
) : TaskApi {
    private suspend fun request(method: TaskHttpMethod, id: String? = null, body: String? = null): TaskHttpResponse {
        if (configuration.token.isBlank()) throw TaskDataException(TaskFailure.CONFIGURATION)
        if (id != null && !id.matches(Regex("[1-9][0-9]*"))) throw TaskDataException(TaskFailure.NOT_FOUND)
        val target = if (id == null) configuration else
            TaskApiConfiguration(configuration.token, "${configuration.endpoint.trimEnd('/')}/$id")
        val response = try {
            transport.execute(TaskHttpRequest(target, method, body))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            throw TaskDataException(TaskFailure.NETWORK, error)
        }
        when (response.status) {
            401, 403 -> throw TaskDataException(TaskFailure.AUTHENTICATION)
            404 -> if (method != TaskHttpMethod.DELETE) throw TaskDataException(TaskFailure.NOT_FOUND)
            in 500..599 -> throw TaskDataException(TaskFailure.SERVER)
            in 200..299 -> Unit
            else -> throw TaskDataException(TaskFailure.INVALID_RESPONSE)
        }
        return response
    }

    override suspend fun fetch(): List<TaskItem> = decode(request(TaskHttpMethod.GET).body)

    override suspend fun fetchTask(id: String): TaskItem {
        val response = request(TaskHttpMethod.GET, id)
        val task = try {
            val root = JsonParser.parseString(response.body)
            require(root.isJsonObject)
            decode("[$root]").single().also { require(it.id == id) }
        } catch (error: Exception) { throw TaskDataException(TaskFailure.INVALID_RESPONSE, error) }
        return task
    }

    override suspend fun updateTask(task: TaskItem) {
        request(TaskHttpMethod.PATCH, task.id, gson.toJson(ApiTaskUpdate.from(task)))
        // The teaching mock returns a fixed sample (possibly another ID). Only HTTP is the ACK;
        // keep the requested task and persist it to Firebase instead of replacing it with that sample.
    }

    override suspend fun deleteTask(id: String) {
        request(TaskHttpMethod.DELETE, id) // 404 is also safe when retrying an already-applied delete.
    }

    fun decode(json: String): List<TaskItem> = try {
        val root = JsonParser.parseString(json)
        require(root.isJsonArray)
        root.asJsonArray.mapIndexed { index, element ->
            val dto = gson.fromJson(element, ApiTaskDto::class.java)
            require(dto.id != null && dto.id > 0 && !dto.title.isNullOrBlank() && dto.title.length <= 500)
            require(TaskDates.isIsoDate(dto.date.orEmpty()))
            require(dto.time?.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")) == true)
            require(dto.done != null && dto.remind != null)
            val category = when (dto.category) { "study" -> TaskCategory.STUDY; "life" -> TaskCategory.LIFE; else -> error("category") }
            val priority = when (dto.priority) {
                "low" -> TaskPriority.LOW; "medium" -> TaskPriority.MEDIUM; "high" -> TaskPriority.HIGH; else -> error("priority")
            }
            TaskItem(dto.id.toString(), dto.title.trim(), category, dto.date!!, dto.time, dto.done, priority,
                dto.note.orEmpty(), if (dto.remind) "Trước 15 phút" else "Không", "Không",
                dto.subtasks.orEmpty().mapIndexed { subIndex, subtask ->
                    require(!subtask.title.isNullOrBlank() && subtask.done != null)
                    Subtask("${dto.id}_$subIndex", subtask.title.trim(), subtask.done)
                }, remind = dto.remind, sortOrder = index)
        }.also { tasks -> require(tasks.map { it.id }.distinct().size == tasks.size) }
    } catch (error: Exception) {
        throw TaskDataException(TaskFailure.INVALID_RESPONSE, error)
    }
}
