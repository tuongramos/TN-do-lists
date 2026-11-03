package vn.edu.uth.todo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.edu.uth.todo.data.FigmaOnboardingRepository
import vn.edu.uth.todo.model.AppBrand
import vn.edu.uth.todo.model.OnboardingContent

internal fun figmaTextStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Int = size,
    tracking: Float = 0f
) = TextStyle(
    fontFamily = TodoDesign.Font,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private fun brandText(brand: AppBrand, splash: Boolean): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = if (splash) FontWeight.ExtraBold else FontWeight.Bold)) {
        append(brand.university)
    }
    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = if (splash) Color.White else TodoDesign.Ink)) {
        append(" ")
    }
    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = if (splash) Color.White else TodoDesign.Blue)) {
        append(brand.product)
    }
}

@Composable
fun StartScreen(brand: AppBrand, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(TodoDesign.Blue).testTag("start_screen")) {
        FigmaViewport(DesignFrame.Splash) {
            Image(
                painter = painterResource(brand.splashLogo),
                contentDescription = "${brand.university} ${brand.product}",
                modifier = Modifier.offset(102.dp, 258.dp).size(196.dp)
                    .clickable(role = Role.Button, onClickLabel = "Tiếp tục", onClick = onContinue)
                    .testTag("splash_logo")
            )
            Text(
                text = remember(brand) { brandText(brand, splash = true) },
                color = Color.White,
                style = figmaTextStyle(40, tracking = -1f),
                modifier = Modifier.figmaBaseline(105f, 455.7f)
            )
            Text(
                text = brand.tagline,
                color = Color.White.copy(alpha = 0.9f),
                style = figmaTextStyle(15),
                modifier = Modifier.figmaBaseline(53.5f, 487.012f)
            )
            Box(Modifier.offset(140.dp, 771.dp).size(120.dp, 4.dp)
                .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(2.dp)))
            Box(Modifier.offset(140.dp, 771.dp).size(48.dp, 4.dp)
                .background(Color.White, RoundedCornerShape(2.dp)))
            Text(
                text = brand.version,
                color = Color.White.copy(alpha = 0.9f),
                style = figmaTextStyle(12),
                modifier = Modifier.figmaBaseline(156f, 800.91f)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    content: OnboardingContent,
    pageIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val page = content.pages[pageIndex]
    val last = pageIndex == content.pages.lastIndex
    Box(modifier.fillMaxSize().background(Color.White).testTag("onboarding_${page.id}")) {
        FigmaViewport(DesignFrame.Onboarding) {
            Image(
                painter = painterResource(content.brand.headerLogo),
                contentDescription = null,
                modifier = Modifier.offset(28.dp, 66.dp).size(32.dp)
            )
            Text(
                text = remember(content.brand) { brandText(content.brand, splash = false) },
                color = TodoDesign.Ink,
                style = figmaTextStyle(19, tracking = -0.3f),
                modifier = Modifier.figmaBaseline(70f, 88.983f)
            )
            // A 68 x 44 touch target surrounds the text without changing its SVG position.
            Box(
                modifier = Modifier.offset(294.dp, 60.dp).size(68.dp, 44.dp)
                    .clickable(role = Role.Button, onClick = onSkip).testTag("skip_button")
            ) {
                Text(
                    text = content.labels.skip,
                    color = TodoDesign.Muted,
                    style = figmaTextStyle(15, FontWeight.Medium),
                    modifier = Modifier.figmaBaseline(12f, 27.512f)
                )
            }
            Image(
                painter = painterResource(page.illustration.resource),
                contentDescription = page.illustration.description,
                modifier = Modifier.offset(55.dp, 171.dp).size(280.dp, 300.dp)
                    .testTag("page_illustration")
                    .pointerInput(pageIndex) {
                        var displacement = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { displacement = 0f },
                            onHorizontalDrag = { change, amount -> change.consume(); displacement += amount },
                            onDragEnd = {
                                val threshold = 48.dp.toPx()
                                when {
                                    displacement < -threshold && !last -> onPageSelected(pageIndex + 1)
                                    displacement > threshold && pageIndex > 0 -> onPageSelected(pageIndex - 1)
                                }
                            }
                        )
                    }
            )
            Text(
                text = page.title,
                color = TodoDesign.Ink,
                style = figmaTextStyle(28, FontWeight.Bold, lineHeight = 36, tracking = -0.4f),
                modifier = Modifier.figmaBaseline(28f, 566.29f).testTag("page_title")
            )
            Text(
                text = page.description,
                color = TodoDesign.Body,
                style = figmaTextStyle(16, lineHeight = 24),
                modifier = Modifier.figmaBaseline(28f, if (last) 603.88f else 639.88f)
            )
            PageIndicators(content, pageIndex, onPageSelected)
            val buttonText = if (last) content.labels.start else content.labels.next
            Box(
                modifier = Modifier.offset(28.dp, 748.dp)
                    .size(if (last) 334.dp else 334.92.dp, 56.dp)
                    .background(TodoDesign.Blue, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = if (last) onComplete else onNext)
                    .testTag("next_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    style = figmaTextStyle(17, FontWeight.SemiBold, lineHeight = 24)
                )
            }
        }
    }
}

@Composable
private fun PageIndicators(content: OnboardingContent, selected: Int, onSelect: (Int) -> Unit) {
    var left = 28
    content.pages.forEachIndexed { index, page ->
        val active = index == selected
        val width = if (active) 24 else 8
        // The small dots retain their original size; their touch area is 48 dp high.
        Box(
            Modifier.offset((left - 4).dp, 692.dp).size((width + 8).dp, 48.dp)
                .clickable(role = Role.Tab, onClick = { onSelect(index) })
                .semantics { contentDescription = "${content.labels.progress} ${page.id}/${content.pages.size}" }
                .testTag("page_dot_${page.id}")
        ) {
            Box(Modifier.offset(4.dp, 20.dp).size(width.dp, 8.dp)
                .background(if (active) TodoDesign.Blue else TodoDesign.Inactive, RoundedCornerShape(4.dp)))
        }
        left += width + 8
    }
}

@Preview(name = "00 - Start", widthDp = 400, heightDp = 860, showBackground = true)
@Composable
private fun StartPreview() = TodoTheme {
    StartScreen(FigmaOnboardingRepository().getContent().brand, {})
}

@Preview(name = "01 - Mọi việc", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FirstPreview() = PagePreview(0)

@Preview(name = "02 - Học tập và Cuộc sống", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SecondPreview() = PagePreview(1)

@Preview(name = "03 - Nhắc đúng lúc", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ThirdPreview() = PagePreview(2)

@Composable
private fun PagePreview(index: Int) = TodoTheme {
    OnboardingScreen(FigmaOnboardingRepository().getContent(), index, {}, {}, {}, {})
}
