package io.github.achmadhafid.firestore_view_model.write

import androidx.lifecycle.LiveData

interface WriteDocumentViewModel {

    fun registerWriteRequest(requestCode: Int): LiveData<WriteDocumentEvent>

    fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        collection: String,
        requireOnline: Boolean = false,
        timeOut: Long = Long.MAX_VALUE
    )

    @Suppress("LongParameterList")
    fun <T : Any> setDocument(
        requestCode: Int,
        data: T,
        document: String,
        requireOnline: Boolean = false,
        requireAuth: Boolean = true,
        timeOut: Long = Long.MAX_VALUE
    )

    @Suppress("LongParameterList")
    fun <T : Any> addDocument(
        requestCode: Int,
        data: T,
        collection: String,
        requireOnline: Boolean = false,
        requireAuth: Boolean = true,
        timeOut: Long = Long.MAX_VALUE
    )

    fun deleteDocument(
        requestCode: Int,
        collection: String,
        requireOnline: Boolean = false,
        timeOut: Long = Long.MAX_VALUE
    )

    fun deleteDocument(
        requestCode: Int,
        document: String,
        requireOnline: Boolean = false,
        requireAuth: Boolean = true,
        timeOut: Long = Long.MAX_VALUE
    )

}
