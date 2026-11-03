package vn.edu.uth.todo.data

import java.util.Locale
import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.UserProfile

/** Shared mapping for Firebase's returned user and persisted Firebase session. */
object AuthUserMapper {
    fun map(id: String, displayName: String?, email: String?, provider: AuthProvider?): UserProfile {
        require(id.isNotBlank())
        val address = email.orEmpty().trim()
        val name = displayName.orEmpty().trim().replace(Regex("\\s+"), " ")
            .ifBlank { address.substringBefore('@').ifBlank { "Người dùng" } }
        val words = name.split(' ')
        val initials = if (words.size > 1) "${words.first().first()}${words.last().first()}"
            else name.take(2)
        return UserProfile(id, name, address, initials.uppercase(Locale.ROOT), provider, isGuest = false)
    }
}
