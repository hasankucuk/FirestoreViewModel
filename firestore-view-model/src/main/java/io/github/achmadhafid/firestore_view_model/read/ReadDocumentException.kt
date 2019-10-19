package io.github.achmadhafid.firestore_view_model.read

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class ReadDocumentException {
    object Offline : ReadDocumentException()
    object Unauthenticated : ReadDocumentException()
    data class FirestoreException(val firestoreException: FirebaseFirestoreException) :
        ReadDocumentException()
}
