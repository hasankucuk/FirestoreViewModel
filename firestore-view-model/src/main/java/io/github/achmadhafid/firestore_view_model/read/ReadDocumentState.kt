package io.github.achmadhafid.firestore_view_model.read

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class ReadDocumentState<out T : Any>(internal val isSingleState: Boolean) {

    object Empty : ReadDocumentState<Nothing>(false)

    object OnProgress : ReadDocumentState<Nothing>(false)

    data class OnDataFound<out T : Any>(
        val value: T,
        val isAuthenticated: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : ReadDocumentState<T>(false)

    data class OnDataNotFound(
        val isAuthenticated: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : ReadDocumentState<Nothing>(false)

    object OnOffline : ReadDocumentState<Nothing>(false)

    data class OnError(val exception: FirebaseFirestoreException) :
        ReadDocumentState<Nothing>(true)

    companion object {
        val transientState = Empty
    }

}
