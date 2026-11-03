package vn.edu.uth.todo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.edu.uth.todo.R
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.model.*

private val Surface = Color(0xFFF5F5F7)
private val StudyFill = Color(0xFFE6EDFF)
private val StudyInk = Color(0xFF1F4FD1)
private val LifeFill = Color(0xFFFFEBD9)
private val LifeInk = Color(0xFFA24A08)

private fun at(x: Float, y: Float, width: Float, height: Float = width) =
    Modifier.offset(x.dp, y.dp).size(width.dp, height.dp)

@Composable
private fun SvgText(
    text: String, x: Float, baseline: Float, size: Int,
    color: Color = TodoDesign.Ink, weight: Int = 400,
    lineHeight: Int = size, tracking: Float = 0f,
    decoration: TextDecoration? = null,
    maxWidth: Float? = null
) {
    val width = if (maxWidth == null) Modifier else Modifier.width(maxWidth.dp).clipToBounds()
    Text(text, color = color, style = figmaTextStyle(size, FontWeight(weight), lineHeight, tracking)
        .copy(textDecoration = decoration),
        softWrap = false, overflow = TextOverflow.Clip,
        modifier = Modifier.figmaBaseline(x, baseline).then(width))
}

@Composable
private fun Rect(x: Float, y: Float, w: Float, h: Float, radius: Int, color: Color) {
    Box(at(x, y, w, h).background(color, RoundedCornerShape(radius.dp)))
}

@Composable
private fun Asset(resource: Int, x: Float, y: Float, width: Float, height: Float = width, description: String? = null) {
    Image(painterResource(resource), description, at(x, y, width, height))
}

@Composable
fun LoginScreen(
    content: LoginContent,
    loading: Boolean,
    onSignIn: (AuthProvider) -> Unit,
    onGuest: () -> Unit,
    onBack: () -> Unit,
    onLegal: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.White).testTag("login_screen")) {
        FigmaViewport(DesignFrame.Onboarding) {
            Box(at(20f, 60f, 44f).clickable(role = Role.Button, onClick = onBack).testTag("login_back")) {
                Asset(R.drawable.auth_back, 8f, 10f, 24f)
            }
            Asset(R.drawable.logo_login, -2f, 171f, 148f)
            SvgText(content.title, 28f, 335.025f, 30, weight = 700, lineHeight = 38, tracking = -0.4f)
            SvgText(content.description, 28f, 408.88f, 16, TodoDesign.Body, lineHeight = 24)
            content.benefits.forEachIndexed { index, benefit ->
                Asset(benefit.icon, 28f, 467f + index * 28, 18f)
                SvgText(benefit.text, 56f, 481.145f + index * 28, 14, Color(0xFF3A3A3C))
            }
            content.options.forEachIndexed { index, option ->
                val facebook = option.provider == AuthProvider.FACEBOOK
                Box(
                    at(28f, 528f + index * 70, 334f, 58f)
                        .clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(16.dp))
                        .clickable(enabled = !loading, role = Role.Button, onClick = { onSignIn(option.provider) })
                        .testTag("login_${option.provider.name.lowercase()}")
                ) {
                    val symbolX = 55.5f
                    Box(at(symbolX, 16.5f, 25f)
                        .border(1.dp, Color(0xFFC7C7CC), RoundedCornerShape(11.5.dp)))
                    SvgText(option.symbol, symbolX + 7.76f, 33.41f, 12,
                        if (facebook) Color(0xFF1877F2) else TodoDesign.Body, 700)
                    SvgText(option.label, 93f, 35.247f, 17, weight = 600)
                }
            }
            if (loading) CircularProgressIndicator(
                Modifier.offset(350.dp, 547.dp).size(16.dp), strokeWidth = 2.dp)
            Rect(28f, 677.5f, 138.5f, 1f, 0, Color(0xFFE5E5EA))
            SvgText(content.divider, 178.5f, 682.778f, 13, TodoDesign.Muted)
            Rect(223.5f, 677.5f, 138.5f, 1f, 0, Color(0xFFE5E5EA))
            Box(at(28f, 706f, 334f, 44f).clickable(enabled = !loading, role = Role.Button, onClick = onGuest)
                .testTag("guest_button")) {
                SvgText(content.guestLabel, 48.9453f, 23.88f, 16, TodoDesign.Blue, 600)
            }
            Box(at(28f, 759f, 334f, 55f).clickable(onClick = onLegal).testTag("legal_button")) {
                SvgText(content.termsPrefix, 3.7667f, 21.91f, 12, TodoDesign.Muted)
                SvgText(content.terms, 165.837f, 21.91f, 12, Color(0xFF3A3A3C), decoration = TextDecoration.Underline)
                SvgText(content.termsJoin, 279.567f, 21.91f, 12, TodoDesign.Muted)
                SvgText(content.privacyFirstLine, 298.291f, 21.91f, 12, Color(0xFF3A3A3C), decoration = TextDecoration.Underline)
                SvgText(content.privacySecondLine, 65.7429f, 39.91f, 12, Color(0xFF3A3A3C), decoration = TextDecoration.Underline)
                SvgText(content.termsSuffix, 179.833f, 39.91f, 12, TodoDesign.Muted)
            }
        }
    }
}

@Composable
fun HomeScreen(
    content: DashboardContent,
    user: UserProfile,
    onProfile: () -> Unit,
    onAction: (HomeAction) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Surface).testTag("home_screen")) {
        FigmaViewport(DesignFrame.Onboarding) {
            Rect(20f, 61f, 48f, 48f, 24, StudyFill)
            SvgText(user.initials, 29.9219f, 91.247f, 17, StudyInk, 700)
            Box(at(20f, 61f, 48f).clickable(role = Role.Button, onClick = onProfile)
                .semantics { contentDescription = user.displayName }.testTag("home_avatar"))
            SvgText(content.date, 80f, 74.145f, 14, TodoDesign.Muted)
            SvgText(content.greetingFor(user),
                80f, 103.82f, 24, weight = 700, tracking = -0.3f, maxWidth = 238f)
            Box(at(326f, 63f, 44f).background(Color.White, CircleShape)
                .clickable(role = Role.Button, onClick = { onAction(HomeAction.NOTIFICATIONS) })) {
                Asset(R.drawable.home_bell, 11f, 10f, 24f)
            }
            Rect(20f, 130f, 350f, 121f, 22, TodoDesign.Blue)
            SvgText(content.progressTitle, 40f, 164.145f, 14, Color.White.copy(alpha = 0.9f))
            SvgText(content.progress.label, 40f, 200.055f, 26, Color.White, 700)
            SvgText(content.encouragement, 40f, 227.145f, 14, Color.White.copy(alpha = 0.9f))
            Canvas(at(278f, 154.5f, 72f)) {
                drawCircle(Color.White.copy(alpha = 0.28f))
                drawArc(Color.White, -90f, 360f * content.progress.percent / 100, useCenter = true)
                drawCircle(TodoDesign.Blue, 28.dp.toPx())
            }
            SvgText("${content.progress.percent}%", 296.625f, 196.012f, 15, Color.White, 700)
            content.categories.forEachIndexed { index, category ->
                val x = 20f + index * 181
                val study = category.category == TaskCategory.STUDY
                Rect(x, 271f, 169f, 123f, 18, Color.White)
                Rect(x + 16, 287f, 36f, 36f, 10, if (study) StudyFill else LifeFill)
                Asset(category.icon, x + 24, 295f, 20f)
                SvgText(category.title, x + 16, 348.012f, 15, weight = 600)
                SvgText(category.subtitle, x + 16, 374.778f, 13, TodoDesign.Muted)
            }
            SvgText(content.todayTitle, 20f, 439.983f, 19, weight = 700)
            Box(at(286f, 411f, 88f, 44f).clickable(role = Role.Button, onClick = { onAction(HomeAction.LIST) })) {
                SvgText(content.allTasks, 8f, 27.145f, 14, TodoDesign.Blue, 500)
            }
            content.tasks.forEachIndexed { index, task ->
                val category = content.categories.first { it.category == task.category }
                TaskCard(task, category, 464f + index * 76) { onAction(HomeAction.LIST) }
            }
            HomeNavigation(content.navigation, onProfile, onAction)
        }
    }
}

@Composable
private fun TaskCard(task: TodoTask, category: CategorySummary, y: Float, onClick: () -> Unit) {
    val study = task.category == TaskCategory.STUDY
    Box(at(20f, y, 350f, 66f).clip(RoundedCornerShape(16.dp)).background(Color.White)
        .clickable(onClick = onClick).testTag("task_${task.id}")) {
        Canvas(at(14f, 20f, 26f)) {
            if (task.completed) {
                drawCircle(TodoDesign.Blue)
                val check = Path().apply {
                    moveTo(8.917.dp.toPx(), 13.dp.toPx())
                    lineTo(11.834.dp.toPx(), 15.917.dp.toPx())
                    lineTo(17.667.dp.toPx(), 10.083.dp.toPx())
                }
                drawPath(check, Color.White, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round))
            } else drawCircle(Color(0xFFC7C7CC), radius = 12.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
        SvgText(task.title, 52f, 27.012f, 15,
            if (task.completed) Color(0xFF8A8A8E) else TodoDesign.Ink, 500,
            decoration = if (task.completed) TextDecoration.LineThrough else null, maxWidth = 254f)
        Rect(52f, 35f, if (study) 64f else 80f, 19f, 6, if (study) StudyFill else LifeFill)
        SvgText(category.title, 60f, 48.91f, 12, if (study) StudyInk else LifeInk, 600)
        SvgText(task.time, if (study) 124f else 140f, 48.91f, 12, TodoDesign.Muted)
        Asset(R.drawable.task_chevron, 321f, 27f, 12f)
    }
}

@Composable
private fun HomeNavigation(items: List<NavigationItem>, onProfile: () -> Unit, onAction: (HomeAction) -> Unit) {
    Rect(0f, 758f, 390f, 86f, 0, Color.White)
    Rect(0f, 758f, 390f, 1f, 0, Color(0xFFE5E5EA))
    val iconX = listOf(34.2f, 110.6f, 147f, 255.4f, 331.8f)
    val textX = listOf(18.9741f, 94.1973f, 0f, 238.144f, 323.47f)
    items.forEachIndexed { index, item ->
        if (item.action == HomeAction.ADD) {
            Asset(item.icon, 147f, 733f, 96f)
            Box(at(167f, 745f, 56f).clip(CircleShape).clickable(role = Role.Button,
                onClick = { onAction(item.action) }).semantics { contentDescription = item.label }.testTag("home_add"))
        } else {
            Asset(item.icon, iconX[index], 769.5f, 24f)
            SvgText(item.label, textX[index], 807.542f, 11,
                if (index == 0) TodoDesign.Blue else TodoDesign.Muted, if (index == 0) 600 else 500)
            Box(at(index * 76.4f + 8, 764f, 68f, 56f).clickable(role = Role.Tab,
                onClick = { if (item.action == HomeAction.PROFILE) onProfile() else onAction(item.action) })
                .semantics { contentDescription = item.label }.testTag("nav_${item.action.name.lowercase()}"))
        }
    }
}

@Composable
fun ProfileScreen(content: ProfileContent, loading: Boolean, onBack: () -> Unit, onLogout: () -> Unit, onAction: () -> Unit) {
    val user = content.user
    Box(Modifier.fillMaxSize().background(Surface).testTag("profile_screen")) {
        FigmaViewport(DesignFrame.Onboarding) {
            Asset(R.drawable.profile_back, 20f, 68f, 18f)
            SvgText(content.back, 42f, 82.88f, 16, TodoDesign.Blue)
            Box(at(16f, 55f, 104f, 44f).clickable(role = Role.Button, onClick = onBack).testTag("profile_back"))
            SvgText(content.title, 171.6f, 83.247f, 17, weight = 600)
            SvgText(content.edit, 340.422f, 82.88f, 16, TodoDesign.Blue, 600)
            Box(at(324f, 55f, 50f, 44f).clickable(role = Role.Button, onClick = onAction).testTag("profile_edit"))
            Rect(151f, 114f, 88f, 88f, 44, StudyFill)
            SvgText(user.initials, 168.5f, 169.76f, 32, StudyInk, 700)
            Box(at(208.5f, 171.5f, 33f).background(TodoDesign.Blue, CircleShape).border(3.dp, Surface, CircleShape)
                .clickable(role = Role.Button, onClick = onAction)) {
                Asset(R.drawable.profile_camera, 6.5f, 6.5f, 20f)
            }
            CenteredProfileText(user.displayName, 236.085f, 22, 700, TodoDesign.Ink)
            val emailText = if (user.provider == AuthProvider.FACEBOOK && user.email.isBlank())
                content.missingFacebookEmail else user.email
            CenteredProfileText(emailText, 264.145f, 14, 400, TodoDesign.Muted)
            val badge = if (user.isGuest) content.guestBadge else "${content.linkedPrefix} ${user.provider?.displayName.orEmpty()}"
            val badgeWidth = if (user.provider == AuthProvider.FACEBOOK) 228f else 208f
            val badgeLeft = (390f - badgeWidth) / 2
            Rect(badgeLeft, 276f, badgeWidth, 23f, 8, Color(0xFFDDF5EA))
            Asset(R.drawable.profile_check, badgeLeft + 9, 279f, 17f)
            SvgText(badge, badgeLeft + 29, 291.91f, 12, Color(0xFF0F7A4F), 600)
            content.statistics.forEachIndexed { index, statistic ->
                val x = 20f + 120 * index
                Rect(x, 319f, 110f, 75f, 16, Color.White)
                SvgText(statistic.value, x + 12, 355.085f, 22, weight = 700)
                SvgText(statistic.label, x + 12, 376.91f, 12, TodoDesign.Muted)
            }
            Rect(20f, 410f, 350f, 161f, 16, Color.White)
            SvgText(content.weekTitle, 36f, 441.012f, 15, weight = 600)
            SvgText(content.weeklyProgress, 288f, 440.778f, 13, TodoDesign.Muted)
            content.week.forEachIndexed { index, day ->
                val x = 42.2856f + index * 46.5714f
                val height = day.completed * 18f
                Rect(x, 535f - height, 26f, height, 6, when {
                    day.selected -> TodoDesign.Blue
                    index == 0 -> TodoDesign.Blue.copy(alpha = 0.35f)
                    else -> Color(0xFFE4E4EA)
                })
                SvgText(day.label, x + when (index) { 0,3,5 -> 5.5f; 1 -> 5f; 6 -> 4.5f; else -> 6f },
                    552.042f, 11, if (day.selected) TodoDesign.Ink else TodoDesign.Muted, if (day.selected) 700 else 500)
            }
            Rect(20f, 587f, 350f, 104f, 16, Color.White)
            Asset(R.drawable.profile_sync, 36f, 602.5f, 20f)
            SvgText(content.sync, 68f, 618.012f, 15)
            SvgText(content.lastSync, 290.39f, 617.645f, 14, TodoDesign.Muted)
            Asset(R.drawable.profile_lock, 36f, 655f, 20f)
            SvgText(content.privacy, 68f, 670.512f, 15)
            Asset(R.drawable.profile_chevron, 340.22f, 659f, 12f)
            Box(at(20f, 587f, 350f, 104f).clickable(onClick = onAction))
            Box(at(20f, 756f, 350f, 52f).clip(RoundedCornerShape(16.dp)).background(Color.White)
                .clickable(enabled = !loading, role = Role.Button, onClick = onLogout).testTag("logout_button")) {
                Asset(R.drawable.profile_logout, 120.5f, 17f, 18f)
                SvgText(content.logout, 146.617f, 31.88f, 16, Color(0xFFC4321F), 600)
            }
        }
    }
}

@Composable
private fun CenteredProfileText(text: String, baseline: Float, size: Int, weight: Int, color: Color) {
    // Full-width centering supports the real Firebase user's name and email later.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(text, color = color, style = figmaTextStyle(size, FontWeight(weight)), maxLines = 1,
            modifier = Modifier.figmaBaseline(0f, baseline))
    }
}

@Preview(name = "04 - Đăng nhập", widthDp = 390, heightDp = 844)
@Composable private fun LoginPreview() = TodoTheme {
    LoginScreen(FigmaTodoRepository().getContent().login, false, {}, {}, {}, {})
}

@Preview(name = "05 - Trang chủ", widthDp = 390, heightDp = 844)
@Composable private fun HomePreview() = TodoTheme {
    val content = FigmaTodoRepository().getContent()
    HomeScreen(content.dashboard, content.profile.user.copy(isGuest = true), {}, {})
}

@Preview(name = "06 - Hồ sơ", widthDp = 390, heightDp = 844)
@Composable private fun ProfilePreview() = TodoTheme {
    ProfileScreen(FigmaTodoRepository().getContent().profile, false, {}, {}, {})
}
