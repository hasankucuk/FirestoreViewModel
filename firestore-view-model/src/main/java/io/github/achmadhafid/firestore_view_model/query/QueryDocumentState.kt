package io.github.achmadhafid.firestore_view_model.query

sealed class QueryDocumentState<out T : Any> {

    object OnLoading : QueryDocumentState<Nothing>()

    data class OnSuccess<out T : Any>(
        val values: List<T>,
        val addedList: List<T>,
        val modifiedList: List<T>,
        val deletedList: List<T>,
        val isFromCache: Boolean = true,
        val hasPendingWrite: Boolean = false
    ) : QueryDocumentState<T>()

    data class OnFailed(val exception: QueryDocumentException) :
        QueryDocumentState<Nothing>()

}
