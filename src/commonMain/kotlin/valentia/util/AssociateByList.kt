package valentia.util

@Deprecated("", ReplaceWith("groupBy(block)"))
fun <K, V> Iterable<V>.associateByList(block: (V) -> K): Map<K, List<V>> = groupBy(block)

//val out = LinkedHashMap<K, ArrayList<V>>()
//for (item in this) {
//    val key = block(item)
//    val list = out.getOrPut(key) { arrayListOf() }
//    list.add(item)
//}
//return out
