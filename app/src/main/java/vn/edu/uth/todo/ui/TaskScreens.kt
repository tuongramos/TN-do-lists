package vn.edu.uth.todo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import vn.edu.uth.todo.R
import vn.edu.uth.todo.data.FigmaTaskRepository
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.model.*

private val TaskSurface = Color(0xFFF5F5F7)
private val StudyBackground = Color(0xFFE6EDFF)
private val StudyText = Color(0xFF1F4FD1)
private val LifeBackground = Color(0xFFFFEBD9)
private val LifeText = Color(0xFFA24A08)
private val PriorityBackground = Color(0xFFFDE7E7)
private val PriorityText = Color(0xFFB42318)

private fun taskAt(x: Float, y: Float, width: Float, height: Float = width) =
    Modifier.offset(x.dp, y.dp).size(width.dp, height.dp)

@Composable
private fun TaskText(
    value: String, x: Float, baseline: Float, size: Int, color: Color = TodoDesign.Ink,
    weight: Int = 400, width: Float? = null, lineHeight: Int = size,
    tracking: Float = 0f, lines: Int = 1, decoration: TextDecoration? = null,
    align: TextAlign = TextAlign.Start
) {
    Text(value, color = color, style = figmaTextStyle(size, FontWeight(weight), lineHeight, tracking)
        .copy(textDecoration = decoration),
        softWrap = lines > 1, maxLines = lines, overflow = TextOverflow.Clip, textAlign = align,
        modifier = Modifier.figmaBaseline(x, baseline).then(
            if (width == null) Modifier else Modifier.width(width.dp).clipToBounds()))
}

@Composable
private fun TaskRect(x: Float, y: Float, width: Float, height: Float, radius: Int, color: Color) {
    Box(taskAt(x, y, width, height).background(color, RoundedCornerShape(radius.dp)))
}

@Composable
private fun TaskIcon(resource: Int, x: Float, y: Float, size: Float) {
    Image(painterResource(resource), null, taskAt(x, y, size))
}

@Composable
private fun TaskCheck(checked: Boolean, x: Float, y: Float, square: Boolean = false) {
    val dimension = if (square) 22f else 26f
    Canvas(taskAt(x, y, dimension)) {
        if (checked) {
            if (square) drawRoundRect(TodoDesign.Blue, cornerRadius = CornerRadius(6.dp.toPx()))
            else drawCircle(TodoDesign.Blue)
            val path = Path().apply {
                if (square) {
                    moveTo(7.5.dp.toPx(), 11.dp.toPx()); lineTo(10.dp.toPx(), 13.5.dp.toPx())
                    lineTo(15.dp.toPx(), 8.5.dp.toPx())
                } else {
                    moveTo(8.9165.dp.toPx(), 13.dp.toPx()); lineTo(11.8332.dp.toPx(), 15.917.dp.toPx())
                    lineTo(17.6665.dp.toPx(), 10.083.dp.toPx())
                }
            }
            drawPath(path, Color.White, style = Stroke((if (square) 1.6f else 1.75f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else if (square) {
            drawRoundRect(Color(0xFFC7C7CC), Offset(1.dp.toPx(), 1.dp.toPx()),
                Size(20.dp.toPx(), 20.dp.toPx()), CornerRadius(5.dp.toPx()), style = Stroke(2.dp.toPx()))
        } else drawCircle(Color(0xFFC7C7CC), radius = 12.dp.toPx(), style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun TaskListScreen(
    labels: TaskListLabels, board: TaskBoard, navigation: List<NavigationItem>,
    onQuery: (String) -> Unit, onFilter: (TaskFilter) -> Unit, onOpen: (String) -> Unit,
    onToggle: (String) -> Unit, onNavigate: (HomeAction) -> Unit,
    status: TaskDataStatus = TaskDataStatus(loaded = true), changesEnabled: Boolean = true,
    onRetry: () -> Unit = {}
) {
    val focus = LocalFocusManager.current
    val list = TaskListState(board, status)
    val scroll = rememberScrollState()
    LaunchedEffect(board.filter, board.query) { scroll.scrollTo(0) }
    Box(Modifier.fillMaxSize().background(TaskSurface).testTag("task_list_screen")) {
        FigmaViewport(DesignFrame.Onboarding) {
            TaskText(labels.title, 20f, 87.79f, 28, weight = 700, tracking = -0.4f)
            TaskText(labels.pendingCount.format(list.pendingTotal), 254f, 82.645f, 14, TodoDesign.Muted)
            Box(taskAt(20f, 111f, 350f, 44f).background(Color(0xFFE9E9EE), RoundedCornerShape(12.dp))) {
                TaskIcon(R.drawable.task_search, 12f, 13f, 18f)
                BasicTextField(board.query, onQuery, singleLine = true,
                    textStyle = figmaTextStyle(16).copy(color = TodoDesign.Ink), cursorBrush = SolidColor(TodoDesign.Blue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier.figmaBaseline(40f, 27.88f).width(278.dp).testTag("task_search"),
                    decorationBox = { inner -> Box {
                        if (board.query.isEmpty()) Text(labels.search, color = Color(0xFF7A7A80), style = figmaTextStyle(16))
                        inner()
                    } })
            }
            val positions = listOf(20f, 105f, 203f)
            val widths = listOf(77f, 90f, 108f)
            val textOffsets = listOf(16.3174f, 17.191f, 17.045f)
            TaskFilter.entries.forEachIndexed { index, filter ->
                val selected = filter == board.filter
                Box(taskAt(positions[index], 171f, widths[index], 36f)
                    .background(if (selected) TodoDesign.Ink else Color.White, RoundedCornerShape(18.dp))
                    .then(if (selected) Modifier else Modifier.border(1.dp, Color(0xFFDCDCE0), RoundedCornerShape(18.dp)))
                    .selectable(selected, role = Role.Tab, onClick = { onFilter(filter) }).testTag("task_filter_${filter.name.lowercase()}")) {
                    TaskText(filter.title, textOffsets[index], 23.145f, 14,
                        if (selected) Color.White else Color(0xFF3A3A3C), 600)
                }
            }
            val pending = list.pending
            val completed = list.completed
            val doneHeader = if (pending.isEmpty()) 11.778f else 47.778f + pending.size * 76
            val doneStart = doneHeader + 13.222f
            val bodyHeight = max(514f, doneStart + completed.size * 60)
            Box(taskAt(0f, 228f, 390f, 514f).verticalScroll(scroll).testTag("task_list_body")) {
                Box(Modifier.width(390.dp).height(bodyHeight.dp)) {
                    if (pending.isNotEmpty()) {
                        TaskText(labels.pending, 20f, 11.778f, 13, TodoDesign.Muted, 600, tracking = 0.5f)
                        pending.forEachIndexed { index, task ->
                            ListTaskCard(task, 25f + 76 * index, labels.urgent, { onOpen(task.id) }, { onToggle(task.id) },
                                changesEnabled && !status.loading && !status.saving)
                        }
                    }
                    if (completed.isNotEmpty()) {
                        TaskText(labels.completed, 20f, doneHeader, 13, TodoDesign.Muted, 600, tracking = 0.5f)
                        completed.forEachIndexed { index, task ->
                            ListTaskCard(task, doneStart + index * 60, labels.urgent, { onOpen(task.id) }, { onToggle(task.id) },
                                changesEnabled && !status.loading && !status.saving)
                        }
                    }
                    if (list.body != TaskListBody.CONTENT) {
                        Column(Modifier.offset(20.dp, 40.dp).width(350.dp).padding(horizontal = 12.dp)
                            .testTag("task_list_${list.body.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            if (list.body == TaskListBody.LOADING) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.height(12.dp))
                            }
                            val message = when (list.body) {
                                TaskListBody.LOADING -> TaskDataMessages.LOADING
                                TaskListBody.ERROR -> status.warning ?: TaskDataMessages.LOAD_ERROR
                                TaskListBody.EMPTY -> TaskListMessages.EMPTY
                                else -> labels.empty
                            }
                            Text(message, color = TodoDesign.Muted, style = figmaTextStyle(15, lineHeight = 22),
                                textAlign = TextAlign.Center)
                            if (list.body == TaskListBody.ERROR)
                                TextButton(onClick = onRetry, modifier = Modifier.testTag("task_retry")) {
                                    Text(TaskDataMessages.RETRY, style = figmaTextStyle(14, FontWeight.SemiBold))
                                }
                            if (list.body == TaskListBody.NO_RESULTS)
                                TextButton(onClick = { onQuery(""); onFilter(TaskFilter.ALL); focus.clearFocus() },
                                    modifier = Modifier.testTag("task_list_clear_filters")) {
                                    Text(TaskListMessages.CLEAR_FILTERS, style = figmaTextStyle(14, FontWeight.SemiBold))
                                }
                        }
                    }
                }
            }
            TaskNavigation(navigation, onNavigate)
        }
    }
}

@Composable
private fun ListTaskCard(task: TaskItem, y: Float, urgent: String, onOpen: () -> Unit, onToggle: () -> Unit, changesEnabled: Boolean) {
    val study = task.category == TaskCategory.STUDY
    Box(taskAt(20f, y, 350f, if (task.completed) 50f else 66f)
        .background(Color.White, RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onOpen)
        .testTag("list_task_${task.id}")) {
        val checkY = if (task.completed) 12f else 20f
        TaskCheck(task.completed, 14f, checkY)
        Box(taskAt(5f, checkY - 9f, 44f).toggleable(task.completed, enabled = changesEnabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = task.title }.testTag("task_toggle_${task.id}"))
        TaskText(task.title, 52f, if (task.completed) 30.512f else 27.012f, 15,
            if (task.completed) Color(0xFF8A8A8E) else TodoDesign.Ink,
            if (task.completed) 400 else 500, width = if (!task.completed && task.priority == TaskPriority.HIGH) 232f else 284f,
            decoration = if (task.completed) TextDecoration.LineThrough else null)
        if (!task.completed) {
            TaskRect(52f, 35f, if (study) 64f else 80f, 19f, 6, if (study) StudyBackground else LifeBackground)
            TaskText(task.category.title, 60f, 48.91f, 12, if (study) StudyText else LifeText, 600)
            TaskText(task.deadline, if (study) { if (task.id == "mobile") 123.51f else 124f } else 140f,
                48.91f, 12, TodoDesign.Muted)
            if (task.priority == TaskPriority.HIGH) {
                TaskRect(296f, 23f, 40f, 20f, 6, PriorityBackground)
                TaskText(urgent, 304f, 37.042f, 11, PriorityText, 600)
            }
        }
    }
}

@Composable
private fun TaskNavigation(items: List<NavigationItem>, onNavigate: (HomeAction) -> Unit) {
    TaskRect(0f, 758f, 390f, 86f, 0, Color.White)
    TaskRect(0f, 758f, 390f, 1f, 0, Color(0xFFE5E5EA))
    val iconX = listOf(34.2f, 110.6f, 147f, 255.4f, 331.8f)
    val textX = listOf(19.377f, 93.854f, 0f, 238.144f, 323.47f)
    items.forEachIndexed { index, item ->
        if (item.action == HomeAction.ADD) {
            TaskIcon(item.icon, 147f, 733f, 96f)
            Box(taskAt(167f, 745f, 56f).clip(CircleShape).clickable(role = Role.Button, onClick = { onNavigate(item.action) })
                .semantics { contentDescription = item.label }.testTag("tasks_nav_add"))
        } else {
            val icon = when (item.action) {
                HomeAction.HOME -> R.drawable.task_nav_home
                HomeAction.LIST -> R.drawable.task_nav_list
                else -> item.icon
            }
            TaskIcon(icon, iconX[index], 769.5f, 24f)
            TaskText(item.label, textX[index], 807.542f, 11,
                if (item.action == HomeAction.LIST) TodoDesign.Blue else TodoDesign.Muted,
                if (item.action == HomeAction.LIST) 600 else 500)
            Box(taskAt(index * 76.4f + 8, 764f, 68f, 56f).clickable(role = Role.Tab, onClick = { onNavigate(item.action) })
                .semantics { contentDescription = item.label }.testTag("tasks_nav_${item.action.name.lowercase()}"))
        }
    }
}

private data class TaskDraft(val title: String, val notes: String)
private data class SubtaskDraft(val id: String, val title: String)

@Composable
fun TaskDetailScreen(
    task: TaskItem, labels: TaskDetailLabels, onBack: () -> Unit, onToggle: () -> Unit,
    onSubtaskToggle: (String) -> Unit, onAddSubtask: (String) -> Unit,
    onEdit: (String, String) -> Unit, onDelete: () -> Unit,
    onSubtaskEdit: (String, String) -> Unit, onSubtaskDelete: (String) -> Unit,
    changesEnabled: Boolean = true
) {
    val focus = LocalFocusManager.current
    var newSubtask by rememberSaveable(task.id) { mutableStateOf("") }
    var draft by remember(task.id) { mutableStateOf<TaskDraft?>(null) }
    var subtaskDraft by remember(task.id) { mutableStateOf<SubtaskDraft?>(null) }
    var deleteRequested by remember(task.id) { mutableStateOf(false) }
    val subtaskHeight = 53f * task.subtasks.size + 52f
    val buttonY = 571f + subtaskHeight + 43f
    val frame = DesignFrame(390, (buttonY + 116f).roundToInt())
    Box(Modifier.fillMaxSize().background(TaskSurface).testTag("task_detail_screen")) {
        FigmaScrollableViewport(frame) {
            TaskIcon(R.drawable.profile_back, 20f, 68f, 18f)
            TaskText(labels.back, 42f, 82.88f, 16, TodoDesign.Blue)
            Box(taskAt(16f, 55f, 112f, 44f).clickable(role = Role.Button, onClick = onBack).testTag("task_detail_back"))
            TaskText(labels.edit, 339f, 82.88f, 16, TodoDesign.Blue, 600)
            Box(taskAt(324f, 55f, 50f, 44f).clickable(enabled = changesEnabled, role = Role.Button, onClick = { draft = TaskDraft(task.title, task.notes) })
                .testTag("task_edit"))
            val study = task.category == TaskCategory.STUDY
            val categoryWidth = if (study) 68f else 92f
            TaskRect(20f, 110f, categoryWidth, 23f, 8, if (study) StudyBackground else LifeBackground)
            TaskText(task.category.title, 30f, 125.91f, 12, if (study) StudyText else LifeText, 600)
            val priorityX = 20 + categoryWidth + 7.5098f
            val high = task.priority == TaskPriority.HIGH
            TaskRect(priorityX, 110f, if (high) 88f else 118f, 23f, 8,
                if (high) PriorityBackground else Color(0xFFE9E9EE))
            TaskText(task.priority.title, priorityX + 10, 125.91f, 12, if (high) PriorityText else TodoDesign.Muted, 600)
            TaskText(task.title, 20f, 169.555f, 26, weight = 700, width = 350f,
                lineHeight = 34, tracking = -0.3f, lines = 2)
            TaskRect(20f, 227f, 350f, 152f, 16, Color.White)
            val rowIcons = listOf(R.drawable.task_calendar, R.drawable.task_reminder, R.drawable.task_repeat)
            val rowLabels = listOf(labels.deadline, labels.reminder, labels.repetition)
            val rowValues = listOf(task.deadline, task.reminder, task.repetition)
            rowLabels.forEachIndexed { index, title ->
                TaskIcon(rowIcons[index], 36f, 242f + index * 51, 20f)
                TaskText(title, 68f, 257.512f + index * 51, 15, TodoDesign.Body)
                TaskText(rowValues[index], 68f, 257.512f + index * 51, 15, weight = 500,
                    width = 286f, align = TextAlign.End)
            }
            TaskText(labels.notes, 20f, 407.778f, 13, TodoDesign.Muted, 600)
            TaskRect(20f, 419f, 350f, 94f, 16, Color.White)
            TaskText(task.notes, 36f, 449.513f, 15, Color(0xFF3A3A3C), width = 318f, lineHeight = 22, lines = 3)
            TaskText(labels.subtasks, 20f, 541.778f, 13, TodoDesign.Muted, 600)
            TaskText(task.subtaskProgressLabel, 300f, 541.778f, 13, TodoDesign.Muted, 600,
                width = 70f, align = TextAlign.End)
            TaskRect(20f, 553f, 350f, 6f, 3, Color(0xFFE4E4EA))
            val progressWidth = 350f * (task.subtaskProgress * 100).roundToInt() / 100
            if (progressWidth > 0) TaskRect(20f, 553f, progressWidth, 6f, 3, TodoDesign.Blue)
            TaskRect(20f, 571f, 350f, subtaskHeight, 16, Color.White)
            task.subtasks.forEachIndexed { index, subtask ->
                val y = 586f + index * 53
                TaskCheck(subtask.completed, 36f, y, square = true)
                Box(taskAt(25f, y - 11, 44f).toggleable(subtask.completed, enabled = changesEnabled, role = Role.Checkbox, onValueChange = { onSubtaskToggle(subtask.id) })
                    .semantics { contentDescription = subtask.title }.testTag("subtask_toggle_${subtask.id}"))
                TaskText(subtask.title, 71f, 602.512f + index * 53, 15,
                    if (subtask.completed) Color(0xFF8A8A8E) else TodoDesign.Ink, width = 197f,
                    decoration = if (subtask.completed) TextDecoration.LineThrough else null)
                TaskIcon(R.drawable.task_edit, 287f, y + 2, 18f)
                Box(taskAt(274f, y - 11, 44f).clickable(enabled = changesEnabled, role = Role.Button,
                    onClick = { subtaskDraft = SubtaskDraft(subtask.id, subtask.title) })
                    .semantics { contentDescription = "${labels.edit} ${subtask.title}" }.testTag("subtask_edit_${subtask.id}"))
                TaskIcon(R.drawable.task_trash, 335f, y + 2, 18f)
                Box(taskAt(322f, y - 11, 44f).clickable(enabled = changesEnabled, role = Role.Button, onClick = { onSubtaskDelete(subtask.id) })
                    .semantics { contentDescription = "${labels.confirmDelete} ${subtask.title}" }.testTag("subtask_delete_${subtask.id}"))
            }
            val addY = 587.5f + task.subtasks.size * 53
            TaskIcon(R.drawable.task_plus, 36f, addY - 0.5f, 20f)
            BasicTextField(newSubtask, { newSubtask = it }, singleLine = true, enabled = changesEnabled,
                textStyle = figmaTextStyle(15).copy(color = TodoDesign.Ink), cursorBrush = SolidColor(TodoDesign.Blue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (changesEnabled && newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" }
                    focus.clearFocus()
                }),
                modifier = Modifier.figmaBaseline(68f, addY + 15.012f).width(218.dp).testTag("subtask_input"),
                decorationBox = { inner -> Box {
                    if (newSubtask.isEmpty()) Text(labels.newSubtask, color = Color(0xFF757575), style = figmaTextStyle(15))
                    inner()
                } })
            TaskText(labels.add, 310.185f, addY + 15.012f, 15,
                if (newSubtask.isBlank()) Color(0xFF8A8A8E) else TodoDesign.Blue, 600)
            Box(taskAt(300f, addY - 13.5f, 60f, 44f)
                .clickable(enabled = changesEnabled && newSubtask.isNotBlank(), role = Role.Button, onClick = {
                    onAddSubtask(newSubtask); newSubtask = ""
                }).testTag("subtask_add"))
            Box(taskAt(15f, buttonY, 350.66f, 56f).background(TodoDesign.Blue, RoundedCornerShape(16.dp))
                .clickable(enabled = changesEnabled, role = Role.Button, onClick = onToggle).testTag("task_complete"))
            TaskText(if (task.completed) labels.reopen else labels.complete, 15f, buttonY + 34.247f, 17,
                Color.White, 600, width = 350.66f, align = TextAlign.Center)
            TaskText(labels.delete, 0f, buttonY + 87.512f, 15, Color(0xFFC4321F), 500,
                width = 390f, align = TextAlign.Center)
            Box(taskAt(100f, buttonY + 62, 190f, 44f).clickable(enabled = changesEnabled, role = Role.Button, onClick = { deleteRequested = true })
                .testTag("task_delete"))
        }
    }
    draft?.let { current ->
        AlertDialog(onDismissRequest = { draft = null }, title = { Text(labels.edit) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(current.title, { draft = current.copy(title = it) }, label = { Text(labels.title) },
                    modifier = Modifier.testTag("task_edit_title"))
                OutlinedTextField(current.notes, { draft = current.copy(notes = it) }, label = { Text(labels.notes) },
                    modifier = Modifier.testTag("task_edit_notes"))
            }
        }, confirmButton = {
            TextButton(enabled = changesEnabled && current.title.isNotBlank() && current.title.trim().length <= 500,
                onClick = { onEdit(current.title, current.notes); draft = null }) { Text(labels.save) }
        }, dismissButton = { TextButton(onClick = { draft = null }) { Text(labels.cancel) } })
    }
    subtaskDraft?.let { current ->
        AlertDialog(onDismissRequest = { subtaskDraft = null }, title = { Text(labels.editSubtask) }, text = {
            OutlinedTextField(current.title, { subtaskDraft = current.copy(title = it) }, modifier = Modifier.testTag("subtask_edit_title"))
        }, confirmButton = {
            TextButton(enabled = changesEnabled && current.title.isNotBlank(), onClick = { onSubtaskEdit(current.id, current.title); subtaskDraft = null }) { Text(labels.save) }
        }, dismissButton = { TextButton(onClick = { subtaskDraft = null }) { Text(labels.cancel) } })
    }
    if (deleteRequested) AlertDialog(onDismissRequest = { deleteRequested = false },
        title = { Text(labels.deleteTitle) }, text = { Text(labels.deleteMessage) },
        confirmButton = { TextButton(enabled = changesEnabled, onClick = { deleteRequested = false; onDelete() }) { Text(labels.confirmDelete) } },
        dismissButton = { TextButton(onClick = { deleteRequested = false }) { Text(labels.cancel) } })
}

@Composable
fun TaskDetailUnavailableScreen(back: String, message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(TaskSurface).testTag("task_detail_unavailable")) {
        FigmaViewport(DesignFrame.Onboarding) {
            TaskIcon(R.drawable.profile_back, 20f, 68f, 18f)
            TaskText(back, 42f, 82.88f, 16, TodoDesign.Blue)
            Box(taskAt(16f, 55f, 112f, 44f).clickable(role = Role.Button, onClick = onBack).testTag("task_detail_back"))
            Column(Modifier.offset(20.dp, 210.dp).width(350.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = TodoDesign.Muted, style = figmaTextStyle(16, lineHeight = 24), textAlign = TextAlign.Center)
                TextButton(onClick = onRetry, modifier = Modifier.testTag("task_retry")) { Text(TaskDataMessages.RETRY) }
            }
        }
    }
}

@Preview(name = "07 - Danh sách", widthDp = 390, heightDp = 844)
@Composable private fun TaskListPreview() = TodoTheme {
    val content = FigmaTaskRepository().getContent()
    TaskListScreen(content.list, TaskBoard(content.tasks), FigmaTodoRepository().getContent().dashboard.navigation,
        {}, {}, {}, {}, {})
}

@Preview(name = "08 - Chi tiết", widthDp = 390, heightDp = 941)
@Composable private fun TaskDetailPreview() = TodoTheme {
    val content = FigmaTaskRepository().getContent()
    TaskDetailScreen(content.tasks.first(), content.detail, {}, {}, {}, {}, { _, _ -> }, {}, { _, _ -> }, {})
}
