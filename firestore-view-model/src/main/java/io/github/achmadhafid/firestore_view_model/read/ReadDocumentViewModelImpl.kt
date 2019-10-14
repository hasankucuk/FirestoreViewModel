package io.github.achmadhafid.firestore_view_model.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import io.github.achmadhafid.firestore_view_model.BaseViewModel
import io.github.achmadhafid.firestore_view_model.unauthenticatedException
import io.github.achmadhafid.firestore_view_model.firestore
import io.github.achmadhafid.firestore_view_model.hasPendingWrites
import io.github.achmadhafid.firestore_view_model.isFromCache
import io.github.achmadhafid.firestore_view_model.isOffline
import io.github.achmadhafid.firestore_view_model.isSignedIn
import io.github.achmadhafid.firestore_view_model.isSignedOut
import io.github.achmadhafid.firestore_view_model.isSynced
import io.github.achmadhafid.firestore_view_model.offlineException
import io.github.achmadhafid.firestore_view_model.uid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ReadDocumentViewModelImpl : BaseViewModel(), ReadDocumentViewModel {

    private val configs    = mutableMapOf<String, ReadDocumentConfig>()
    private val documents  = mutableMapOf<String, ReadDocumentData>()
    private val timestamps = mutableMapOf<String, Long>()
    private val listeners  = mutableMapOf<String, ListenerRegistration>()

    override fun onConnectionStateChange(isAvailable: Boolean) {
        //region re-attach or detach any listener that requires internet connection

        configs.filter {
            it.value.requireOnline
        }.forEach {
            attachListener(it.value)
        }

        //endregion
    }

    override fun onAuthStateChange(user: FirebaseUser?) {
        //region re-attach or detach any listener that requires auth

        configs.filter {
            it.value.requireAuth
        }.forEach {
            attachListener(it.value)
        }

        //endregion
    }

    override fun onCleared() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
        documents.clear()
        configs.clear()
    }

    //region Main API

    override fun <T : Any> getDocumentByAuthUid(
        config: ReadDocumentByAuthUidConfig<T>
    ): LiveData<ReadDocumentEvent<T>> {
        addConfig(config.asDocumentConfig())
        return documents[config.collection]!!.map { (snapshot, exception) ->
            val state = exception?.let {
                if (it.isOffline) ReadDocumentState.OnOffline
                else ReadDocumentState.OnError(it)
            } ?: snapshot?.let {
                if (snapshot.exists()) {
                    ReadDocumentState.OnDataFound(
                        it.toObject(config.clazz.java)?.apply { (config.dataBuilder)(it) }!!,
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
            }
            ReadDocumentEvent(state!!)
        }
    }

    override fun <T : Any> getDocumentById(
        config: ReadDocumentByIdConfig<T>
    ): LiveData<ReadDocumentEvent<T>> {
        addConfig(config.asDocumentConfig())
        return documents[config.document]!!.map { (snapshot, exception) ->
            val state = exception?.let {
                if (it.isOffline) ReadDocumentState.OnOffline
                else ReadDocumentState.OnError(it)
            } ?: snapshot?.let {
                if (snapshot.exists()) {
                    ReadDocumentState.OnDataFound(
                        it.toObject(config.clazz.java)?.apply { (config.dataBuilder)(it) }!!,
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
            }
            ReadDocumentEvent(state!!)
        }
    }

    //endregion
    //region Private Helper

    private fun addConfig(config: ReadDocumentConfig) {
        if (!documents.containsKey(config.path)) {
            configs[config.path] = config
            documents[config.path] = MutableLiveData()
            attachListener(config)
        }
    }

    @Suppress("ComplexMethod")
    private fun attachListener(config: ReadDocumentConfig) {
        fun isConstraintFulfilled() = if (config.requireOnline && !isConnected()) {
            updateSnapshot(config, null, offlineException)
            false
        } else if (config.requireAuth && isSignedOut) {
            updateSnapshot(config, null, unauthenticatedException)
            false
        } else {
            true
        }

        /* clear current listener */
        listeners[config.path]?.remove()

        listeners[config.path] =
            firestore.document(config.documentPath ?: "${config.collectionPath}/$uid")
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                    if (!isConstraintFulfilled()) {
                        return@addSnapshotListener
                    } else if (snapshot?.isSynced == true || config.syncWait <= 0L || !isConnected()) {
                        updateSnapshot(config, snapshot, exception)
                    } else {
                        coroutineScope.launch {
                            val timestamp = System.currentTimeMillis()
                            delay(config.syncWait)
                            updateSnapshot(config, snapshot, exception, timestamp)
                        }
                    }
                }

        /* immediately check constraint */
        isConstraintFulfilled()
    }

    private fun updateSnapshot(
        config: ReadDocumentConfig,
        snapshot: DocumentSnapshot? = null,
        exception: FirebaseFirestoreException? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!timestamps.contains(config.path) || timestamps[config.path]!! < timestamp) {
            timestamps[config.path] = timestamp
            //region avoid double error update
            exception?.let {
                documents[config.path]?.value?.second?.let { return@updateSnapshot }
            }
            //endregion
            documents[config.path]?.postValue(snapshot to exception)
        }
    }

    //endregion

}

private typealias ReadDocumentData = MutableLiveData<Pair<DocumentSnapshot?, FirebaseFirestoreException?>>
