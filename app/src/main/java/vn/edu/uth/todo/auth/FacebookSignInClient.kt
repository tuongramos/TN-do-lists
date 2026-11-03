package vn.edu.uth.todo.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import vn.edu.uth.todo.model.FacebookAuthMessages
import vn.edu.uth.todo.model.FacebookSignInRequest
import vn.edu.uth.todo.model.FacebookSignInResult

/** Activity-scoped SDK bridge. Only a request ID is saved, never an access token. */
class FacebookSignInClient(
    activity: ComponentActivity,
    retainedRequestId: Long?,
    private val onResult: (Long, FacebookSignInResult) -> Unit
) {
    private val loginManager = LoginManager.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private var pendingId: Long? = activity.savedStateRegistry.consumeRestoredStateForKey(STATE_KEY)
        ?.takeIf { it.containsKey(REQUEST_ID) }?.getLong(REQUEST_ID)
        ?.takeIf { it == retainedRequestId }

    private val launcher = activity.registerForActivityResult(
        loginManager.createLogInActivityResultContract(callbackManager)
    ) { /* The SDK contract delivers through callbackManager. */ }

    init {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val token = result.accessToken
                finish(if (token.token.isBlank() || token.isExpired) {
                    FacebookSignInResult.Failure(FacebookAuthMessages.INVALID_RESPONSE)
                } else FacebookSignInResult.Token(token.token))
            }

            override fun onCancel() = finish(FacebookSignInResult.Cancelled)

            override fun onError(error: FacebookException) {
                // Provider errors can contain sensitive values; do not log the exception/token.
                finish(FacebookSignInResult.Failure(FacebookAuthMessages.UNEXPECTED))
            }
        })
        activity.savedStateRegistry.registerSavedStateProvider(STATE_KEY) {
            Bundle().apply { pendingId?.let { putLong(REQUEST_ID, it) } }
        }
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                loginManager.unregisterCallback(callbackManager)
            }
        })
    }

    fun launch(request: FacebookSignInRequest) {
        // Activity recreation reconnects the existing provider window instead of opening another.
        if (pendingId == request.id) return
        if (pendingId != null) return
        pendingId = request.id
        try {
            // Email is optional and is not enabled for this Meta app's use case.
            // Firebase authenticates the access token even when no email is shared.
            launcher.launch(listOf("public_profile"))
        } catch (_: Exception) {
            finish(FacebookSignInResult.Failure(FacebookAuthMessages.UNEXPECTED))
        }
    }

    private fun finish(result: FacebookSignInResult) {
        val id = pendingId ?: return
        pendingId = null
        onResult(id, result)
    }

    private companion object {
        const val STATE_KEY = "facebook_login_window"
        const val REQUEST_ID = "request_id"
    }
}
