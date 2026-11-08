package vn.edu.uth.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.edu.uth.todo.model.TaskDataMessages
import vn.edu.uth.todo.model.TaskDataStatus

@Composable
fun BoxScope.TaskStatusBanner(status: TaskDataStatus, onRetry: () -> Unit) {
    val message = when {
        status.saving -> TaskDataMessages.SAVING
        status.loading -> TaskDataMessages.LOADING
        status.warning != null -> status.warning
        else -> return
    }
    Surface(color = if (status.warning != null) Color(0xFFFFF0E9) else Color(0xFFF0F3FF),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 20.dp, end = 20.dp)
            .widthIn(max = 450.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.testTag("task_data_status")) {
        Row(Modifier.defaultMinSize(minHeight = 38.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (status.loading || status.saving) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(message, style = figmaTextStyle(11, lineHeight = 14), maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (!status.loading && !status.saving && status.warning != null)
                TextButton(onClick = onRetry, modifier = Modifier.testTag("task_retry")) { Text(TaskDataMessages.RETRY, style = figmaTextStyle(11)) }
        }
    }
}
