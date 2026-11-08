package vn.edu.uth.todo.model

import androidx.annotation.DrawableRes

/** Immutable value objects: screen content is supplied by the repository. */
data class AppBrand(
    val university: String,
    val product: String,
    val tagline: String,
    val version: String,
    @DrawableRes val splashLogo: Int,
    @DrawableRes val headerLogo: Int
)

data class Illustration(
    @DrawableRes val resource: Int,
    val description: String
)

data class OnboardingPage(
    val id: Int,
    val title: String,
    val description: String,
    val illustration: Illustration
)

data class OnboardingLabels(
    val skip: String,
    val next: String,
    val start: String,
    val completed: String,
    val progress: String
)

data class OnboardingContent(
    val brand: AppBrand,
    val pages: List<OnboardingPage>,
    val labels: OnboardingLabels
)

sealed interface AppScreen {
    data object Start : AppScreen
    data class Onboarding(val pageIndex: Int) : AppScreen
    data object Login : AppScreen
    data object Home : AppScreen
    data object Profile : AppScreen
    data object TaskList : AppScreen
    data class TaskDetail(val taskId: String) : AppScreen
}

data class OnboardingUiState(
    val screen: AppScreen = AppScreen.Start,
    val completed: Boolean = false
)

/** Navigation rules are independent of Android and of the Compose UI. */
class OnboardingNavigator(private val pageCount: Int) {
    init { require(pageCount > 0) }

    fun open(pageIndex: Int): AppScreen.Onboarding =
        AppScreen.Onboarding(pageIndex.coerceIn(0, pageCount - 1))

    fun next(screen: AppScreen): AppScreen = when (screen) {
        AppScreen.Start -> open(0)
        is AppScreen.Onboarding -> open(screen.pageIndex + 1)
        else -> screen
    }

    fun previous(screen: AppScreen): AppScreen = when (screen) {
        AppScreen.Start -> AppScreen.Start
        is AppScreen.Onboarding -> if (screen.pageIndex > 0) open(screen.pageIndex - 1) else AppScreen.Start
        else -> screen
    }

    fun skip(): AppScreen.Onboarding = open(pageCount - 1)
    fun isLast(screen: AppScreen): Boolean =
        screen is AppScreen.Onboarding && screen.pageIndex == pageCount - 1
}
