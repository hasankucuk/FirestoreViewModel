package io.github.achmadhafid.firestore_view_model.query

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlin.reflect.KClass

data class QueryDocumentConfig<T : Any>(
    internal val requestCode: Int,
    internal var clazz: KClass<T>,
    internal var defaultQuery: (CollectionReference.() -> Query)? = null,
    internal var currentQuery: (CollectionReference.() -> Query)? = null,
    internal var defaultViewState: Any? = null,
    internal var currentViewState: Any? = null,
    internal var dataBuilder: (T.(DocumentSnapshot) -> Unit) = {},
    var collection: String? = null,
    var requireAuth: Boolean = false,
    var requireOnline: Boolean = false,
    var syncWait: Long = 0L
)

fun QueryDocumentConfig<*>.withQuery(query: CollectionReference.() -> Query) {
    defaultQuery = query
    currentQuery = defaultQuery
}

fun QueryDocumentConfig<*>.withViewState(viewState: () -> Any?) {
    defaultViewState = viewState()
    currentViewState = defaultViewState
}

fun <T : Any> QueryDocumentConfig<T>.withDataBuilder(builder: T.(DocumentSnapshot) -> Unit) {
    this.dataBuilder = builder
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> QueryDocumentConfig<*>.getViewState(): T? =
    currentViewState as? T
