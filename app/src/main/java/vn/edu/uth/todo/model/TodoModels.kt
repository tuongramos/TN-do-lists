package vn.edu.uth.todo.model

import androidx.annotation.DrawableRes

enum class AuthProvider(val displayName: String) { GOOGLE("Google"), FACEBOOK("Facebook") }

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val initials: String,
    val provider: AuthProvider? = null,
    val isGuest: Boolean = false
)

sealed interface AuthResult {
    data class Success(val user: UserProfile) : AuthResult
    data class Failure(val message: String) : AuthResult
    data object Cancelled : AuthResult
}

data class LoginBenefit(val text: String, @DrawableRes val icon: Int)
data class SignInOption(val provider: AuthProvider, val label: String, val symbol: String)
data class LoginContent(
    val title: String,
    val description: String,
    val benefits: List<LoginBenefit>,
    val options: List<SignInOption>,
    val divider: String,
    val guestLabel: String,
    val termsPrefix: String,
    val terms: String,
    val termsJoin: String,
    val privacyFirstLine: String,
    val privacySecondLine: String,
    val termsSuffix: String
)

enum class TaskCategory(val title: String) { STUDY("Học tập"), LIFE("Cuộc sống") }

data class CategorySummary(
    val category: TaskCategory,
    val title: String,
    val remaining: Int,
    @DrawableRes val icon: Int
) { val subtitle: String get() = "$remaining việc còn lại" }

data class TodoTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val time: String,
    val completed: Boolean
)

data class DailyProgress(val completed: Int, val total: Int) {
    init { require(total >= 0 && completed in 0..total) }
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
    val percent: Int get() = kotlin.math.round(fraction * 100).toInt()
    val label: String get() = "$completed/$total việc"
}

enum class HomeAction { HOME, LIST, ADD, NOTIFICATIONS, PROFILE }
data class NavigationItem(val action: HomeAction, val label: String, @DrawableRes val icon: Int)

data class DashboardContent(
    val date: String,
    val greeting: String,
    val greetingTemplate: String,
    val progressTitle: String,
    val encouragement: String,
    val progress: DailyProgress,
    val categories: List<CategorySummary>,
    val tasks: List<TodoTask>,
    val todayTitle: String,
    val allTasks: String,
    val navigation: List<NavigationItem>
) {
    fun greetingFor(user: UserProfile): String = if (user.isGuest) greeting else greetingTemplate.format(user.displayName)
}

data class ProfileStatistic(val value: String, val label: String)
data class WeekDay(val label: String, val completed: Int, val selected: Boolean = false)
data class ProfileContent(
    val user: UserProfile,
    val back: String,
    val title: String,
    val edit: String,
    val linkedPrefix: String,
    val guestBadge: String,
    val statistics: List<ProfileStatistic>,
    val weekTitle: String,
    val weeklyProgress: String,
    val week: List<WeekDay>,
    val sync: String,
    val lastSync: String,
    val privacy: String,
    val logout: String,
    val missingFacebookEmail: String
)

data class AppLabels(
    val loginErrorTitle: String,
    val acknowledge: String,
    val signedIn: String,
    val signedOut: String,
    val guestMessage: String,
    val featurePending: String,
    val unexpectedError: String
)

data class TodoContent(
    val login: LoginContent,
    val dashboard: DashboardContent,
    val profile: ProfileContent,
    val labels: AppLabels
)

data class UiNotice(val title: String, val message: String)
data class TodoUiState(
    val screen: AppScreen = AppScreen.Start,
    val session: UserProfile? = null,
    val loading: Boolean = false,
    val notice: UiNotice? = null,
    val announcement: String? = null,
    val googleSignInRequest: GoogleSignInRequest? = null,
    val facebookSignInRequest: FacebookSignInRequest? = null,
    val taskBoard: TaskBoard = TaskBoard(emptyList()),
    val taskStatus: TaskDataStatus = TaskDataStatus(),
    val detailStatus: TaskDetailStatus = TaskDetailStatus(),
    val showNotificationPrompt: Boolean = false
)
