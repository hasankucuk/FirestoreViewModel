package io.github.achmadhafid.firestore_view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.achmadhafid.zpack.ktx.isConnected

internal val auth by lazy {
    FirebaseAuth.getInstance()
}
internal val firestore by lazy {
    FirebaseFirestore.getInstance()
}

internal val isSignedIn
    get() = auth.currentUser != null

internal val isSignedOut
    get() = auth.currentUser == null

internal val uid
    get() = auth.uid

internal val DocumentSnapshot.isFromCache
    get() = metadata.isFromCache

internal val DocumentSnapshot.hasPendingWrites
    get() = metadata.hasPendingWrites()

internal val DocumentSnapshot.isSynced
    get() = !isFromCache && !hasPendingWrites

internal val AndroidViewModel.isConnected
    get() = getApplication<Application>().isConnected == true
