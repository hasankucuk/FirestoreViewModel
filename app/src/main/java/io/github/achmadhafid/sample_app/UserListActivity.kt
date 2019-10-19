package io.github.achmadhafid.sample_app

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.Query
import io.github.achmadhafid.firestore_view_model.FirestoreViewModel
import io.github.achmadhafid.firestore_view_model.firestoreViewModel
import io.github.achmadhafid.firestore_view_model.query.QueryDocumentException
import io.github.achmadhafid.firestore_view_model.query.QueryDocumentState
import io.github.achmadhafid.firestore_view_model.query.registerQueryRequest
import io.github.achmadhafid.firestore_view_model.query.withDataBuilder
import io.github.achmadhafid.firestore_view_model.query.withQuery
import io.github.achmadhafid.firestore_view_model.query.withViewState
import io.github.achmadhafid.sample_app.entity.User
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.simplepref.simplePref
import io.github.achmadhafid.zpack.ktx.bindView
import io.github.achmadhafid.zpack.ktx.onSingleClick
import io.github.achmadhafid.zpack.ktx.setMaterialToolbar
import io.github.achmadhafid.zpack.ktx.toastShort

class UserListActivity : BaseActivity(R.layout.activity_user_list),
    FragmentManager.OnBackStackChangedListener, FirestoreViewModel.Extension, SimplePref {

    //region Preference

    private var requireAuth by simplePref("require_auth") { true }
    private var requireOnline by simplePref("require_online") { true }
    private var syncWait by simplePref("sync_wait") { 0L }

    //endregion
    //region View

    private val fab: ExtendedFloatingActionButton by bindView(R.id.fab)
    private val recyclerView: RecyclerView by bindView(R.id.recycler_view)

    //endregion
    //region View Model

    private val viewModel by firestoreViewModel()

    //endregion
    //region Adapter

    private val listAdapter = UserListAdapter()

    //endregion
    //region Lifecycle Callback

    @Suppress("ComplexMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //region Setup toolbar

        setMaterialToolbar(R.id.toolbar)

        //endregion
        //region Setup recycler view

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter       = listAdapter

        //endregion
        //region setup fab

        fab.onSingleClick {
            val viewState: Map<String, Query.Direction>? = viewModel.getQueryViewState(RC_QUERY)
            viewState?.let {
                if (it["name"] == Query.Direction.DESCENDING) {
                    val newViewState = mapOf("name" to Query.Direction.ASCENDING)
                    viewModel.changeQuery(RC_QUERY, newViewState) {
                        orderBy("name", Query.Direction.ASCENDING)
                    }
                } else {
                    viewModel.resetQuery(RC_QUERY)
                }
            }
        }

        //endregion
        //region Observe user listing query

        viewModel.registerQueryRequest<User>(RC_QUERY) {
            collection    = "Users"
            requireAuth   = this@UserListActivity.requireAuth
            requireOnline = this@UserListActivity.requireOnline
            syncWait      = this@UserListActivity.syncWait
            withQuery {
                orderBy("name", Query.Direction.DESCENDING)
            }
            withViewState {
                mapOf("name" to Query.Direction.DESCENDING)
            }
            withDataBuilder { snapshot ->
                id = snapshot.id
            }
        }.observe(this) { event ->
            when (val state = event.getState()) {
                QueryDocumentState.OnLoading    -> {
                    toastShort("Please wait")
                    listAdapter.submitList(emptyList())
                    fab.hide()
                }
                is QueryDocumentState.OnSuccess -> {
                    listAdapter.submitList(state.values)
                    fab.show()
                }
                is QueryDocumentState.OnFailed  -> {
                    val message = if (event.hasBeenConsumed(localClassName)) {
                        "(has been handled)"
                    } else when (val error = state.exception) {
                        QueryDocumentException.Offline -> "No internet connection"
                        QueryDocumentException.Unauthenticated -> "Please login first"
                        is QueryDocumentException.FirestoreException -> error.firestoreException.message
                    }
                    toastShort("Error: $message")
                    listAdapter.submitList(emptyList())
                    fab.hide()
                }
            }
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

private const val RC_QUERY = 5000
