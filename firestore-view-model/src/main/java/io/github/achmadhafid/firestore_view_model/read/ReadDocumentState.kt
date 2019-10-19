package io.github.achmadhafid.firestore_view_model.read

sealed class ReadDocumentState<out T : Any> {

    object OnLoading : ReadDocumentState<Nothing>()

    data class OnSuccess<out T : Any>(
        val value: T?,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : ReadDocumentState<T>()

    data class OnError(val exception: ReadDocumentException) :
        ReadDocumentState<Nothing>()

}
