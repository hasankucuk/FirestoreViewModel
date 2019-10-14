package io.github.achmadhafid.firestore_view_model

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import io.github.achmadhafid.firestore_view_model.query.QueryDocumentViewModel
import io.github.achmadhafid.firestore_view_model.query.QueryDocumentViewModelImpl
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentViewModel
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentViewModelImpl
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentViewModel
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentViewModelImpl
import io.github.achmadhafid.zpack.ktx.getViewModel

class FirestoreViewModel
internal constructor(
    application: Application,
    private val readDocumentViewModel: ReadDocumentViewModelImpl,
    private val writeDocumentViewModel: WriteDocumentViewModelImpl,
    private val queryDocumentViewModel: QueryDocumentViewModelImpl
) : AndroidViewModel(application),
    ReadDocumentViewModel by readDocumentViewModel,
    WriteDocumentViewModel by writeDocumentViewModel,
    QueryDocumentViewModel by queryDocumentViewModel {
    
    //region Net

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            readDocumentViewModel.onConnectionStateChange(true)
            writeDocumentViewModel.onConnectionStateChange(true)
            queryDocumentViewModel.onConnectionStateChange(true)
        }

        override fun onUnavailable() {
            readDocumentViewModel.onConnectionStateChange(false)
            writeDocumentViewModel.onConnectionStateChange(false)
            queryDocumentViewModel.onConnectionStateChange(false)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            readDocumentViewModel.onConnectionStateChange(false)
            writeDocumentViewModel.onConnectionStateChange(false)
            queryDocumentViewModel.onConnectionStateChange(false)
        }
    }
    private val authStateCallback = FirebaseAuth.AuthStateListener {
        readDocumentViewModel.onAuthStateChange(auth.currentUser)
        writeDocumentViewModel.onAuthStateChange(auth.currentUser)
        queryDocumentViewModel.onAuthStateChange(auth.currentUser)
    }

    init {
        auth.addAuthStateListener(authStateCallback)
        registerNetworkCallback(networkCallback)

        readDocumentViewModel.init(viewModelScope) { isConnected }
        writeDocumentViewModel.init(viewModelScope) { isConnected }
        queryDocumentViewModel.init(viewModelScope) { isConnected }
    }

    override fun onCleared() {
        super.onCleared()

        auth.removeAuthStateListener(authStateCallback)
        unregisterNetworkCallback(networkCallback)

        readDocumentViewModel.onCleared()
        writeDocumentViewModel.onCleared()
        queryDocumentViewModel.onCleared()
    }

    interface Extension

}

//region Factory

internal class FirestoreViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        FirestoreViewModel(
            application,
            ReadDocumentViewModelImpl(),
            WriteDocumentViewModelImpl(),
            QueryDocumentViewModelImpl()
        ) as T
}

//endregion


//region Consumer API view extension functions

fun <A> A.firestoreViewModel(): Lazy<FirestoreViewModel>
        where A : FirestoreViewModel.Extension, A : FragmentActivity =
    lazy(LazyThreadSafetyMode.NONE) {
        getViewModel<FirestoreViewModel>(
            FirestoreViewModelFactory(application)
        )
    }

fun <F> F.firestoreViewModel(): Lazy<FirestoreViewModel>
        where F : FirestoreViewModel.Extension, F : Fragment =
    lazy(LazyThreadSafetyMode.NONE) {
        getViewModel<FirestoreViewModel>(
            FirestoreViewModelFactory(requireActivity().application)
        )
    }

//endregion
