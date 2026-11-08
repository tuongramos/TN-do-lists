package vn.edu.uth.todo.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import vn.edu.uth.todo.model.TaskItem

interface TaskCache {
    fun read(owner: String): List<TaskItem>?
    fun write(owner: String, tasks: List<TaskItem>)
}

class SharedPreferencesTaskCache(context: Context, private val gson: Gson = Gson()) : TaskCache {
    private val preferences = context.applicationContext.getSharedPreferences("uth_tasks_v1", Context.MODE_PRIVATE)
    private fun key(owner: String) = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray())
        .joinToString("") { "%02x".format(it) }

    override fun read(owner: String): List<TaskItem>? = preferences.getString(key(owner), null)?.let { json ->
        runCatching {
            gson.fromJson<List<FirebaseTaskRecord>>(json, object : TypeToken<List<FirebaseTaskRecord>>() {}.type)
                .map { it.toDomain() }
        }.getOrNull()
    }

    override fun write(owner: String, tasks: List<TaskItem>) {
        preferences.edit().putString(key(owner), gson.toJson(tasks.map(FirebaseTaskRecord::from))).apply()
    }
}
