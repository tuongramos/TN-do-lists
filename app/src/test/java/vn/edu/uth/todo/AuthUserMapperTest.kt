package vn.edu.uth.todo

import org.junit.Assert.*
import org.junit.Test
import vn.edu.uth.todo.data.AuthUserMapper
import vn.edu.uth.todo.model.AuthProvider
import vn.edu.uth.todo.model.GoogleSignInResult

class AuthUserMapperTest {
    @Test fun realGoogleIdentityReplacesAllDemoIdentityFields() {
        val user = AuthUserMapper.map("firebase-uid", "Nguyễn Văn A", "student@example.test", AuthProvider.GOOGLE)
        assertEquals("firebase-uid", user.id)
        assertEquals("Nguyễn Văn A", user.displayName)
        assertEquals("student@example.test", user.email)
        assertEquals("NA", user.initials)
        assertEquals(AuthProvider.GOOGLE, user.provider)
        assertFalse(user.isGuest)
    }

    @Test fun displayNameWhitespaceIsNormalized() {
        val user = AuthUserMapper.map("id", "  Minh   Anh  ", " mail@example.test ", AuthProvider.GOOGLE)
        assertEquals("Minh Anh", user.displayName)
        assertEquals("MA", user.initials)
        assertEquals("mail@example.test", user.email)
    }

    @Test fun missingNameUsesEmailAndSingleWordInitials() {
        val user = AuthUserMapper.map("id", null, "an@example.test", AuthProvider.GOOGLE)
        assertEquals("an", user.displayName)
        assertEquals("AN", user.initials)
    }

    @Test fun missingOptionalProfileFieldsStillProducesAnAuthenticatedUser() {
        val user = AuthUserMapper.map("id", null, null, AuthProvider.GOOGLE)
        assertEquals("Người dùng", user.displayName)
        assertEquals("", user.email)
        assertFalse(user.isGuest)
    }

    @Test fun tokenStringRepresentationDoesNotExposeTheToken() {
        val token = GoogleSignInResult.Token("private-test-token")
        assertFalse(token.toString().contains("private-test-token"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingFirebaseUidCannotCreateAUser() {
        AuthUserMapper.map("", "Minh Anh", "email@example.test", AuthProvider.GOOGLE)
    }
}
