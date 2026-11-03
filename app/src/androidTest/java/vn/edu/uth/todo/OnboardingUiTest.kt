package vn.edu.uth.todo

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import vn.edu.uth.todo.data.FigmaOnboardingRepository
import vn.edu.uth.todo.ui.OnboardingScreen
import vn.edu.uth.todo.ui.StartScreen
import vn.edu.uth.todo.ui.TodoTheme

class OnboardingUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val content = FigmaOnboardingRepository().getContent()

    @Test fun actualAppKeepsItsPageAfterActivityRecreationAndSupportsBack() {
        compose.onNodeWithTag("splash_logo").performClick()
        compose.onNodeWithTag("onboarding_1").assertIsDisplayed()
        compose.onNodeWithTag("next_button").performClick()
        compose.onNodeWithTag("onboarding_2").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("onboarding_2").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("onboarding_1").assertIsDisplayed()
        compose.onNodeWithTag("skip_button").performClick()
        compose.onNodeWithTag("onboarding_3").assertIsDisplayed()
    }

    @Test fun navigationButtonsDotsSwipeSkipAndCompletionWork() {
        var completions = 0
        setTestContent {
            TodoTheme {
                val selected = remember { mutableIntStateOf(0) }
                OnboardingScreen(
                    content, selected.intValue,
                    onNext = { selected.intValue++ },
                    onSkip = { selected.intValue = content.pages.lastIndex },
                    onPageSelected = { selected.intValue = it },
                    onComplete = { completions++ }
                )
            }
        }
        compose.onNodeWithTag("onboarding_1").assertIsDisplayed()
        compose.onNodeWithTag("next_button").performClick()
        compose.onNodeWithTag("onboarding_2").assertIsDisplayed()
        compose.onNodeWithTag("page_dot_1").performClick()
        compose.onNodeWithTag("onboarding_1").assertIsDisplayed()
        compose.onNodeWithTag("page_illustration").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("onboarding_2").assertIsDisplayed()
        compose.onNodeWithTag("skip_button").performClick()
        compose.onNodeWithTag("onboarding_3").assertIsDisplayed()
        compose.onNodeWithTag("next_button").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
        compose.onNodeWithTag("onboarding_3").assertIsDisplayed()
    }

    @Test fun captureStart() {
        setTestContent { TodoTheme { StartScreen(content.brand, {}) } }
        capture("start_screen", "start")
    }

    @Test fun captureFirst() = capturePage(0)
    @Test fun captureSecond() = capturePage(1)
    @Test fun captureThird() = capturePage(2)

    private fun capturePage(index: Int) {
        setTestContent {
            TodoTheme { OnboardingScreen(content, index, {}, {}, {}, {}) }
        }
        capture("onboarding_${index + 1}", "onboarding_${index + 1}")
    }

    private fun capture(tag: String, fileName: String) {
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$fileName.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        compose.runOnUiThread { compose.activity.setContent(content = content) }
    }
}
