package vn.edu.uth.todo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.edu.uth.todo.R
import vn.edu.uth.todo.model.NotificationCopy

@Composable
fun NotificationPermissionDialog(onAllow: () -> Unit, onLater: () -> Unit) {
    Dialog(onDismissRequest = onLater) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(88.dp).background(TodoDesign.Blue.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.nav_bell), contentDescription = null,
                        modifier = Modifier.size(44.dp), colorFilter = ColorFilter.tint(TodoDesign.Blue))
                }
                Text(NotificationCopy.TITLE, color = TodoDesign.Ink,
                    style = figmaTextStyle(22, FontWeight.Bold, 28), textAlign = TextAlign.Center)
                Text(NotificationCopy.PROMPT, color = TodoDesign.Body,
                    style = figmaTextStyle(15, lineHeight = 22), textAlign = TextAlign.Center)
                Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(NotificationCopy.ALLOW, style = figmaTextStyle(15, FontWeight.SemiBold, 21),
                            textAlign = TextAlign.Center)
                    }
                    OutlinedButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(NotificationCopy.LATER, style = figmaTextStyle(15, FontWeight.SemiBold, 21),
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
