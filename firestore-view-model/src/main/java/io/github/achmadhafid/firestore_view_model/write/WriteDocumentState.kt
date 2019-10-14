package io.github.achmadhafid.firestore_view_model.write

import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.achmadhafid.firestore_view_model.offlineException
import io.github.achmadhafid.firestore_view_model.unauthenticatedException

sealed class WriteDocumentState(internal val isSingleState: Boolean) {

    object Empty : WriteDocumentState(false)

    data class OnProgress(val isOnline: Boolean) : WriteDocumentState(!isOnline)

    data class OnSuccess(val documentPath: String) : WriteDocumentState(true)

    data class OnFailed(val exception: FirebaseFirestoreException) : WriteDocumentState(true)

    companion object {
        val transientState = Empty
    }

}

//region Helper for WriteDocumentEvent

internal fun onProgressEvent(isOnline: Boolean) =
    WriteDocumentEvent(WriteDocumentState.OnProgress(isOnline))

internal val WriteDocumentEvent.isInProgress
    get() = getState() is WriteDocumentState.OnProgress

internal fun getSuccessEvent(documentPath: String) =
    WriteDocumentEvent(WriteDocumentState.OnSuccess(documentPath))

internal val offlineExceptionEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(offlineException))

internal val unauthenticatedExceptionEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(unauthenticatedException))

internal fun getErrorEvent(throwable: Throwable) = when (throwable) {
    is FirebaseFirestoreException -> WriteDocumentEvent(
        WriteDocumentState.OnFailed(throwable)
    )
    else -> WriteDocumentEvent(WriteDocumentState.OnFailed(
        FirebaseFirestoreException("Unknown", FirebaseFirestoreException.Code.UNKNOWN)
    ))
}

//endregion
