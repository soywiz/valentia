package valentia.util

fun <T> whileMap(cond: (Int) -> Boolean, gen: () -> T): List<T> {
    val out = arrayListOf<T>()
    while (cond(out.size)) out += gen()
    return out
}