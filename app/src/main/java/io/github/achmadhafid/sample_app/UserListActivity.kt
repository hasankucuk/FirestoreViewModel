package io.github.achmadhafid.sample_app

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.achmadhafid.firestore_view_model.FirestoreViewModel
import io.github.achmadhafid.firestore_view_model.firestoreViewModel
import io.github.achmadhafid.firestore_view_model.query.QueryDocumentState
import io.github.achmadhafid.firestore_view_model.query.registerQueryRequest
import io.github.achmadhafid.firestore_view_model.query.withDataBuilder
import io.github.achmadhafid.sample_app.entity.User
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.simplepref.simplePref
import io.github.achmadhafid.zpack.ktx.bindView
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
        //region Observe user listing query

        viewModel.registerQueryRequest<User>(RC_QUERY) {
            collection    = "Users"
            requireAuth   = this@UserListActivity.requireAuth
            requireOnline = this@UserListActivity.requireOnline
            syncWait      = this@UserListActivity.syncWait
            withDataBuilder { snapshot ->
                id = snapshot.id
            }
        }.observe(this) {
            when (val state = it.getState()) {
                QueryDocumentState.OnProgress        -> toastShort("Please wait")
                QueryDocumentState.OnOffline         -> toastShort("Error, No internet connection")
                is QueryDocumentState.OnError        -> toastShort("Error, ${state.exception.message}")
                is QueryDocumentState.OnDataNotFound -> toastShort("User not found")
                is QueryDocumentState.OnDataFound    -> listAdapter.submitList(state.values)
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

//TODO("Extract bug")
