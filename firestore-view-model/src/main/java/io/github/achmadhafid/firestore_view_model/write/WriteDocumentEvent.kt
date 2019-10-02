package io.github.achmadhafid.firestore_view_model.write

class WriteDocumentEvent(private val state: WriteDocumentState) {

    private var hasBeenConsumed = false

    fun getState(ignoreSingleStateIfHasBeenConsumed: Boolean = true): WriteDocumentState =
        if (state.isSingleState) {
            if (hasBeenConsumed && ignoreSingleStateIfHasBeenConsumed) WriteDocumentState.transientState
            else {
                hasBeenConsumed = true
                state
            }
        } else state
}
