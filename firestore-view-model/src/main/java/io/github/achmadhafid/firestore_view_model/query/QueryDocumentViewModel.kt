package io.github.achmadhafid.firestore_view_model.query

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

interface QueryDocumentViewModel {

    fun <T : Any> registerQueryRequest(
        config: QueryDocumentConfig<T>,
        overrideIfExist: Boolean
    ): LiveData<QueryDocumentEvent<T>>

    fun changeQuery(requestCode: Int, viewState: Any? = null, query: CollectionReference.() -> Query)

    fun resetQuery(requestCode: Int)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getQueryViewState(requestCode: Int): T?

}

//region Extension Functions for Client API

inline fun <reified T : Any> QueryDocumentViewModel.registerQueryRequest(
    requestCode: Int,
    overrideIfExist: Boolean = false,
    builder: QueryDocumentConfig<T>.() -> Unit
): LiveData<QueryDocumentEvent<T>> = registerQueryRequest(
    QueryDocumentConfig(requestCode, T::class).apply(builder), overrideIfExist
)

//endregion
