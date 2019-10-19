package io.github.achmadhafid.firestore_view_model.read

import com.google.firebase.firestore.DocumentSnapshot
import kotlin.reflect.KClass

data class ReadDocumentByIdConfig<T : Any>(
    internal var clazz: KClass<T>,
    var document: String? = null,
    var requireAuth: Boolean = false,
    var requireOnline: Boolean = false,
    var syncWait: Long = 0L,
    internal var dataBuilder: T.(DocumentSnapshot) -> Unit = {}
)

fun <T : Any> ReadDocumentByIdConfig<T>.withDataBuilder(builder: T.(DocumentSnapshot) -> Unit) {
    this.dataBuilder = builder
}

internal fun <T : Any> ReadDocumentByIdConfig<T>.asDocumentConfig(): ReadDocumentConfig =
    ReadDocumentConfig(null, document, requireAuth, requireOnline, syncWait)
