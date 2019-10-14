package io.github.achmadhafid.sample_app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import io.github.achmadhafid.firebase_auth_view_model.isSignedIn
import io.github.achmadhafid.firebase_auth_view_model.isSignedOut
import io.github.achmadhafid.firebase_auth_view_model.observeAuthState
import io.github.achmadhafid.firebase_auth_view_model.onAny
import io.github.achmadhafid.firebase_auth_view_model.signOut
import io.github.achmadhafid.firebase_auth_view_model.signin.GoogleSignInException
import io.github.achmadhafid.firebase_auth_view_model.signin.GoogleSignInExtensions
import io.github.achmadhafid.firebase_auth_view_model.signin.SignInState
import io.github.achmadhafid.firebase_auth_view_model.signin.observeGoogleSignIn
import io.github.achmadhafid.firebase_auth_view_model.signin.onSignInByGoogleResult
import io.github.achmadhafid.firebase_auth_view_model.signin.startGoogleSignInActivity
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.simplepref.simplePref
import io.github.achmadhafid.zpack.ktx.stringRes
import io.github.achmadhafid.zpack.ktx.toastShort
import io.github.achmadhafid.zpack.ktx.toggleTheme

abstract class BaseActivity(@LayoutRes layout: Int) : AppCompatActivity(layout),
    SimplePref, GoogleSignInExtensions {

    //region Preference

    private var appTheme: Int? by simplePref()

    //endregion
    //region Resource

    private val webClientId by stringRes(R.string.web_client_id)

    //endregion
    //region Lifecycle Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //region observe auth state

        observeAuthState {
            onAny {
                invalidateOptionsMenu()
            }
        }

        //endregion
        //region observe sign in flow

        observeGoogleSignIn {
            when (val state = it.getState()) {
                SignInState.OnProgress -> toastShort("Please wait")
                is SignInState.OnSuccess -> toastShort("Login success!")
                is SignInState.OnFailed -> {
                    val message = when (val error = state.exception) {
                        GoogleSignInException.Canceled -> return@observeGoogleSignIn
                        GoogleSignInException.Unknown -> "unknown error, please try again"
                        GoogleSignInException.Offline -> "No internet connection"
                        GoogleSignInException.Timeout -> "timeout, please try again"
                        is GoogleSignInException.WrappedApiException ->
                            error.apiException.message
                        is GoogleSignInException.WrappedFirebaseAuthException ->
                            error.firebaseAuthException.message
                    }
                    toastShort("Login failed: $message")
                }
            }
        }

        //endregion
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_action_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.forEach {
            if (it.itemId == R.id.action_sign_in) {
                it.isVisible = isSignedOut
            }
            if (it.itemId == R.id.action_sign_out) {
                it.isVisible = isSignedIn
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> {
                appTheme = toggleTheme()
                true
            }
            R.id.action_sign_in -> {
                startGoogleSignInActivity(webClientId, RC_GOOGLE_SIGN_IN)
                true
            }
            R.id.action_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onSignInByGoogleResult(resultCode, data)
    }

    //endregion

}

private const val RC_GOOGLE_SIGN_IN = 2001
