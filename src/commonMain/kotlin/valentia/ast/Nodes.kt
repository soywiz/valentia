package valentia.ast

import valentia.parser.BaseReader

open class Node {
    var reader: BaseReader? = null
    var spos: Int = -1
    var epos: Int = -1
    val rangeStr: String? get() = reader?.readAbsoluteRange(spos, epos)
}
fun <T : Node> T.enrich(reader: BaseReader, spos: Int, epos: Int = reader.pos): T {
    this.reader = reader
    this.spos = spos
    this.epos = epos
    return this
}

// Type

open class TypeNode : Node()
object UnknownType : TypeNode()
object DynamicType : TypeNode()
data class SimpleType(val name: String) : TypeNode()

// ID

open class Identifier : Node()

// Expressions

open class Expr : Node() {
}

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

open class LiteralExpr(val literal: Any?) : Expr()

data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value)
data class IntLiteralExpr(val value: Int) : LiteralExpr(value)

open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: TypeNode) : Expr()
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr()

data class ThisExpr(val id: Identifier?) : Expr() {
}

// Statements


open class Stm : Node() {
}

data class Stms(val stms: List<Stm>) : Stm() {
    constructor(vararg stms: Stm) : this(stms.toList())
}

object EmptyStm : Stm()

abstract class LoopStm : Node() {
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr?, val vardecl: Node?, val body: Stm?, val annotations: List<Node> = emptyList()) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr?, val body: Stm) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
