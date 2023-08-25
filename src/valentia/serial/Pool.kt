package valentia.serial

open class Pool<T>(
    val items: MutableList<T> = arrayListOf<T>()
) {
    val itemsToIndex = LinkedHashMap<T, Int>()

    fun getOrNull(item: T): Int? = itemsToIndex[item]

    fun getOrThrow(item: T): Int = itemsToIndex[item] ?: error("Can't find $item")

    open operator fun get(item: T): Int {
        itemsToIndex[item]?.let { return it }
        beforeAddingEntry(item)
        val index = items.size
        addEntry(item, index)
        return index
    }

    protected open fun beforeAddingEntry(item: T) {
    }

    protected open fun addEntry(item: T, index: Int) {
        itemsToIndex[item] = index
        items.add(item)
    }

    operator fun get(index: Int): T = items[index]
    override fun hashCode(): Int = items.hashCode()
    override fun equals(other: Any?): Boolean = (other is Pool<*>) && this.items == other.items
    override fun toString(): String = "${this::class.simpleName}($itemsToIndex)"
}