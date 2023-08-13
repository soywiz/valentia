package valentia.parser

import valentia.ast.Node
import valentia.ast.enrich
import valentia.util.Disjunction2
import valentia.util.Disjunction3
import valentia.util.isLetterOrDigitOrUndescore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TokenReader(val tokens: List<Token>) : BaseTokenReader {
    override var pos: Int = 0
    override val len: Int get() = tokens.size
    override fun peek(offset: Int): Token = tokens.getOrElse(pos + offset) { EOFToken }
    override fun toString(): String = "TokenReader(peek=${peek()}, pos=$pos, len=$len)"
}

inline fun <reified T: Token> BaseTokenReader.expect(): T {
    val token = read()
    if (token !is T) error("Not a ${T::class} but $token")
    return token
}

inline fun <reified T: Token> BaseTokenReader.expectOpt(consume: Boolean = true): T? {
    val token = peek()
    return if (token is T) {
        skip()
        token
    } else null
}

interface BaseTokenReader : BaseConsumer {
    fun peek(offset: Int = 0): Token
    fun read(): Token {
        while (peek() is HiddenToken) pos++
        return peek().also { skip() }
    }
    override fun EOF() {
        check(eof) { "Not EOF found but ${peek()}" }
    }

    override fun skip(count: Int) {
        pos += count
        while (peek() is HiddenToken) pos++
    }

    fun expectAny(strs: Set<String>): String {
        val str = peek().str
        if (str !in strs) error("Couldn't find ${strs} in $this")
        return str
    }

    fun <T> expectAnyOpt(strs: Map<String, T>): T? {
        val str = peek().str
        return strs[str]
    }

    fun <T> expectAny(strs: Map<String, T>): T {
        return expectAnyOpt(strs) ?: error("Couldn't find ${strs} in $this")
    }

    fun expectAnyOpt(strs: Set<String>, consume: Boolean = true): String? {
        val token = peek()
        if (token.str in strs) {
            if (consume) {
                skip()
            }
            return token.str
        }
        return null
    }

    override fun readAbsoluteRange(start: Int, end: Int): String {
        val oldPos = pos
        try {
            val out = arrayListOf<String>()
            for (p in start until end) {
                pos = p
                out += peek().str
            }
            return out.joinToString("")
        } finally {
            pos = oldPos
        }
    }

    override fun matches(str: String, consume: Boolean): Boolean {
        if (peek().str == str) {
            if (consume) skip()
            return true
        }
        return false
    }
}

interface BaseTokenParser : BaseTokenReader {
}

open class StrReader(val str: String) : BaseReader {
    override var pos: Int = 0
    override val len: Int get() = str.length
    override fun peekChar(offset: Int): Char = str.getOrElse(pos + offset) { '\u0000' }
    override fun peek(count: Int): String = str.substring(pos, (pos + count).coerceAtMost(len))
    override fun readAbsoluteRange(start: Int, end: Int): String = str.substring(start, end)

    override fun toString(): String = "StrReader(pos=$pos, len=$len, peek='${peek(8)}')"
}

interface BaseConsumer {
    var pos: Int
    val len: Int
    val eof: Boolean get() = pos >= len
    val hasMore: Boolean get() = !eof
    fun readAbsoluteRange(start: Int, end: Int): String
    fun skip(count: Int = 1) { pos += count }
    fun matches(str: String, consume: Boolean = false): Boolean

    fun reportError(e: Throwable) {
    }

    fun debug(message: Any?) {
        //kotlin.io.println(message)
    }

    fun unexpected(reason: String? = null): Nothing = TODO("reason=$reason")

    //open fun Hidden() {
    //}

    fun expect(str: String) {
        if (!matches(str, consume = true)) error("Expected '$str' but found $this")
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

interface BaseReader : BaseConsumer {
    fun peekChar(offset: Int = 0): Char
    fun readChar(): Char = peekChar().also { skip(1) }
    fun peek(count: Int): String
    fun read(count: Int): String = peek(count).also { skip(it.length) }

    fun expectChar(char: Char) {
        val c = peekChar()
        when (c) {
            char -> skip(1)
            else -> error("Expected '$char' but found '·c'")
        }
    }

    override fun matches(str: String, consume: Boolean): Boolean {
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

inline fun <T> BaseConsumer.resetOnException(block: () -> T): Boolean {
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

}

inline fun <T> BaseConsumer.OR(vararg funcs: () -> T?, name: String? = null): T {
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

inline fun <T1, T2> BaseConsumer.ORDis(func1: () -> T1?, func2: () -> T2?): Disjunction2<T1, T2> {
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

inline fun <T1, T2, T3> BaseConsumer.ORDis(func1: () -> T1?, func2: () -> T2?, func3: () -> T3?): Disjunction3<T1, T2, T3> {
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
inline fun <T> BaseConsumer.opt(block: () -> T): T? {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var res: T? = null
    resetOnException { res = block() }
    return res
}

inline fun <T> BaseConsumer.multiple(atLeastOne: Boolean, block: () -> T?): List<T> {
    val out = arrayListOf<T>()
    if (atLeastOne) {
        out += block() ?: TODO("multiple atLeastOne=$atLeastOne")
    }
    loop@while (hasMore) {
        val oldPos = pos
        try {
            val res = block()
            if (res == null) {
                pos = oldPos
                break@loop
            }
            out += res
        } catch (e: IllegalStateException) {
            pos = oldPos
            break@loop
        }
    }
    return out
}

inline fun <T> BaseConsumer.oneOrMore(block: () -> T?): List<T> = multiple(atLeastOne = true, block)
inline fun <T> BaseConsumer.zeroOrMore(block: () -> T?): List<T> = multiple(atLeastOne = false, block)

inline fun BaseConsumer.recoverWithExpect(token: String, block: () -> Unit) {
    TODO("recoverWithExpect")
}

inline fun <T : Node> BaseConsumer.enrich(crossinline block: () -> T): T {
    val spos = pos
    return block().also { it.enrich(this@enrich, spos) }
}

inline fun <T : Node> BaseConsumer.enrichOpt(crossinline block: () -> T?): T? {
    val spos = pos
    return block()?.also { it.enrich(this@enrichOpt, spos) }
}
