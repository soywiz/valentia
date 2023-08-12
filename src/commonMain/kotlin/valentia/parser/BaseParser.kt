package valentia.parser

import valentia.ast.Node
import valentia.ast.enrich
import valentia.util.Disjunction2
import valentia.util.Disjunction3
import valentia.util.isLetterOrDigitOrUndescore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class StrReader(val str: String) : BaseReader {
    override var pos: Int = 0
    override val len: Int get() = str.length
    override fun peekChar(offset: Int): Char = str.getOrElse(pos + offset) { '\u0000' }
    override fun peek(count: Int): String = str.substring(pos, (pos + count).coerceAtMost(len))
    override fun readAbsoluteRange(start: Int, end: Int): String = str.substring(start, end)

    override fun toString(): String = "StrReader(pos=$pos, len=$len, peek='${peek(8)}')"
}

interface BaseReader {
    var pos: Int
    val len: Int
    val eof: Boolean get() = pos >= len
    val hasMore: Boolean get() = !eof

    fun readAbsoluteRange(start: Int, end: Int): String

    fun skip(count: Int = 1) { pos += count }
    fun peekChar(offset: Int = 0): Char
    fun readChar(): Char = peekChar().also { skip(1) }
    fun peek(count: Int): String
    fun read(count: Int): String = peek(count).also { skip(it.length) }

    fun matches(str: String, consume: Boolean = false): Boolean {
        for (n in str.indices) {
            if (peekChar(n) != str[n]) return false
        }
        if (str[0].isLetter()) {
            if (peekChar(str.length).isLetterOrDigitOrUndescore()) {
                // Not a full identifier!!
                return false
            }
        }
        if (consume) skip(str.length)
        return true
    }
}

inline fun BaseReader.readWhile(block: (Char) -> Boolean): String = readUntil { !block(it) }

inline fun BaseReader.readUntil(block: (Char) -> Boolean): String {
    val spos = pos
    while (hasMore) {
        val c = peekChar()
        if (block(c)) {
            break
        } else {
            skip()
        }
    }
    return readAbsoluteRange(spos, pos)
}

inline fun <T> BaseReader.keep(block: () -> T): T {
    val oldPos = pos
    try {
        return block()
    } finally {
        pos = oldPos
    }
}

data class StringSet(val strings: List<String>) {
    constructor(vararg strings: String) : this(strings.toList())
}

inline fun <T> BaseReader.resetOnException(block: () -> T): Boolean {
    val oldPos = pos
    try {
        block()
        return true
    } catch (e: IllegalStateException) {
        pos = oldPos
        return false
    }
}

interface BaseParser : BaseReader {
    fun reportError(e: Throwable)

    fun debug(message: Any?) {
        //kotlin.io.println(message)
    }

    fun unexpected(reason: String? = null): Nothing = TODO("reason=$reason")

    //open fun Hidden() {
    //}

    fun expect(str: String) {
        if (!matches(str, consume = true)) error("Expected '$str' but found $this")
    }
    fun expectChar(char: Char) {
        val c = peekChar()
        when (c) {
            char -> skip(1)
            else -> error("Expected '$char' but found '·c'")
        }
    }

    fun expectAny(vararg strs: String): String {
        return expectAnyOpt(*strs) ?: error("Couldn't find ${strs.toList()} in $this")
    }

    fun expectAnyOpt(vararg strs: String, consume: Boolean = true): String? {
        for (str in strs) if (matches(str, consume = consume)) return str
        return null
    }

    fun expectOpt(str: String): Boolean {
        return matches(str, consume = true)
    }

    fun <T> expectAndRecoverSure(start: String, end: String, block: () -> T): T {
        return expectAndRecover(start, end, block) ?: TODO("expectAndRecoverSure recovery")
    }

    //@OptIn(ExperimentalContracts::class)
    fun <T> expectAndRecover(start: String, end: String, block: () -> T): T? {
        //contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        expect(start)
        try {
            val res = block()
            expect(end)
            return res
        } catch (e: IllegalStateException) {
            debug("RECOVERING NOT IMPLEMENTED!")
            throw e
        }
        return null
    }

    fun EOF() {
        check(eof) { "Not EOF found" }
    }

}

inline fun <T> BaseParser.OR(vararg funcs: () -> T?, name: String? = null): T {
    val rpos = pos
    val exceptions = arrayListOf<Throwable>()
    for (func in funcs) {
        pos = rpos
        try {
            return func() ?: continue
        } catch (e: IllegalStateException) {
            exceptions += e
        }
    }
    pos = rpos
    error("Couldn't match any of OR[$name][$this] [${funcs.size}]: ${exceptions.toList()}")
}

inline fun <T1, T2> BaseParser.ORDis(func1: () -> T1?, func2: () -> T2?): Disjunction2<T1, T2> {
    val rpos = pos
    val exceptions = arrayListOf<Throwable>()
    for (n in 0 until 2) {
        pos = rpos
        try {
            return Disjunction2(when (n) {
                0 -> func1()
                1 -> func2()
                else -> TODO()
            } ?: continue)
        } catch (e: IllegalStateException) {
            exceptions += e
        }
    }
    error("Couldn't match any of OR [2]: ${exceptions.toList()}")
}

inline fun <T1, T2, T3> BaseParser.ORDis(func1: () -> T1?, func2: () -> T2?, func3: () -> T3?): Disjunction3<T1, T2, T3> {
    val rpos = pos
    val exceptions = arrayListOf<Throwable>()
    for (n in 0 until 2) {
        pos = rpos
        try {
            return Disjunction3(when (n) {
                0 -> func1()
                1 -> func2()
                2 -> func3()
                else -> TODO()
            } ?: continue)
        } catch (e: IllegalStateException) {
            exceptions += e
        }
    }
    error("Couldn't match any of OR [2]: ${exceptions.toList()}")
}

@OptIn(ExperimentalContracts::class)
inline fun <T> BaseParser.opt(block: () -> T): T? {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var res: T? = null
    resetOnException { res = block() }
    return res
}

inline fun <T> BaseParser.multiple(atLeastOne: Boolean, block: () -> T?): List<T> {
    val out = arrayListOf<T>()
    if (atLeastOne) {
        out += block() ?: TODO("multiple")
    }
    while (hasMore) {
        val oldPos = pos
        try {
            out += block() ?: break
        } catch (e: IllegalStateException) {
            pos = oldPos
            break
        }
    }
    return out
}

inline fun <T> BaseParser.oneOrMore(block: () -> T?): List<T> = multiple(atLeastOne = true, block)
inline fun <T> BaseParser.zeroOrMore(block: () -> T?): List<T> = multiple(atLeastOne = false, block)

inline fun BaseParser.recoverWithExpect(token: String, block: () -> Unit) {
    TODO("recoverWithExpect")
}

inline fun <T : Node> BaseParser.enrich(crossinline block: () -> T): T {
    val spos = pos
    return block().also { it.enrich(this@enrich, spos) }
}

inline fun <T : Node> BaseParser.enrichOpt(crossinline block: () -> T?): T? {
    val spos = pos
    return block()?.also { it.enrich(this@enrichOpt, spos) }
}