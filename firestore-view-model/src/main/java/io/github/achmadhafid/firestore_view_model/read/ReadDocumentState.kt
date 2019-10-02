package io.github.achmadhafid.firestore_view_model.read

sealed class ReadDocumentState<out T : Any>(internal val isSingleState: Boolean) {

    object Empty : ReadDocumentState<Nothing>(false)

    object OnProgress : ReadDocumentState<Nothing>(false)

    class OnDataFound<T : Any>(
        val value: T,
        val isSignedIn: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : ReadDocumentState<T>(false)

    class OnDataNotFound(
        val isSignedIn: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : ReadDocumentState<Nothing>(false)

    class OnError(val exception: ReadDocumentException) :
        ReadDocumentState<Nothing>(true)

    companion object {
        val transientState = Empty
    }

}
