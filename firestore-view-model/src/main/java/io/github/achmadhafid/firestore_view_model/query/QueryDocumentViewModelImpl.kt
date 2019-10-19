@file:Suppress("TooManyFunctions")

package io.github.achmadhafid.firestore_view_model.query

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.github.achmadhafid.firestore_view_model.BaseViewModel
import io.github.achmadhafid.firestore_view_model.extracts
import io.github.achmadhafid.firestore_view_model.firestore
import io.github.achmadhafid.firestore_view_model.hasPendingWrites
import io.github.achmadhafid.firestore_view_model.isFromCache
import io.github.achmadhafid.firestore_view_model.isSignedOut
import io.github.achmadhafid.firestore_view_model.isSynced
import io.github.achmadhafid.firestore_view_model.of
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class QueryDocumentViewModelImpl : BaseViewModel(), QueryDocumentViewModel {

    private val configs    = mutableMapOf<Int, QueryDocumentConfig<*>>()
    private val documents  = mutableMapOf<Int, QueryDocumentData>()
    private val events     = mutableMapOf<Int, LiveData<QueryDocumentEvent<*>>>()
    private val timestamps = mutableMapOf<Int, Long>()
    private val listeners  = mutableMapOf<Int, ListenerRegistration>()

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
        events.clear()
        configs.clear()
        timestamps.clear()
    }

    //region Main API

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> registerQueryRequest(
        config: QueryDocumentConfig<T>
    ): LiveData<QueryDocumentEvent<T>> {
        if (!events.containsKey(config.requestCode)) {
            events[config.requestCode] =
                addConfig(config).map { (snapshot, exception) ->
                    val state = exception?.let {
                        QueryDocumentState.OnFailed(it)
                    } ?: snapshot?.let {

                        val documents = if (it.isEmpty) emptyList()
                        else it.documents.of(config.clazz.java, config.dataBuilder)
                        val result = if (it.isEmpty) Triple(emptyList(), emptyList(), emptyList())
                        else it.extracts(config.clazz, config.dataBuilder)

                        QueryDocumentState.OnSuccess(
                            documents,
                            result.first, result.second, result.third,
                            it.isFromCache,
                            it.hasPendingWrites
                        )
                    } ?: QueryDocumentState.OnLoading
                    QueryDocumentEvent(state)
                } as LiveData<QueryDocumentEvent<*>>
        }
        return events[config.requestCode]!! as LiveData<QueryDocumentEvent<T>>
    }

    override fun changeQuery(requestCode: Int, viewState: Any?, query: CollectionReference.() -> Query) {
        configs[requestCode]?.let {
            it.currentQuery     = query
            it.currentViewState = viewState
            attachListener(it)
        } ?: throw IllegalStateException("Query with request code `$requestCode` is not found")
    }

    override fun resetQuery(requestCode: Int) {
        configs[requestCode]?.let {
            it.currentQuery     = it.defaultQuery
            it.currentViewState = it.defaultViewState
            attachListener(it)
        } ?: throw IllegalStateException("Query with request code `$requestCode` is not found")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getQueryViewState(requestCode: Int): T? =
        configs[requestCode]?.getViewState()

    //endregion
    //region Private Helper

    private fun addConfig(config: QueryDocumentConfig<*>): QueryDocumentData {
        if (!documents.containsKey(config.requestCode)) {
            configs[config.requestCode] = config
            documents[config.requestCode] = MutableLiveData()
            attachListener(config)
        }
        return documents[config.requestCode]!!
    }

    @Suppress("ComplexMethod")
    private fun attachListener(config: QueryDocumentConfig<*>) {
        fun isConstraintFulfilled() = if (config.requireOnline && !isConnected()) {
            updateSnapshot(config, null, QueryDocumentException.Offline)
            false
        } else if (config.requireAuth && isSignedOut) {
            updateSnapshot(config, null, QueryDocumentException.Unauthenticated)
            false
        } else {
            true
        }

        /* clear current listener */
        listeners[config.requestCode]?.remove()

        config.collection?.let { collectionPath ->
            val eventListener = EventListener<QuerySnapshot> { snapshot, exception ->
                if (!isConstraintFulfilled()) {
                    return@EventListener
                } else if (snapshot?.isSynced == true || config.syncWait <= 0L || !isConnected()) {
                    updateSnapshot(config, snapshot, exception?.let {
                        QueryDocumentException.FirestoreException(it)
                    })
                } else {
                    coroutineScope.launch {
                        val timestamp = System.currentTimeMillis()
                        delay(config.syncWait)
                        updateSnapshot(config, snapshot, exception?.let {
                            QueryDocumentException.FirestoreException(it)
                        }, timestamp)
                    }
                }
            }
            val reference = firestore.collection(collectionPath)

            config.currentQuery?.let { queryBuilder ->
                reference.queryBuilder().addSnapshotListener(MetadataChanges.INCLUDE, eventListener)
            } ?: reference.addSnapshotListener(MetadataChanges.INCLUDE, eventListener)

        } ?: throw IllegalStateException("Collection must be set")

        /* immediately check constraint */
        if (isConstraintFulfilled())
            /* emit OnLoading state */
            updateSnapshot(config, null, null)
    }

    private fun updateSnapshot(
        config: QueryDocumentConfig<*>,
        snapshot: QuerySnapshot? = null,
        exception: QueryDocumentException? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (!timestamps.contains(config.requestCode) || timestamps[config.requestCode]!! < timestamp) {
            timestamps[config.requestCode] = timestamp
            //region avoid double error update
            exception?.let {
                if (documents[config.requestCode]?.value?.second == it) {
                    return@updateSnapshot
                }
            }
            //endregion
            documents[config.requestCode]?.postValue(snapshot to exception)
        }
    }

    //endregion

}

private typealias QueryDocumentData = MutableLiveData<Pair<QuerySnapshot?, QueryDocumentException?>>
