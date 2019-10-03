package io.github.achmadhafid.firestore_view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import io.github.achmadhafid.zpack.ktx.isConnected

internal val auth by lazy {
    FirebaseAuth.getInstance()
}

internal val isSignedIn
    get() = auth.currentUser != null

internal val isSignedOut
    get() = auth.currentUser == null

internal val uid
    get() = auth.uid

internal val AndroidViewModel.isConnected
    get() = getApplication<Application>().isConnected == true
