package vn.edu.uth.todo.model

data class GoogleSignInRequest(val id: Long)

/** Tokens are transient: never store this result in SavedStateHandle or logs. */
sealed interface GoogleSignInResult {
    class Token(val idToken: String) : GoogleSignInResult {
        override fun toString() = "GoogleSignInResult.Token([redacted])"
    }
    data class Failure(val message: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

object GoogleAuthMessages {
    const val NOT_CONFIGURED = "Đăng nhập Google chưa được cấu hình đầy đủ."
    const val NO_ACCOUNT = "Không tìm thấy tài khoản Google để đăng nhập. Hãy thêm tài khoản Google trên thiết bị rồi thử lại."
    const val PROVIDER_UNAVAILABLE = "Không thể mở đăng nhập Google. Hãy kiểm tra hoặc cập nhật Dịch vụ Google Play rồi thử lại."
    const val INVALID_RESPONSE = "Google chưa trả về thông tin đăng nhập hợp lệ. Vui lòng thử lại."
    const val NETWORK = "Không thể kết nối máy chủ. Hãy kiểm tra Internet rồi thử lại."
    const val ACCOUNT_CONFLICT = "Email này đang được dùng với phương thức đăng nhập khác. Hãy dùng phương thức đã đăng ký."
    const val TOO_MANY_REQUESTS = "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau."
    const val INVALID_CREDENTIAL = "Không thể xác thực tài khoản Google. Vui lòng thử lại."
    const val UNEXPECTED = "Đăng nhập Google chưa thành công. Vui lòng thử lại."
}
