package vn.edu.uth.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.uth.todo.model.AppScreen
import vn.edu.uth.todo.model.OnboardingNavigator

class OnboardingNavigatorTest {
    private val navigator = OnboardingNavigator(3)

    @Test fun nextTraversesExactlyThreePagesAndStopsAtTheLast() {
        var screen: AppScreen = AppScreen.Start
        (0..2).forEach { expected ->
            screen = navigator.next(screen)
            assertEquals(AppScreen.Onboarding(expected), screen)
        }
        assertEquals(screen, navigator.next(screen))
        assertTrue(navigator.isLast(screen))
    }

    @Test fun previousWalksBackToStartWithoutNegativePageIndices() {
        assertEquals(AppScreen.Onboarding(1), navigator.previous(AppScreen.Onboarding(2)))
        assertEquals(AppScreen.Onboarding(0), navigator.previous(AppScreen.Onboarding(1)))
        assertEquals(AppScreen.Start, navigator.previous(AppScreen.Onboarding(0)))
        assertEquals(AppScreen.Start, navigator.previous(AppScreen.Start))
    }

    @Test fun skipOpensTheFinalPageWithoutCreatingAnExtraScreen() {
        assertEquals(AppScreen.Onboarding(2), navigator.skip())
        assertTrue(navigator.isLast(navigator.skip()))
        assertFalse(navigator.isLast(AppScreen.Start))
        assertFalse(navigator.isLast(AppScreen.Onboarding(0)))
    }

    @Test fun restoredPageIndexIsAlwaysClampedToAValidPage() {
        assertEquals(AppScreen.Onboarding(0), navigator.open(-10))
        assertEquals(AppScreen.Onboarding(2), navigator.open(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPageListIsRejected() { OnboardingNavigator(0) }
}
