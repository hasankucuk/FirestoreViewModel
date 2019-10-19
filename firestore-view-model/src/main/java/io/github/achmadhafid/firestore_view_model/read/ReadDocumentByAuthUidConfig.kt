package io.github.achmadhafid.firestore_view_model.read

import com.google.firebase.firestore.DocumentSnapshot
import kotlin.reflect.KClass

data class ReadDocumentByAuthUidConfig<T : Any>(
    internal var clazz: KClass<T>,
    var collection: String? = null,
    var requireOnline: Boolean = false,
    var syncWait: Long = 0L,
    internal var dataBuilder: T.(DocumentSnapshot) -> Unit = {}
)

fun <T : Any> ReadDocumentByAuthUidConfig<T>.withDataBuilder(builder: T.(DocumentSnapshot) -> Unit) {
    this.dataBuilder = builder
}

internal fun <T : Any> ReadDocumentByAuthUidConfig<T>.asDocumentConfig(): ReadDocumentConfig =
    ReadDocumentConfig(
        collection,
        requireAuth = true,
        requireOnline = requireOnline,
        syncWait = syncWait
    )
