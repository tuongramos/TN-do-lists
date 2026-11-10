package vn.edu.uth.todo.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat

interface NotificationSettings {
    fun isEnabled(owner: String): Boolean
    fun setEnabled(owner: String, enabled: Boolean)
    fun canPostNotifications(): Boolean
}

class DeviceNotificationSettings(context: Context) : NotificationSettings {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("uth_notifications", Context.MODE_PRIVATE)
    private val manager = NotificationManagerCompat.from(appContext)

    override fun isEnabled(owner: String): Boolean = preferences.getBoolean(owner, false)

    override fun setEnabled(owner: String, enabled: Boolean) {
        preferences.edit().putBoolean(owner, enabled).apply()
    }

    override fun canPostNotifications(): Boolean = manager.areNotificationsEnabled()
}
