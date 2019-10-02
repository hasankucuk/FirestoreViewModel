package io.github.achmadhafid.firestore_view_model.read

class ReadDocumentEvent<out T : Any>(private val state: ReadDocumentState<T>) {

    private var hasBeenConsumed = false

    fun getState(ignoreSingleStateIfHasBeenConsumed: Boolean = true): ReadDocumentState<T> =
        if (state.isSingleState) {
            if (hasBeenConsumed && ignoreSingleStateIfHasBeenConsumed) ReadDocumentState.transientState
            else {
                hasBeenConsumed = true
                state
            }
        } else state

}
