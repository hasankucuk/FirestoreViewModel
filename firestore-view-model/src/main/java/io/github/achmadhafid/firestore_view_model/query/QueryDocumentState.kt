package io.github.achmadhafid.firestore_view_model.query

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class QueryDocumentState<out T : Any>(internal val isSingleState: Boolean) {

    object Empty : QueryDocumentState<Nothing>(false)

    object OnProgress : QueryDocumentState<Nothing>(false)

    data class OnDataFound<out T : Any>(
        val values: List<T?>,
        val addedList: List<T>,
        val modifiedList: List<T>,
        val deletedList: List<T>,
        val isAuthenticated: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : QueryDocumentState<T>(false)

    data class OnDataNotFound(
        val isAuthenticated: Boolean,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : QueryDocumentState<Nothing>(false)

    object OnOffline : QueryDocumentState<Nothing>(false)

    data class OnError(val exception: FirebaseFirestoreException) :
        QueryDocumentState<Nothing>(true)

    companion object {
        val transientState = Empty
    }

}
