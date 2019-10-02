package io.github.achmadhafid.firestore_view_model.read

internal data class ReadDocumentConfig(
    val collectionPath: String? = null,
    val documentPath: String? = null,
    val isAuthRequired: Boolean = true,
    val isOnlineRequired: Boolean = false,
    val syncWait: Long = 0L
) {
    val path: String
        get() = collectionPath ?: documentPath!!
}
