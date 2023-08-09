package valentia.ast

open class Node {
}

// ID

open class Identifier : Node()

// Expressions

open class Expr : Node() {
}

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

data class LiteralExpr(val literal: Any?) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val kind: String, val expr: Expr) : Expr()
data class RangeTestExpr(val kind: String, val expr: Expr) : Expr()

data class ThisExpr(val id: Identifier?) : Expr() {
}

// Statements


open class Stm : Node() {
}

abstract class LoopStm : Node() {
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr?, val vardecl: Node?, val body: Stm?, val annotations: List<Node> = emptyList()) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr?, val body: Unit) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
