package valentia.parser

import valentia.ast.Node
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class BaseParser {
    fun unexpected(reason: String? = null): Nothing = TODO("reason=$reason")

    fun OR(vararg func: () -> Unit) {

    }

    fun expect(str: String) {
        TODO()
    }

    //@OptIn(ExperimentalContracts::class)
    fun <T> expectAndRecover(start: String, end: String, block: () -> T): T? {
        //contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        TODO()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> opt(block: () -> T): T? {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        TODO()
    }

    inline fun <T> oneOrMore(block: () -> T): List<T> {
        TODO()
    }

    inline fun <T> zeroOrMore(block: () -> T): List<T> {
        TODO()
    }

    inline fun recoverWithExpect(token: String, block: () -> Unit) {
        TODO()
    }

    fun peekIdentifier(): String {
        TODO()
    }

    inline fun <T : Node> enrich(block: () -> T): T {
        // @TODO: Put in the node all the tokens expected
        return block()
    }
}
