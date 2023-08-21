package valentia.serializing

open class Pool<T> {
    val items = arrayListOf<T>()
    val itemsToIndex = LinkedHashMap<T, Int>()

    operator fun get(item: T): Int {
        itemsToIndex[item]?.let { return it }
        val index = items.size
        itemsToIndex[item] = index
        items.add(item)
        return index
    }

    operator fun get(index: Int): T = items[index]
}