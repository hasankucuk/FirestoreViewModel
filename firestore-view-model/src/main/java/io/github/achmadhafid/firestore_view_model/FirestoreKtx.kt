package io.github.achmadhafid.firestore_view_model

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlin.reflect.KClass

inline val DocumentSnapshot.isFromCache
    get() = metadata.isFromCache

inline val QuerySnapshot.isFromCache
    get() = metadata.isFromCache

inline val DocumentSnapshot.hasPendingWrites
    get() = metadata.hasPendingWrites()

inline val QuerySnapshot.hasPendingWrites
    get() = metadata.hasPendingWrites()

inline val DocumentSnapshot.isSynced
    get() = !isFromCache && !hasPendingWrites

inline val QuerySnapshot.isSynced
    get() = !isFromCache && !hasPendingWrites

inline fun <reified T : Any> DocumentSnapshot.of(objectBuilder: T.(DocumentSnapshot) -> Unit): T? =
    toObject(T::class.java)?.apply { objectBuilder(this@of) }

inline fun <reified T : Any> List<DocumentSnapshot>.of(objectBuilder: T.(DocumentSnapshot) -> Unit): List<T?> =
    map { it.of(objectBuilder) }

fun <T : Any> List<DocumentSnapshot>.of(clazz: Class<T>, objectBuilder: T.(DocumentSnapshot) -> Unit): List<T?> =
    map { snapshot -> snapshot.toObject(clazz)?.apply { objectBuilder(snapshot) } }

inline fun <reified T : Any> DocumentChange.of(noinline objectBuilder: T.(DocumentSnapshot) -> Unit): T =
    of(T::class, objectBuilder)

fun <T : Any> DocumentChange.of(clazz: KClass<T>, objectBuilder: T.(DocumentSnapshot) -> Unit): T =
    document.toObject(clazz.java).apply { objectBuilder(document) }

inline fun <reified T : Any> QuerySnapshot.extracts(
    noinline objectBuilder: T.(DocumentSnapshot) -> Unit
): Triple<List<T>, List<T>, List<T>> = extracts(T::class, objectBuilder)

fun <T : Any> QuerySnapshot.extracts(
    clazz: KClass<T>,
    objectBuilder: T.(DocumentSnapshot) -> Unit
): Triple<List<T>, List<T>, List<T>> {

    val addedList    = mutableListOf<T>()
    val modifiedList = mutableListOf<T>()
    val deletedList  = mutableListOf<T>()

    documentChanges.forEach { change ->
        with(change.of(clazz, objectBuilder)) {
            when (change.type) {
                DocumentChange.Type.ADDED    -> addedList.add(this)
                DocumentChange.Type.MODIFIED -> modifiedList.add(this)
                DocumentChange.Type.REMOVED  -> deletedList.add(this)
            }
        }
    }

    return Triple(addedList, modifiedList, deletedList)
}

//region Internal Helper

internal const val OFFLINE         = "No internet connection"
internal const val UNAUTHENTICATED = "Unauthenticated"

internal val offlineException
    get() = FirebaseFirestoreException(OFFLINE, FirebaseFirestoreException.Code.UNKNOWN)
internal val unauthenticatedException
    get() = FirebaseFirestoreException(UNAUTHENTICATED, FirebaseFirestoreException.Code.UNAUTHENTICATED)

val FirebaseFirestoreException.isOffline
    get() = code == FirebaseFirestoreException.Code.UNKNOWN && message == OFFLINE
val FirebaseFirestoreException.isUnauthenticated
    get() = code == FirebaseFirestoreException.Code.UNAUTHENTICATED && message == UNAUTHENTICATED

//endregion
