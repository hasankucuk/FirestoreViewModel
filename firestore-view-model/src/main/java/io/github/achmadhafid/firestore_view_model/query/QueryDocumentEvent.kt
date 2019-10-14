package io.github.achmadhafid.firestore_view_model.query

class QueryDocumentEvent<out T : Any>(private val state: QueryDocumentState<T>) {

    private var hasBeenConsumed = false

    fun getState(ignoreSingleStateIfHasBeenConsumed: Boolean = true): QueryDocumentState<T> =
        if (state.isSingleState) {
            if (hasBeenConsumed && ignoreSingleStateIfHasBeenConsumed) QueryDocumentState.transientState
            else {
                hasBeenConsumed = true
                state
            }
        } else state

}
