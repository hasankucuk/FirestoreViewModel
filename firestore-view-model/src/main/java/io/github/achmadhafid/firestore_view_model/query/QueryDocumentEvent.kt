package io.github.achmadhafid.firestore_view_model.query

class QueryDocumentEvent<out T : Any>(private val state: QueryDocumentState<T>) {

    private var consumers = mutableMapOf<Int, Boolean>()

    fun getState(id: Int? = null): QueryDocumentState<T> {
        id?.let {
            if (consumers[id] == null) consumers[id] = false
            else if (consumers[id] == false) consumers[id] = true
        }
        return state
    }

    fun getState(id: String): QueryDocumentState<T> = getState(id.hashCode())

    fun hasBeenConsumed(id: Int) = consumers[id] ?: false

    fun hasBeenConsumed(id: String) = hasBeenConsumed(id.hashCode())

}
