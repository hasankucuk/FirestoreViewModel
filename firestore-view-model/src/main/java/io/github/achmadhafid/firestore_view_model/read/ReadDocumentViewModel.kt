package io.github.achmadhafid.firestore_view_model.read

import androidx.lifecycle.LiveData

interface ReadDocumentViewModel {

    fun <T : Any> getDocumentByAuthUid(
        config: ReadDocumentByAuthUidConfig<T>
    ): LiveData<ReadDocumentEvent<T>>

    fun <T : Any> getDocumentById(
        config: ReadDocumentByIdConfig<T>
    ): LiveData<ReadDocumentEvent<T>>

}

//region Extension Functions for Client API

inline fun <reified T : Any> ReadDocumentViewModel.getDocumentByAuthUid(
    builder: ReadDocumentByAuthUidConfig<T>.() -> Unit
): LiveData<ReadDocumentEvent<T>> = getDocumentByAuthUid(
    ReadDocumentByAuthUidConfig(T::class).apply(builder)
)

inline fun <reified T : Any> ReadDocumentViewModel.getDocumentById(
    builder: ReadDocumentByIdConfig<T>.() -> Unit
): LiveData<ReadDocumentEvent<T>> = getDocumentById(
    ReadDocumentByIdConfig(T::class).apply(builder)
)

//endregion
