package io.github.achmadhafid.firestore_view_model

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentConfig
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentEvent
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentException
import io.github.achmadhafid.firestore_view_model.read.ReadDocumentState
import io.github.achmadhafid.firestore_view_model.write.WriteDocumentEvent
import io.github.achmadhafid.firestore_view_model.write.getErrorEvent
import io.github.achmadhafid.firestore_view_model.write.getSuccessEvent
import io.github.achmadhafid.firestore_view_model.write.isInProgress
import io.github.achmadhafid.firestore_view_model.write.offlineEvent
import io.github.achmadhafid.firestore_view_model.write.onProgressEvent
import io.github.achmadhafid.firestore_view_model.write.unauthenticatedEvent
import io.github.achmadhafid.zpack.ktx.getViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.reflect.KClass

private typealias ReadDocumentData = MutableLiveData<Pair<DocumentSnapshot?, ReadDocumentException?>>

@Suppress("TooManyFunctions")
class FirestoreViewModel
internal constructor(
    application: Application
) : AndroidViewModel(application), FirebaseAuth.AuthStateListener {

    private val firestore by lazy {
        Firebase.firestore
    }

    init {
        auth.addAuthStateListener(this)
    }

    //region Read

    private val configs    = mutableMapOf<String, ReadDocumentConfig>()
    private val documents  = mutableMapOf<String, ReadDocumentData>()
    private val timestamps = mutableMapOf<String, Long>()
    private val listeners  = mutableMapOf<String, ListenerRegistration>()

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        configs.forEach { attachListener(it.value) }
    }

    private fun updateSnapshot(
        config: ReadDocumentConfig,
        snapshot: DocumentSnapshot? = null,
        exception: ReadDocumentException? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!timestamps.contains(config.path) || timestamps[config.path]!! < timestamp) {
            timestamps[config.path] = timestamp
            documents[config.path]?.value = snapshot to exception
        }
    }

    private fun attachListener(config: ReadDocumentConfig) {
        /* clear current listener */
        listeners[config.path]?.remove()

        if (isSignedIn || !config.isAuthRequired) {
            listeners[config.path] =
                firestore.document(config.documentPath ?: "${config.collectionPath}/$uid")
                    .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, firestoreException ->
                        val exception = firestoreException?.let {
                            ReadDocumentException.FirestoreException(it)
                        }

                        if (config.isOnlineRequired && !isConnected) {
                            documents[config.path]?.value = null to ReadDocumentException.Offline
                            return@addSnapshotListener
                        }

                        if (snapshot?.isSynced == true || config.syncWait <= 0L) {
                            updateSnapshot(config, snapshot, exception)
                        } else {
                            viewModelScope.launch {
                                val timestamp = System.currentTimeMillis()
                                delay(config.syncWait)
                                updateSnapshot(config, snapshot, exception, timestamp)
                            }
                        }
                    }
        } else {
            documents[config.path]?.value = null to ReadDocumentException.Unauthenticated
        }
    }

    private fun addConfig(config: ReadDocumentConfig) {
        if (!documents.containsKey(config.path)) {
            configs[config.path] = config
            documents[config.path] = MutableLiveData()
            attachListener(config)
        }
    }

    fun <T : Any> getDocument(
        clazz: KClass<T>,
        collectionPath: String,
        syncWait: Long = 0L,
        isOnlineRequired: Boolean = false,
        builder: T.(DocumentSnapshot) -> T
    ): LiveData<ReadDocumentEvent<T>> {
        addConfig(
            ReadDocumentConfig(
                collectionPath,
                isOnlineRequired = isOnlineRequired,
                syncWait = syncWait
            )
        )
        return documents[collectionPath]?.map { (snapshot, exception) ->
            val state = exception?.let {
                ReadDocumentState.OnError(it)
            } ?: snapshot?.let {
                if (snapshot.exists()) {
                    ReadDocumentState.OnDataFound(
                        it.toObject(clazz.java)?.builder(it)!!,
                        isSignedIn,
                        snapshot.isFromCache,
                        snapshot.hasPendingWrites
                    )
                } else {
                    ReadDocumentState.OnDataNotFound(
                        isSignedIn,
                        snapshot.isFromCache,
                        snapshot.hasPendingWrites
                    )
                }
            }!!
            ReadDocumentEvent(state)
        }!!
    }

    @Suppress("LongParameterList")
    fun <T : Any> getDocument(
        clazz: KClass<T>,
        documentPath: String,
        syncWait: Long = 0L,
        isAuthRequired: Boolean = true,
        isOnlineRequired: Boolean = false,
        builder: T.(DocumentSnapshot) -> T
    ): LiveData<ReadDocumentEvent<T>> {
        addConfig(
            ReadDocumentConfig(
                null,
                documentPath,
                isAuthRequired,
                isOnlineRequired,
                syncWait
            )
        )
        return documents[documentPath]?.map { (snapshot, exception) ->
            val state = exception?.let {
                ReadDocumentState.OnError(it)
            } ?: snapshot?.let {
                if (snapshot.exists()) {
                    ReadDocumentState.OnDataFound(
                        it.toObject(clazz.java)?.builder(it)!!,
                        isSignedIn,
                        snapshot.isFromCache,
                        snapshot.hasPendingWrites
                    )
                } else {
                    ReadDocumentState.OnDataNotFound(
                        isSignedIn,
                        snapshot.isFromCache,
                        snapshot.hasPendingWrites
                    )
                }
            }!!
            ReadDocumentEvent(state)
        }!!
    }

    //endregion
    //region Write

    private val requests = mutableMapOf<Int, MutableLiveData<WriteDocumentEvent>>()

    fun getRequest(requestCode: Int): LiveData<WriteDocumentEvent> {
        if (!requests.containsKey(requestCode)) {
            requests[requestCode] = MutableLiveData()
        }
        return requests[requestCode]!!
    }

    fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        collectionPath: String,
        isOnlineRequired: Boolean = false
    ) {
        val event = getRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (isOnlineRequired && !isConnected) {
            event.value = offlineEvent
        } else if (isSignedOut) {
            event.value = unauthenticatedEvent
        } else {
            viewModelScope.launch {
                runCatching {
                    "${collectionPath}/$uid".also {
                        firestore.document(it)
                            .set(data)
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(it))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }
            event.value = onProgressEvent
        }
    }

    fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        documentPath: String,
        isAuthRequired: Boolean = true,
        isOnlineRequired: Boolean = false
    ) {
        val event = getRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (isOnlineRequired && !isConnected) {
            event.value = offlineEvent
        } else if (isAuthRequired && isSignedOut) {
            event.value = unauthenticatedEvent
        } else {
            viewModelScope.launch {
                runCatching {
                    documentPath.also {
                        firestore.document(it)
                            .set(data)
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(it))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }
            event.value = onProgressEvent
        }
    }

    fun <T : Any> addDocument(
        requestCode: Int,
        data: T,
        collectionPath: String,
        isAuthRequired: Boolean = true,
        isOnlineRequired: Boolean = false
    ) {
        val event = getRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (isOnlineRequired && !isConnected) {
            event.value = offlineEvent
        } else if (isAuthRequired && isSignedOut) {
            event.value = unauthenticatedEvent
        } else {
            viewModelScope.launch {
                runCatching {
                    firestore.collection(collectionPath)
                        .add(data)
                        .await()
                }.onSuccess {
                    event.postValue(getSuccessEvent(it.path))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }
            event.value = onProgressEvent
        }
    }

    fun deleteDocument(
        requestCode: Int,
        collectionPath: String,
        isOnlineRequired: Boolean = false
    ) {
        val event = getRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (isOnlineRequired && !isConnected) {
            event.value = offlineEvent
        } else if (isSignedOut) {
            event.value = unauthenticatedEvent
        } else {
            viewModelScope.launch {
                runCatching {
                    "$collectionPath/$uid".also {
                        firestore.document(it)
                            .delete()
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(it))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }
            event.value = onProgressEvent
        }
    }

    fun deleteDocument(
        requestCode: Int,
        documentPath: String,
        isAuthRequired: Boolean = true,
        isOnlineRequired: Boolean = false
    ) {
        val event = getRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (isOnlineRequired && !isConnected) {
            event.value = offlineEvent
        } else if (isAuthRequired && isSignedOut) {
            event.value = unauthenticatedEvent
        } else {
            viewModelScope.launch {
                runCatching {
                    documentPath.also {
                        firestore.document(it)
                            .delete()
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(it))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }
            event.value = onProgressEvent
        }
    }

    //endregion

    override fun onCleared() {
        super.onCleared()

        auth.removeAuthStateListener(this)
        listeners.values.forEach { it.remove() }
        listeners.clear()
        documents.clear()
        configs.clear()
        requests.clear()
    }

    interface Extension

}

//region Consumer API view extension functions

fun <A> A.firestoreViewModel(): Lazy<FirestoreViewModel>
        where A : FirestoreViewModel.Extension, A : FragmentActivity =
    lazy(LazyThreadSafetyMode.NONE) {
        getViewModel<FirestoreViewModel>(
            ViewModelProvider.AndroidViewModelFactory(application)
        )
    }

fun <F> F.firestoreViewModel(): Lazy<FirestoreViewModel>
        where F : FirestoreViewModel.Extension, F : Fragment =
    lazy(LazyThreadSafetyMode.NONE) {
        getViewModel<FirestoreViewModel>(
            ViewModelProvider.AndroidViewModelFactory(
                requireActivity().application
            )
        )
    }

//endregion
