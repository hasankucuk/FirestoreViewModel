package io.github.achmadhafid.firestore_view_model.read

class ReadDocumentEvent<out T : Any>(private val state: ReadDocumentState<T>) {

    private var consumers = mutableMapOf<Int, Boolean>()

    fun getState(id: Int? = null): ReadDocumentState<T> {
        id?.let {
            if (consumers[id] == null) consumers[id] = false
            else if (consumers[id] == false) consumers[id] = true
        }
        return state
    }

    fun getState(id: String?): ReadDocumentState<T> = getState(id?.hashCode())

    fun hasBeenConsumed(id: Int) = consumers[id] ?: false

    fun hasBeenConsumed(id: String) = hasBeenConsumed(id.hashCode())

}
