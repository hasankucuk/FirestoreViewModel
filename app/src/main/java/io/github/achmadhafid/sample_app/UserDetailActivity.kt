package io.github.achmadhafid.sample_app

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.observe
import com.google.android.material.button.MaterialButton
import io.github.achmadhafid.firestore_view_model.FirestoreViewModel
import io.github.achmadhafid.firestore_view_model.firestoreViewModel
import io.github.achmadhafid.firestore_view_model.isOffline
import io.github.achmadhafid.firestore_view_model.isUnauthenticated
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentState
import io.github.achmadhafid.firestore_view_model.read.getDocumentByAuthUid
import io.github.achmadhafid.firestore_view_model.read.withDataBuilder
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentState
import io.github.achmadhafid.lottie_dialog.isValidInput
import io.github.achmadhafid.lottie_dialog.lottieInputDialog
import io.github.achmadhafid.lottie_dialog.model.LottieDialogInput
import io.github.achmadhafid.lottie_dialog.model.LottieDialogType
import io.github.achmadhafid.lottie_dialog.onInvalidInput
import io.github.achmadhafid.lottie_dialog.onValidInput
import io.github.achmadhafid.lottie_dialog.withContent
import io.github.achmadhafid.lottie_dialog.withInputSpec
import io.github.achmadhafid.lottie_dialog.withTitle
import io.github.achmadhafid.sample_app.entity.User
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.simplepref.simplePref
import io.github.achmadhafid.zpack.ktx.bindView
import io.github.achmadhafid.zpack.ktx.hide
import io.github.achmadhafid.zpack.ktx.onSingleClick
import io.github.achmadhafid.zpack.ktx.setMaterialToolbar
import io.github.achmadhafid.zpack.ktx.show
import io.github.achmadhafid.zpack.ktx.toastShort

class UserDetailActivity : BaseActivity(R.layout.activity_user_detail),
    FragmentManager.OnBackStackChangedListener, FirestoreViewModel.Extension, SimplePref {

    //region Preference

    private var requireOnline by simplePref("require_online") { true }
    private var syncWait by simplePref("sync_wait") { 0L }

    //endregion
    //region View

    private val tvMessage: TextView by bindView(R.id.tv_message)
    private val btnAction: MaterialButton by bindView(R.id.btn_action)

    //endregion
    //region View Model

    private val viewModel by firestoreViewModel()

    //endregion
    //region Lifecycle Callback

    @Suppress("ComplexMethod", "LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //region Setup toolbar

        setMaterialToolbar(R.id.toolbar)

        //endregion
        //region Observe user read document event

        viewModel.getDocumentByAuthUid<User> {
            collection    = "Users"
            requireOnline = this@UserDetailActivity.requireOnline
            syncWait      = this@UserDetailActivity.syncWait
            withDataBuilder { snapshot ->
                id = snapshot.id
            }
        }.observe(this) {
            when (val state = it.getState()) {
                is ReadDocumentState.OnDataFound -> {
                    tvMessage.text = "Hello, ${state.value.name}"
                    btnAction.apply {
                        text = "Delete user data"
                        onSingleClick {
                            viewModel.deleteDocument(
                                RC_DELETE_USER,
                                "Users",
                                true
                            )
                        }
                        show()
                    }
                }
                is ReadDocumentState.OnDataNotFound -> {
                    if (state.isAuthenticated) {
                        tvMessage.text = "User not found"
                        btnAction.apply {
                            text = "Save user data"
                            onSingleClick {
                                //region show input name dialog
                                lottieInputDialog {
                                    type = LottieDialogType.BOTTOM_SHEET
                                    withTitle("Your name?")
                                    withContent("Please enter your name here")
                                    withInputSpec {
                                        inputType = LottieDialogInput.Type.TEXT
                                        initialValue = ""
                                        isValidInput { input ->
                                            input.isNotEmpty()
                                        }
                                    }
                                    onValidInput { input ->
                                        //region save data
                                        viewModel.setDocument(
                                            RC_SAVE_USER,
                                            User(name = input, email = "me@email.com"),
                                            "Users",
                                            true
                                        )
                                        //endregion
                                    }
                                    onInvalidInput {
                                        toastShort("Name must not be empty")
                                    }
                                }
                                //endregion
                            }
                            show()
                        }
                    } else {
                        tvMessage.text = "Please login first"
                        btnAction.hide()
                    }
                }
                is ReadDocumentState.OnOffline -> {
                    tvMessage.text = "No internet connection"
                    btnAction.hide()
                }
                is ReadDocumentState.OnError -> {
                    tvMessage.text = if (state.exception.isUnauthenticated) {
                        "Please login first"
                    } else {
                        "Error: ${state.exception.message}"
                    }
                    btnAction.hide()
                }
            }
        }

        //endregion
        //region Observe write document event

        viewModel.registerWriteRequest(RC_SAVE_USER)
            .observe(this) {
                val message = when (val state = it.getState()) {
                    WriteDocumentState.Empty -> return@observe
                    is WriteDocumentState.OnProgress -> "Please wait..."
                    is WriteDocumentState.OnSuccess -> "Success, user data saved!"
                    is WriteDocumentState.OnFailed -> when {
                        state.exception.isOffline -> "Failed, no internet connection"
                        state.exception.isUnauthenticated -> "Please login first"
                        else -> "Failed, ${state.exception.message}"
                    }
                }
                toastShort(message)
            }

        viewModel.registerWriteRequest(RC_DELETE_USER)
            .observe(this) {
                val message = when (val state = it.getState()) {
                    WriteDocumentState.Empty -> return@observe
                    is WriteDocumentState.OnProgress -> "Please wait..."
                    is WriteDocumentState.OnSuccess -> "Success, user data deleted!"
                    is WriteDocumentState.OnFailed -> when {
                        state.exception.isOffline -> "Failed, no internet connection"
                        state.exception.isUnauthenticated -> "Please login first"
                        else -> "Failed, ${state.exception.message}"
                    }
                }
                toastShort(message)
            }

        //endregion
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            showSettings()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.removeOnBackStackChangedListener(this)
    }

    override fun onBackStackChanged() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    //endregion

}

private const val RC_SAVE_USER   = 1001
private const val RC_DELETE_USER = 1002
