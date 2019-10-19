package io.github.achmadhafid.firestore_view_model.write

class WriteDocumentEvent(private val state: WriteDocumentState) {

    private var consumers = mutableMapOf<Int, Boolean>()

    fun getState(id: Int? = null): WriteDocumentState {
        id?.let {
            if (consumers[id] == null) consumers[id] = false
            else if (consumers[id] == false) consumers[id] = true
        }
        return state
    }

    fun getState(id: String?): WriteDocumentState = getState(id?.hashCode())

    fun hasBeenConsumed(id: Int) = consumers[id] ?: false

    fun hasBeenConsumed(id: String) = hasBeenConsumed(id.hashCode())

}
