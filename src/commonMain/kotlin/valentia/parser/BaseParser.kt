package valentia.parser

import valentia.ast.Node
import valentia.ast.enrich
import valentia.util.isLetterOrDigitOrUndescore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TokenReader(val tokens: List<Token>) : BaseTokenReader {
    override var pos: Int = 0
    override val len: Int get() = tokens.size
    override fun peek(offset: Int): Token = tokens.getOrElse(pos + offset) { EOFToken }
    override fun toString(): String {
        val peekStr = (0 until 12).map { peek(it) }.filter { it !is NLToken }.joinToString("") { it.str }
        val prevPeekStr = (-6 until 0).map { peek(it) }.filter { it !is NLToken }.joinToString("") { it.str }
        return "TokenReader(line=${peek().line}, pos=$pos/$len, peek=${peek(0)}, peek=\"$peekStr\", prev=\"$prevPeekStr\")"
    }
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

fun BaseTokenReader.peekSkipping(count: Int): Token {
    var n = 0
    var remaining = count
    while (hasMore) {
        while (peek(n) is HiddenToken || peek(n) is NLToken) n++
        if (remaining > 0) {
            remaining--
            n++
        } else {
            break
        }
    }
    return peek(n)
}

interface BaseTokenReader : BaseConsumer {
    fun peek(offset: Int = 0): Token
    fun read(): Token {
        while (peek() is HiddenToken) pos++
        return peek().also { skip() }
    }
    override fun EOF(message: String?) {
        check(eof) { "Not EOF found but ${peek()} message=$message" }
    }

    override fun skip(count: Int) {
        pos += count
        while (peek() is HiddenToken) pos++
    }

    fun expectAny(strs: Set<String>): String {
        return expectAnyOpt(strs) ?: error("Couldn't find ${strs} in $this")
    }

    fun <T> expectAnyOpt(strs: Map<String, T>): T? {
        val str = peek().str
        return strs[str]?.also { skip() }
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
    override fun peek(count: Int, offset: Int): String = str.substring((pos + offset).coerceIn(0 .. len), (pos + count + offset).coerceIn(0 .. len))
    override fun readAbsoluteRange(start: Int, end: Int): String = str.substring(start, end)

    override fun toString(): String = "StrReader(pos=$pos, len=$len, peek='${peek(8)}' prev=${peek(4, -4)})"
}

//@Deprecated("", ReplaceWith("debug { message }"))
//inline fun BaseConsumer.debug(message: String) = debug { message }

inline fun BaseConsumer.debug(message: () -> String) {
    //kotlin.io.println(message)
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

    fun unexpected(reason: String? = null): Nothing = TODO("reason=$reason")

    //open fun Hidden() {
    //}

    fun expect(str: String, reason: String? = null) {
        if (!matches(str, consume = true)) error("Expected '$str' in '$reason' but found $this")
    }

    fun expectRet(str: String, reason: String? = null): String {
        expect(str, reason)
        return str
    }

    @Deprecated("")
    fun expectAny(vararg strs: String): String {
        return expectAnyOpt(*strs) ?: error("Couldn't find ${strs.toList()} in $this")
    }

    fun expectAny(str1: String, consume: Boolean = true): String {
        if (matches(str1, consume = consume)) return str1
        error("Couldn't find $str1 in $this")
    }

    fun expectAny(str1: String, str2: String, consume: Boolean = true): String {
        if (matches(str1, consume = consume)) return str1
        if (matches(str2, consume = consume)) return str2
        error("Couldn't find $str1 or $str2 in $this")
    }

    fun expectAny(str1: String, str2: String, str3: String, consume: Boolean = true): String {
        if (matches(str1, consume = consume)) return str1
        if (matches(str2, consume = consume)) return str2
        if (matches(str3, consume = consume)) return str3
        error("Couldn't find $str1, $str2 or $str3 in $this")
    }

    fun expectAny(str1: String, str2: String, str3: String, str4: String, consume: Boolean = true): String {
        if (matches(str1, consume = consume)) return str1
        if (matches(str2, consume = consume)) return str2
        if (matches(str3, consume = consume)) return str3
        if (matches(str4, consume = consume)) return str4
        error("Couldn't find $str1, $str2, $str3 or $str4 in $this")
    }

    @Deprecated("")
    fun expectAnyOpt(vararg strs: String, consume: Boolean = true): String? {
        for (str in strs) if (matches(str, consume = consume)) return str
        return null
    }

    fun expectOpt(str: String): Boolean {
        return matches(str, consume = true)
    }

    fun EOF(message: String? = null) {
        check(eof) { "Not EOF found (message=$message)" }
    }

}

inline fun <T> BaseConsumer.expectAndRecoverSure(start: String, end: String, reason: String? = null, nullIfNotMatching: Boolean = false, block: () -> T): T {
    return expectAndRecover(start, end, reason, nullIfNotMatching, block) ?: TODO("expectAndRecoverSure recovery reason=$reason : $this")
}

//@OptIn(ExperimentalContracts::class)
inline fun <T> BaseConsumer.expectAndRecover(start: String, end: String, reason: String? = null, nullIfNotMatching: Boolean = false, block: () -> T): T? {
    //contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val spos = pos
    if (nullIfNotMatching) {
        if (!expectOpt(start)) return null.also { pos = spos }
    } else {
        expect(start, reason)
    }
    try {
        val res = block()
        if (nullIfNotMatching) {
            if (!expectOpt(end)) return null.also { pos = spos }
        } else {
            expect(end, reason)
        }
        return res
    } catch (e: IllegalStateException) {
        println("RECOVERING NOT IMPLEMENTED! ERROR: ${e.message}")
        throw e
    }
    return null
}


interface BaseReader : BaseConsumer {
    fun peekChar(offset: Int = 0): Char
    fun readChar(): Char = peekChar().also { skip(1) }
    fun peek(count: Int, offset: Int = 0): String
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

@Deprecated("slow")
inline fun <T> BaseConsumer.OR(vararg funcs: () -> T?, name: String? = null): T {
    val rpos = pos
    val exceptions = arrayListOf<Throwable>()
    for (func in funcs) {
        pos = rpos
        return func() ?: continue
    }
    pos = rpos
    error("  Couldn't match any of OR[$name][$this] [${funcs.size}]:  \n${exceptions.joinToString("\n  ")}")
}

inline fun <T> BaseConsumer.ORNullable(vararg funcs: () -> T?, name: String? = null): T? {
    val rpos = pos
    for (func in funcs) {
        pos = rpos
        return func() ?: continue
    }
    pos = rpos
    return null
}

/*
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

inline fun <T1, T2, T3> BaseConsumer.ORDis(func1: () -> T1?, func2: () -> T2?, func3: () -> T3?): Disjunction3<T1, T2, T3>? {
    val rpos = pos
    //val exceptions = arrayListOf<Throwable>()
    for (n in 0 until 2) {
        pos = rpos
        //try {
            return Disjunction3(when (n) {
                0 -> func1()
                1 -> func2()
                2 -> func3()
                else -> TODO()
            } ?: continue)
        //} catch (e: IllegalStateException) {
        //    exceptions += e
        //}
    }
    //error("Couldn't match any of OR [2]: ${exceptions.toList()}")
    //error("Couldn't match any of OR")
    return null
}

 */

@OptIn(ExperimentalContracts::class)
@Deprecated("")
inline fun <T> BaseConsumer.opt(block: () -> T): T? {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var res: T? = null
    val spos = pos
    res = block()
    if (res == null) pos = spos
    //resetOnException { res = block() }
    return res
}

inline fun <T> BaseConsumer.multiple(atLeastOne: Boolean, catch: Boolean = true, block: () -> T?): List<T> {
    val out = arrayListOf<T>()
    if (atLeastOne) {
        out += block() ?: TODO("multiple atLeastOne=$atLeastOne")
    }
    loop@while (hasMore) {
        if (out.size >= 1000) {
            TODO("Too long")
        }
        val oldPos = pos
        //try {
            val res = block()
            if (res == null) {
                pos = oldPos
                break@loop
            }
            out += res
        //} catch (e: IllegalStateException) {
        //    if (!catch) throw e
        //    pos = oldPos
        //    break@loop
        //}
    }
    return out
}

inline fun <T> BaseConsumer.oneOrMore(catch: Boolean = true, block: () -> T?): List<T> = multiple(atLeastOne = true, catch = catch, block)
inline fun <T> BaseConsumer.zeroOrMore(catch: Boolean = true, block: () -> T?): List<T> = multiple(atLeastOne = false, catch = catch, block)

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
