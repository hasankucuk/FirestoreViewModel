@file:Suppress("TooManyFunctions")

package io.github.achmadhafid.firestore_view_model.write

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.achmadhafid.firestore_view_model.BaseViewModel
import io.github.achmadhafid.firestore_view_model.firestore
import io.github.achmadhafid.firestore_view_model.isSignedOut
import io.github.achmadhafid.firestore_view_model.uid
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

internal class WriteDocumentViewModelImpl : BaseViewModel(), WriteDocumentViewModel {

    private val requests = mutableMapOf<Int, MutableLiveData<WriteDocumentEvent>>()

    override fun onConnectionStateChange(isAvailable: Boolean) {
        /* No-op, connection state currently is checked upfront */
    }

    override fun onAuthStateChange(user: FirebaseUser?) {
        /* No-op, auth state currently is checked upfront */
    }

    override fun onCleared() {
        requests.clear()
    }

    //region Main API

    override fun registerWriteRequest(requestCode: Int): LiveData<WriteDocumentEvent> {
        if (!requests.containsKey(requestCode)) {
            requests[requestCode] = MutableLiveData()
        }
        return requests[requestCode]!!
    }

    override fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        collection: String,
        requireOnline: Boolean,
        timeOut: Long
    ) {
        val event = registerWriteRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (requireOnline && !isConnected()) {
            event.value = offlineExceptionEvent
        } else if (isSignedOut) {
            event.value = unauthenticatedExceptionEvent
        } else {
            val document = "${collection}/$uid"
            coroutineScope.launch {
                runCatching {
                    firestore.document(document)
                        .set(data)
                        .await()
                }.onSuccess {
                    event.postValue(getSuccessEvent(document))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }

            event.value = onProgressEvent
        }
    }

    override fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        document: String,
        requireOnline: Boolean,
        requireAuth: Boolean,
        timeOut: Long
    ) {
        val event = registerWriteRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (requireOnline && !isConnected()) {
            event.value = offlineExceptionEvent
        } else if (requireAuth && isSignedOut) {
            event.value = unauthenticatedExceptionEvent
        } else {
            coroutineScope.launch {
                runCatching {
                    withTimeout(timeOut) {
                        firestore.document(document)
                            .set(data)
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(document))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }

            event.value = onProgressEvent
        }
    }

    override fun <T : Any> addDocument(
        requestCode: Int,
        data: T,
        collection: String,
        requireOnline: Boolean,
        requireAuth: Boolean,
        timeOut: Long
    ) {
        val event = registerWriteRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (requireOnline && !isConnected()) {
            event.value = offlineExceptionEvent
        } else if (requireAuth && isSignedOut) {
            event.value = unauthenticatedExceptionEvent
        } else {
            coroutineScope.launch {
                runCatching {
                    withTimeout(timeOut) {
                        firestore.collection(collection)
                            .add(data)
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(it.path))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }

            event.value = onProgressEvent
        }
    }

    override fun deleteDocument(
        requestCode: Int,
        collection: String,
        requireOnline: Boolean,
        timeOut: Long
    ) {
        val event = registerWriteRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (requireOnline && !isConnected()) {
            event.value = offlineExceptionEvent
        } else if (isSignedOut) {
            event.value = unauthenticatedExceptionEvent
        } else {
            val document = "$collection/$uid"
            coroutineScope.launch {
                runCatching {
                    withTimeout(timeOut) {
                        firestore.document(document)
                            .delete()
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(document))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }

            event.value = onProgressEvent
        }
    }

    override fun deleteDocument(
        requestCode: Int,
        document: String,
        requireOnline: Boolean,
        requireAuth: Boolean,
        timeOut: Long
    ) {
        val event = registerWriteRequest(requestCode) as MutableLiveData<WriteDocumentEvent>
        if (event.value?.isInProgress == true) return

        if (requireOnline && !isConnected()) {
            event.value = offlineExceptionEvent
        } else if (requireAuth && isSignedOut) {
            event.value = unauthenticatedExceptionEvent
        } else {
            coroutineScope.launch {
                runCatching {
                    withTimeout(timeOut) {
                        firestore.document(document)
                            .delete()
                            .await()
                    }
                }.onSuccess {
                    event.postValue(getSuccessEvent(document))
                }.onFailure {
                    event.postValue(getErrorEvent(it))
                }
            }

            event.value = onProgressEvent
        }
    }

    //endregion

}

//region Helper for WriteDocumentEvent

private val WriteDocumentEvent.isInProgress
    get() = getState() is WriteDocumentState.OnProgress

private val onProgressEvent =
    WriteDocumentEvent(WriteDocumentState.OnProgress)

private val offlineExceptionEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(WriteDocumentException.Offline))

private val timeoutExceptionEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(WriteDocumentException.Timeout))

private val unauthenticatedExceptionEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(WriteDocumentException.Unauthenticated))

private fun getSuccessEvent(documentPath: String) =
    WriteDocumentEvent(WriteDocumentState.OnSuccess(documentPath))

private fun getErrorEvent(throwable: Throwable): WriteDocumentEvent = when (throwable) {
    is TimeoutCancellationException -> timeoutExceptionEvent
    is FirebaseFirestoreException -> WriteDocumentEvent(
        WriteDocumentState.OnFailed(WriteDocumentException.FirestoreException(throwable))
    )
    else -> getErrorEvent(
        FirebaseFirestoreException(
            "Unknown error",
            FirebaseFirestoreException.Code.UNKNOWN,
            throwable
        )
    )
}

//endregion
