package vn.edu.uth.todo.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TaskDates {
    private fun format(pattern: String, locale: Locale = Locale.ROOT) = SimpleDateFormat(pattern, locale).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    }.format(Date())
    fun isIsoDate(value: String): Boolean = value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
        runCatching { format("yyyy-MM-dd").parse(value) != null }.getOrDefault(false)
    fun shortDate(value: String): String = runCatching { format("dd/MM").format(format("yyyy-MM-dd").parse(value)!!) }.getOrDefault(value)
    fun heading(value: String): String = runCatching {
        format("EEEE, d 'tháng' M", Locale("vi", "VN")).format(format("yyyy-MM-dd").parse(value)!!)
            .replaceFirstChar { it.titlecase(Locale("vi", "VN")) }
    }.getOrDefault(value)
    fun displayDate(value: String): String {
        if (!isIsoDate(value)) return value
        return if (value == todayIso()) "Hôm nay" else format("dd/MM/yyyy").format(format("yyyy-MM-dd").parse(value)!!)
    }
    fun weekOf(value: String): List<String> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = format("yyyy-MM-dd").parse(value) ?: Date()
            add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
        }
        return (0..6).map { index ->
            format("yyyy-MM-dd").format(calendar.time).also { if (index < 6) calendar.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }
}

data class TaskStatistics(
    val date: String,
    val isToday: Boolean,
    val dayTasks: List<TaskItem>,
    val completedInDay: Int,
    val remainingStudy: Int,
    val remainingLife: Int,
    val completedTotal: Int,
    val remainingTotal: Int,
    val completionPercent: Int,
    val week: List<WeekDay>
)

object TaskStatisticsCalculator {
    fun calculate(tasks: List<TaskItem>, today: String = TaskDates.todayIso()): TaskStatistics {
        // The teaching API has fixed historical dates. Show their actual date instead of calling it today.
        val dates = tasks.map { it.date }.filter(TaskDates::isIsoDate)
        val selected = if (today in dates || dates.isEmpty()) today else dates.minOrNull()!!
        val dayTasks = tasks.filter { it.date == selected || (!TaskDates.isIsoDate(it.date) && it.date == "Hôm nay") }
            .sortedWith(compareBy<TaskItem> { it.time }.thenBy { it.sortOrder })
        val complete = tasks.count { it.completed }
        val week = TaskDates.weekOf(selected).mapIndexed { index, day ->
            WeekDay(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")[index],
                tasks.count { it.date == day && it.completed }, day == selected)
        }
        return TaskStatistics(selected, selected == today, dayTasks, dayTasks.count { it.completed },
            tasks.count { it.category == TaskCategory.STUDY && !it.completed },
            tasks.count { it.category == TaskCategory.LIFE && !it.completed }, complete, tasks.size - complete,
            if (tasks.isEmpty()) 0 else (complete * 100f / tasks.size).toInt(), week)
    }
}
