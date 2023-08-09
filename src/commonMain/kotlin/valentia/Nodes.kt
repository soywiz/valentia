package valentia

open class Node {
}

open class Expr : Node() {
}

open class Stm : Node() {
}

open class LoopStm : Node() {
}

/** Iterates over a collection */
open class ForLoopStm(val expr: Expr?, val vardecl: Node?, val body: Stm?, val annotations: List<Node> = emptyList()) : LoopStm() {
}

/** Executes 0 or more times */
open class WhileLoopStm(val cond: Expr?, val body: Unit) : LoopStm() {
}

/** Executes 1 or more times */
open class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
