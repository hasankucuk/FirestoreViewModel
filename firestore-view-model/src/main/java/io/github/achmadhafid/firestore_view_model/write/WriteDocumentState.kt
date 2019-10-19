package io.github.achmadhafid.firestore_view_model.write

sealed class WriteDocumentState {

    object OnProgress : WriteDocumentState()

    data class OnSuccess(val documentPath: String) : WriteDocumentState()

    data class OnFailed(val exception: WriteDocumentException) : WriteDocumentState()

}
