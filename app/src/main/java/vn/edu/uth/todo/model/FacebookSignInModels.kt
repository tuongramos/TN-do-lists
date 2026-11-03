package vn.edu.uth.todo.model

data class FacebookSignInRequest(val id: Long)

sealed interface FacebookSignInResult {
    class Token(val accessToken: String) : FacebookSignInResult {
        override fun toString() = "FacebookSignInResult.Token([redacted])"
    }
    data class Failure(val message: String) : FacebookSignInResult
    data object Cancelled : FacebookSignInResult
}

object FacebookAuthMessages {
    const val NOT_CONFIGURED = "Đăng nhập Facebook chưa được cấu hình đầy đủ."
    const val INVALID_RESPONSE = "Facebook chưa trả về thông tin đăng nhập hợp lệ. Vui lòng thử lại."
    const val INVALID_CREDENTIAL = "Không thể xác thực tài khoản Facebook. Vui lòng đăng nhập lại."
    const val UNEXPECTED = "Đăng nhập Facebook chưa thành công. Vui lòng thử lại."
    const val PROVIDER_DISABLED = "Phương thức đăng nhập này chưa được bật trong Firebase Authentication."
}
