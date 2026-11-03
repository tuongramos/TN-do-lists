package vn.edu.uth.todo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.FigmaTodoRepository
import vn.edu.uth.todo.data.UnconfiguredAuthRepository
import vn.edu.uth.todo.model.*

class AuthContractTest {
    private val content = FigmaTodoRepository().getContent()

    @Test fun loginOffersExactlyGoogleAndFacebook() {
        assertEquals(listOf(AuthProvider.GOOGLE, AuthProvider.FACEBOOK), content.login.options.map { it.provider })
    }

    @Test fun unconfiguredProvidersNeverReturnAnAuthenticatedUser() = runBlocking {
        val repository = UnconfiguredAuthRepository()
        AuthProvider.entries.forEach { provider ->
            val result = repository.signIn(provider)
            assertTrue(result is AuthResult.Failure)
            assertTrue((result as AuthResult.Failure).message.contains(provider.displayName))
        }
    }

    @Test fun dashboardKeepsTheExactFigmaFigures() {
        assertEquals("3/7 việc", content.dashboard.progress.label)
        assertEquals(43, content.dashboard.progress.percent)
        assertEquals(listOf(4, 3), content.dashboard.categories.map { it.remaining })
        assertEquals(listOf("09:00", "14:00", "17:30", "06:30"), content.dashboard.tasks.map { it.time })
    }

    @Test fun profileKeepsTheHistoricalStatisticsAndWeeklyChart() {
        assertEquals(listOf("128", "12 ngày", "86%"), content.profile.statistics.map { it.value })
        assertEquals(18, content.profile.week.sumOf { it.completed })
        assertEquals("18/22 việc", content.profile.weeklyProgress)
        assertEquals("T3", content.profile.week.single { it.selected }.label)
    }

    @Test fun guestIdentityAndAuthenticatedIdentityHaveDifferentGreetings() {
        val user = content.profile.user
        assertEquals("Chào UTH!", content.dashboard.greetingFor(user.copy(isGuest = true)))
        assertEquals("Chào Minh Anh!", content.dashboard.greetingFor(user))
    }

    @Test fun onboardingNavigatorDoesNotAdvanceIntoProtectedScreens() {
        val navigator = OnboardingNavigator(3)
        assertEquals(AppScreen.Home, navigator.next(AppScreen.Home))
        assertEquals(AppScreen.Login, navigator.previous(AppScreen.Login))
        assertFalse(navigator.isLast(AppScreen.Profile))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDailyProgressIsRejected() { DailyProgress(8, 7) }
}
