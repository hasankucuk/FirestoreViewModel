package io.github.achmadhafid.firestore_view_model.write

import com.google.firebase.firestore.FirebaseFirestoreException

sealed class WriteDocumentException {
    object Unknown : WriteDocumentException()
    object Offline : WriteDocumentException()
    object Unauthenticated : WriteDocumentException()
    data class FirestoreException(val firestoreException: FirebaseFirestoreException) :
        WriteDocumentException()
}
