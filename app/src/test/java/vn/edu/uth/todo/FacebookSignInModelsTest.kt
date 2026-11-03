package vn.edu.uth.todo

import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.AuthUserMapper
import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.FacebookSignInResult

class FacebookSignInModelsTest {
    @Test fun accessTokenCannotLeakThroughStringRepresentation() {
        val result = FacebookSignInResult.Token("sensitive-test-value")
        assertFalse(result.toString().contains("sensitive-test-value"))
        assertTrue(result.toString().contains("[redacted]"))
    }

    @Test fun facebookCanAuthenticateWithoutSharingAnEmail() {
        val user = AuthUserMapper.map("facebook-firebase-uid", "Nguyễn Minh Bình", null, AuthProvider.FACEBOOK)
        assertEquals("Nguyễn Minh Bình", user.displayName)
        assertEquals("", user.email)
        assertEquals(AuthProvider.FACEBOOK, user.provider)
        assertFalse(user.isGuest)
    }
}
