package io.github.achmadhafid.firestore_view_model.write

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class WriteDocumentState(internal val isSingleState: Boolean) {

    object Empty : WriteDocumentState(false)

    object OnProgress : WriteDocumentState(false)

    data class OnSuccess(val documentPath: String) : WriteDocumentState(true)

    data class OnFailed(val exception: WriteDocumentException) : WriteDocumentState(true)

    companion object {
        val transientState = Empty
    }

}

//region extensions for WriteDocumentEvent

internal val offlineEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(WriteDocumentException.Offline))
internal val unauthenticatedEvent =
    WriteDocumentEvent(WriteDocumentState.OnFailed(WriteDocumentException.Unauthenticated))
internal val onProgressEvent =
    WriteDocumentEvent(WriteDocumentState.OnProgress)

internal val WriteDocumentEvent.isInProgress
    get() = getState() == WriteDocumentState.OnProgress

internal fun getSuccessEvent(documentPath: String) =
    WriteDocumentEvent(WriteDocumentState.OnSuccess(documentPath))

internal fun getErrorEvent(throwable: Throwable) = when (throwable) {
    is FirebaseFirestoreException -> WriteDocumentEvent(
        WriteDocumentState.OnFailed(WriteDocumentException.FirestoreException(throwable))
    )
    else -> WriteDocumentEvent(
        WriteDocumentState.OnFailed(WriteDocumentException.Unknown)
    )
}

//endregion
