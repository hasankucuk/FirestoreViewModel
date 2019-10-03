package io.github.achmadhafid.firestore_view_model

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

inline val DocumentSnapshot.isFromCache
    get() = metadata.isFromCache

inline val DocumentSnapshot.hasPendingWrites
    get() = metadata.hasPendingWrites()

inline val DocumentSnapshot.isSynced
    get() = !isFromCache && !hasPendingWrites

inline fun <reified T> DocumentSnapshot.of(objectBuilder: T.(DocumentSnapshot) -> Unit): T? =
    toObject(T::class.java)?.apply { objectBuilder(this@of) }

inline fun <reified T> DocumentChange.of(objectBuilder: T.(QueryDocumentSnapshot) -> Unit) =
    document.toObject(T::class.java).apply { objectBuilder(document) }

inline fun <reified T> QuerySnapshot.extracts(
    objectBuilder: T.(QueryDocumentSnapshot) -> Unit
): Triple<List<T>, List<T>, List<T>> {

    val addedList    = mutableListOf<T>()
    val modifiedList = mutableListOf<T>()
    val deletedList  = mutableListOf<T>()

    documentChanges.forEach { change ->
        with(change.of(objectBuilder)) {
            when (change.type) {
                DocumentChange.Type.ADDED    -> addedList.add(this)
                DocumentChange.Type.MODIFIED -> modifiedList.add(this)
                DocumentChange.Type.REMOVED  -> deletedList.add(this)
            }
        }
    }

    return Triple(addedList, modifiedList, deletedList)
}
