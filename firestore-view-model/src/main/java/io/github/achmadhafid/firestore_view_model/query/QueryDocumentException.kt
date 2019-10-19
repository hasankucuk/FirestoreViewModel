package io.github.achmadhafid.firestore_view_model.query

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class QueryDocumentException {
    object Offline : QueryDocumentException()
    object Unauthenticated : QueryDocumentException()
    data class FirestoreException(val firestoreException: FirebaseFirestoreException) :
        QueryDocumentException()
}
