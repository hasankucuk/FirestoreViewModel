package io.github.achmadhafid.sample_app

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import io.github.achmadhafid.firebase_auth_view_model.observeAuthState
import io.github.achmadhafid.firebase_auth_view_model.onSignedIn
import io.github.achmadhafid.firebase_auth_view_model.onSignedOut
import io.github.achmadhafid.firebase_auth_view_model.signOut
import io.github.achmadhafid.firebase_auth_view_model.signin.GoogleSignInExtensions
import io.github.achmadhafid.firebase_auth_view_model.signin.SignInState
import io.github.achmadhafid.firebase_auth_view_model.signin.observeGoogleSignIn
import io.github.achmadhafid.firebase_auth_view_model.signin.onSignInByGoogleResult
import io.github.achmadhafid.firebase_auth_view_model.signin.startGoogleSignInActivity
import io.github.achmadhafid.firestore_view_model.FirestoreViewModel
import io.github.achmadhafid.firestore_view_model.firestoreViewModel
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentException
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentState
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentException
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentState
import io.github.achmadhafid.sample_app.entity.User
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.zpack.ktx.bindView
import io.github.achmadhafid.zpack.ktx.d
import io.github.achmadhafid.zpack.ktx.hide
import io.github.achmadhafid.zpack.ktx.onSingleClick
import io.github.achmadhafid.zpack.ktx.setMaterialToolbar
import io.github.achmadhafid.zpack.ktx.show
import io.github.achmadhafid.zpack.ktx.stringRes
import io.github.achmadhafid.zpack.ktx.toastShort

class MainActivity : BaseActivity(R.layout.activity_main),
    SimplePref, GoogleSignInExtensions, FirestoreViewModel.Extension {

    //region Resource

    private val webClientId by stringRes(R.string.web_client_id)

    //endregion
    //region View Binding

    private val btnAuth: MaterialButton by bindView(R.id.btn_auth)
    private val btnUser: MaterialButton by bindView(R.id.btn_user)

    //endregion
    //region View Model

    private val viewModel by firestoreViewModel()

    //endregion

    //region Lifecycle Callback

    @Suppress("ComplexMethod", "LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //region setup action bar

        setMaterialToolbar(R.id.toolbar)

        //endregion
        //region observe auth state

        observeAuthState {
            onSignedIn {
                btnAuth.text = "Logout"
                btnAuth.onSingleClick { signOut() }
            }
            onSignedOut {
                btnAuth.text = "Login by Google"
                btnAuth.onSingleClick { startGoogleSignInActivity(webClientId, GOOGLE_SIGN_IN) }
            }
        }

        //endregion
        //region observe sign in flow

        observeGoogleSignIn {
            when (it.getState()) {
                is SignInState.OnFailed -> {
                    toastShort("Please try again")
                }
            }
        }

        //endregion
        //region Observe user readDocumentEvent

        viewModel.getDocument(User::class, "Users", MAX_SYNC_WAIT, true) {
            apply { id = it.id }
        }.observe(this, Observer {
            when (val state = it.getState()) {
                is ReadDocumentState.OnDataFound -> {
                    btnUser.show()
                    btnUser.text = "Delete user data"
                    btnUser.onSingleClick {
                        viewModel.deleteDocument(DELETE_USER, "Users")
                    }
                    d("User found: ${state.value}")
                    d("--- IS FROM CACHE : ${state.isFromCache} ---")
                    toastShort("User found: ${state.value}")
                }
                is ReadDocumentState.OnDataNotFound -> {
                    if (state.isSignedIn) {
                        btnUser.show()
                        btnUser.text = "Save user data"
                        btnUser.onSingleClick {
                            val user = User(phone = "01234567890", email = "myemail@email.com")
                            viewModel.setDocument(SAVE_USER, user, "Users", true)
                        }
                        d("--- User not found ---")
                        d("--- IS FROM CACHE : ${state.isFromCache} ---")
                        toastShort("User not found")
                    } else {
                        btnUser.hide()
                        toastShort("User logout")
                    }
                }
                is ReadDocumentState.OnError -> {
                    btnUser.hide()
                    val message = when (val error = state.exception) {
                        ReadDocumentException.Offline -> "No internet connection"
                        ReadDocumentException.Unauthenticated -> "Login required"
                        is ReadDocumentException.FirestoreException -> error.firestoreException.message
                    }
                    toastShort("User error: $message")
                }
            }
        })

        //endregion
        //region observe write document event

        viewModel.getRequest(SAVE_USER)
            .observe(this, Observer {
                val message = when (val state = it.getState()) {
                    WriteDocumentState.Empty -> return@Observer
                    WriteDocumentState.OnProgress -> "Please wait..."
                    is WriteDocumentState.OnSuccess -> "Success, User saved!"
                    is WriteDocumentState.OnFailed -> when (val exception = state.exception) {
                        WriteDocumentException.Unknown -> "Failed, Unknown error"
                        WriteDocumentException.Offline -> "Failed, no internet connection"
                        WriteDocumentException.Unauthenticated -> "Failed, please login first"
                        is WriteDocumentException.FirestoreException -> exception.firestoreException.message
                    }
                }
                toastShort("$message")
                d("=========== Save user: $message ===========")
            })

        viewModel.getRequest(DELETE_USER)
            .observe(this, Observer {
                val message = when (val state = it.getState()) {
                    WriteDocumentState.Empty -> return@Observer
                    WriteDocumentState.OnProgress -> "Please wait..."
                    is WriteDocumentState.OnSuccess -> "Success, User deleted!"
                    is WriteDocumentState.OnFailed -> when (val exception = state.exception) {
                        WriteDocumentException.Unknown -> "Failed, Unknown error"
                        WriteDocumentException.Offline -> "Failed, no internet connection"
                        WriteDocumentException.Unauthenticated -> "Failed, please login first"
                        is WriteDocumentException.FirestoreException -> exception.firestoreException.message
                    }
                }
                toastShort("$message")
                d("=========== Delete user: $message ===========")
            })

        //endregion
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onSignInByGoogleResult(resultCode, data)
    }

    //endregion

}

private const val SAVE_USER = 1001
private const val DELETE_USER = 1002

private const val GOOGLE_SIGN_IN = 2001

private const val MAX_SYNC_WAIT = 5 * 1000L
