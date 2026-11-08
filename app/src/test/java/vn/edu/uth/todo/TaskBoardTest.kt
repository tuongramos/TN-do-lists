package vn.edu.uth.todo

import java.io.*
import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.FigmaTaskRepository
import vn.edu.uth.todo.model.*

class TaskBoardTest {
    private val board = TaskBoard(FigmaTaskRepository().getContent().tasks)

    @Test fun listStartsWithTheSixTasksInTheProvidedDesign() {
        assertEquals(listOf("mobile", "chapter", "shopping", "meeting"), board.pending.map { it.id })
        assertEquals(listOf("exercise", "reading"), board.completed.map { it.id })
        assertEquals("2/3", board.find("mobile")!!.subtaskProgressLabel)
    }

    @Test fun categoryAndAccentInsensitiveSearchAreAppliedTogether() {
        assertEquals(listOf("chapter"), board.copy(query = "CAU TRUC").visibleItems.map { it.id })
        assertEquals(listOf("shopping"), board.copy(filter = TaskFilter.LIFE, query = "DI CHO").visibleItems.map { it.id })
        assertTrue(board.copy(filter = TaskFilter.STUDY, query = "DI CHO").visibleItems.isEmpty())
    }

    @Test fun completionMovesOnlyTheSelectedTaskToTheCompletedGroup() {
        val changed = board.update("chapter") { it.copy(completed = true) }
        assertEquals(3, changed.pending.size)
        assertEquals(listOf("chapter", "exercise", "reading"), changed.completed.map { it.id })
        assertFalse(board.find("chapter")!!.completed)
        assertEquals(board.items.filterNot { it.id == "chapter" }, changed.items.filterNot { it.id == "chapter" })
    }

    @Test fun savedTaskObjectsRetainEditsFiltersAndSubtaskProgress() {
        val changed = board.update("mobile") { task -> task.copy(notes = "Ghi chú đã sửa",
            subtasks = task.subtasks.map { it.copy(completed = true) }) }.copy(filter = TaskFilter.STUDY, query = "lap trinh")
        val bytes = ByteArrayOutputStream().also { output -> ObjectOutputStream(output).use { it.writeObject(changed) } }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as TaskBoard }
        assertEquals(changed, restored)
        assertEquals("3/3", restored.find("mobile")!!.subtaskProgressLabel)
        assertEquals(listOf("mobile"), restored.visibleItems.map { it.id })
    }

    @Test fun removingAllSubtasksProducesZeroProgress() {
        val task = board.find("mobile")!!.copy(subtasks = emptyList())
        assertEquals("0/0", task.subtaskProgressLabel)
        assertEquals(0f, task.subtaskProgress, 0f)
    }
}
