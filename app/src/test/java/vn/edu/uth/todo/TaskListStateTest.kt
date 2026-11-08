package vn.edu.uth.todo

import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.MockTaskApi
import vn.edu.uth.todo.data.TaskApiConfiguration
import vn.edu.uth.todo.model.*

class TaskListStateTest {
    private val sample = MockTaskApi(TaskApiConfiguration("test")).decode(
        javaClass.getResourceAsStream("/tasks-api-sample.json")!!.bufferedReader().use { it.readText() })

    @Test fun emptyCollectionIsDifferentFromLoadingFailureAndNoSearchMatches() {
        val empty = TaskBoard(emptyList())
        assertEquals(TaskListBody.LOADING, TaskListState(empty, TaskDataStatus(loading = true)).body)
        val failed = TaskDataStatus(warning = "Lỗi kết nối")
        assertEquals(TaskListBody.ERROR, TaskListState(empty, failed).body)
        assertEquals(TaskListBody.EMPTY, TaskListState(empty, TaskDataStatus(loaded = true)).body)
        val noMatches = TaskBoard(sample, query = "không có kết quả này")
        assertEquals(TaskListBody.NO_RESULTS, TaskListState(noMatches, TaskDataStatus(loaded = true)).body)
        assertEquals(TaskListBody.CONTENT, TaskListState(TaskBoard(sample), failed).body)
    }

    @Test fun filteringDoesNotChangeTheTotalPendingCountOrTheUnderlyingCollection() {
        val board = TaskBoard(sample, TaskFilter.LIFE, "di cho")
        val list = TaskListState(board, TaskDataStatus(loaded = true))
        assertEquals(4, list.pendingTotal)
        assertEquals(listOf("3"), list.pending.map { it.id })
        assertTrue(list.completed.isEmpty())
        assertEquals(6, board.items.size)
        val completed = TaskListState(board.update("3") { it.copy(completed = true) }, list.status)
        assertEquals(3, completed.pendingTotal)
        assertTrue(completed.pending.isEmpty())
        assertEquals(listOf("3"), completed.completed.map { it.id })
        assertFalse(board.find("3")!!.completed)
    }
}
