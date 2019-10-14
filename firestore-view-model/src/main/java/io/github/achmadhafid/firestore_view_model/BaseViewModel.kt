package io.github.achmadhafid.firestore_view_model

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope

internal abstract class BaseViewModel {

    protected lateinit var coroutineScope: CoroutineScope
    protected lateinit var isConnected: () -> Boolean

    fun init(coroutineScope: CoroutineScope, isConnected: () -> Boolean) {
        this.coroutineScope = coroutineScope
        this.isConnected    = isConnected
    }

    abstract fun onConnectionStateChange(isAvailable: Boolean)
    abstract fun onAuthStateChange(user: FirebaseUser?)
    abstract fun onCleared()

}
