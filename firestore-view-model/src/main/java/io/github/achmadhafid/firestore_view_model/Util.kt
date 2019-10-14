package io.github.achmadhafid.firestore_view_model

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.github.achmadhafid.zpack.ktx.connectivityManager
import io.github.achmadhafid.zpack.ktx.isConnected

internal val auth by lazy {
    FirebaseAuth.getInstance()
}

internal val firestore by lazy {
    Firebase.firestore
}

internal val isSignedIn
    get() = auth.currentUser != null

internal val isSignedOut
    get() = auth.currentUser == null

internal val uid
    get() = auth.uid

internal val AndroidViewModel.isConnected
    get() = getApplication<Application>().isConnected == true

internal fun AndroidViewModel.registerNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
    val networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    getApplication<Application>().connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
}

internal fun AndroidViewModel.unregisterNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
    getApplication<Application>().connectivityManager.unregisterNetworkCallback(networkCallback)
}
