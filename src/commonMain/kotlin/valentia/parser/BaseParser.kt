package valentia.parser

import valentia.ast.Node
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class StrReader(val str: String) : BaseReader {
    override var pos: Int = 0
    override val len: Int get() = str.length
    override fun peekChar(offset: Int): Char = str.getOrElse(pos + offset) { '\u0000' }
}

interface BaseReader {
    var pos: Int
    val len: Int
    val eof: Boolean get() = pos >= len
    val hasMore: Boolean get() = !eof
    fun skip(count: Int = 1) { pos += count }
    fun peekChar(offset: Int = 0): Char
    fun readChar(): Char = peekChar().also { skip(1) }
    //fun peek(count: Int): String = str.substring(pos, (pos + count).coerceAtMost(len))

    fun matches(str: String, consume: Boolean = false): Boolean {
        for (n in str.indices) {
            if (peekChar(n) != str[n]) return false
        }
        if (consume) skip(str.length)
        return true
    }
}

inline fun <T> BaseReader.keep(block: () -> T): T {
    val oldPos = pos
    try {
        return block()
    } finally {
        pos = oldPos
    }
}
inline fun <T> BaseReader.resetOnException(block: () -> T): Boolean {
    val oldPos = pos
    try {
        block()
        return true
    } catch (e: Throwable) {
        pos = oldPos
        return false
    }
}

interface BaseParser : BaseReader {
    fun unexpected(reason: String? = null): Nothing = TODO("reason=$reason")

    fun OR(vararg func: () -> Unit) {
        TODO()
    }

    fun expect(str: String) {
        TODO()
    }
    fun expectChar(char: Char) {
        TODO()
    }

    fun expectAny(vararg strs: String): String {
        return expectAnyOpt(*strs) ?: error("Couldn't find ${strs.toList()}")
    }

    fun expectAnyOpt(vararg strs: String): String? {
        for (str in strs) if (matches(str, consume = true)) return str
        return null
    }

    fun expectOpt(str: String): Boolean {
        return expectAnyOpt(str) != null
    }

    //@OptIn(ExperimentalContracts::class)
    fun <T> expectAndRecover(start: String, end: String, block: () -> T): T? {
        //contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        TODO()
    }

    fun peekIdentifier(): String {
        TODO()
    }

    fun EOF() {
        TODO()
    }

}

@OptIn(ExperimentalContracts::class)
inline fun <T> BaseParser.opt(block: () -> T): T? {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var res: T? = null
    resetOnException { res = block() }
    return res
}

inline fun <T> BaseParser.multiple(atLeastOne: Boolean, block: () -> T): List<T> {
    val out = arrayListOf<T>()
    if (atLeastOne) out += block()
    while (hasMore) {
        if (!resetOnException { out += block() }) break
    }
    return out
}

inline fun <T> BaseParser.oneOrMore(block: () -> T): List<T> = multiple(atLeastOne = true, block)
inline fun <T> BaseParser.zeroOrMore(block: () -> T): List<T> = multiple(atLeastOne = false, block)

inline fun BaseParser.recoverWithExpect(token: String, block: () -> Unit) {
    TODO()
}

inline fun <T : Node> BaseParser.enrich(block: () -> T): T {
    // @TODO: Put in the node all the tokens expected
    return block()
}