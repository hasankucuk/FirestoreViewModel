package io.github.achmadhafid.sample_app

import android.os.Bundle
import android.view.Menu
import androidx.core.view.forEach
import com.google.android.material.button.MaterialButton
import io.github.achmadhafid.firebase_auth_view_model.signin.GoogleSignInExtensions
import io.github.achmadhafid.firestore_view_model.FirestoreViewModel
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.zpack.ktx.bindView
import io.github.achmadhafid.zpack.ktx.onSingleClick
import io.github.achmadhafid.zpack.ktx.setMaterialToolbar
import io.github.achmadhafid.zpack.ktx.startActivity

class MainActivity : BaseActivity(R.layout.activity_main),
    SimplePref, GoogleSignInExtensions, FirestoreViewModel.Extension {

    //region View Binding

    private val btnList: MaterialButton by bindView(R.id.btn_open_user_list)
    private val btnDetail: MaterialButton by bindView(R.id.btn_open_user_detail)

    //endregion
    //region Lifecycle Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMaterialToolbar(R.id.toolbar)
        btnList.onSingleClick { startActivity<UserListActivity>() }
        btnDetail.onSingleClick { startActivity<UserDetailActivity>() }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.forEach {
            if (it.itemId == R.id.action_settings) {
                it.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    //endregion

}
