package valentia.ast

import valentia.parser.BaseReader

open class Node {
    var reader: BaseReader? = null
    var spos: Int = -1
    var epos: Int = -1
    val rangeStr: String? get() = reader?.readAbsoluteRange(spos, epos)
}

data class AnnotationNodes(
    val annotations: List<AnnotationNode>,
) : Node()

data class AnnotationNode(
    val name: Identifier = Identifier(""),
    val args: List<Expr> = emptyList(),
    val useSite: String? = null,
) : Node()

data class ImportNode(
    val identifier: Identifier,
    val alias: String? = null,
    val all: Boolean = false,
) : Node() {

}

data class FileNode(
    val shebang: String? = null,
    val _package: Identifier? = null,
    val fileAnnotations: List<AnnotationNodes> = emptyList(),
    val imports: List<ImportNode> = emptyList(),
    val topLevelDecls: List<Node> = emptyList(),
) : Node() {

}

data class EnumEntry(val id: String) : Node()

data class DummyNode(val dummy: Unit = Unit) : Node()

fun <T : Node> T.enrich(node: Node): T = enrich(node.reader, node.spos, node.epos)
fun <T : Node> T.enrich(reader: BaseReader, spos: Int): T = enrich(reader, spos, reader.pos)
fun <T : Node> T.enrich(reader: BaseReader?, spos: Int, epos: Int): T {
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
fun TypeNode.nullable(): TypeNode {
    println("TODO: TypeNode.nullable")
    return this
}

// Decl

open class DeclNode : Node()

data class FunDecl(val name: String, val params: List<FuncValueParam> = emptyList(), val body: Stm? = null) : DeclNode()
data class VariableDecl(val id: String, val type: TypeNode?) : DeclNode()
data class VariableDecls(val decls: List<VariableDecl>) : DeclNode() {
    constructor(vararg decls: VariableDecl) : this(decls.toList())
}

// ID

data class Identifier(val parts: List<String>) : Expr() {
    constructor(str: String) : this(str.split("."))
    val fqname: String = parts.joinToString(".")
    override fun toString(): String = "Identifier($fqname)"
}

// Expressions

open class Expr : Node()

data class BreakExpr(val label: String? = null) : Expr()
data class ContinueExpr(val label: String? = null) : Expr()
data class ReturnExpr(val expr: Expr?, val label: String? = null) : Expr()
data class ThrowExpr(val expr: Expr) : Expr()
data class Parameter(val id: String, val type: TypeNode)
data class FuncValueParam(val id: String, val type: TypeNode)
data class TypeParameter(val id: String, val type: TypeNode?)

data class CastExpr(val expr: Expr, val targetType: TypeNode, val kind: String) : Expr()
data class CallExpr(val expr: Expr, val params: List<Expr>, val lambdaArg: Expr? = null, val typeArgs: List<TypeNode>? = null) : Expr()
data class IndexedExpr(val expr: Expr, val indices: List<Expr>) : Expr()
data class UnaryPostOpExpr(val expr: Expr, val op: String) : Expr()
data class UnaryPreOpExpr(val op: String, val expr: Expr) : Expr()

data class IdentifierExpr(val id: String) : Expr()

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

open class LiteralExpr(val literal: Any?) : Expr()

data class NullLiteralExpr(val dummy: Unit = Unit) : LiteralExpr(null)
data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value)
data class IntLiteralExpr(val value: Long, val isLong: Boolean = false, val isUnsigned: Boolean = false) : LiteralExpr(value)
data class StringLiteralExpr(val value: String) : LiteralExpr(value)

open class IncompleteExpr(val message: String) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: TypeNode) : Expr()
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr()

data class ThisExpr(val id: Identifier?) : Expr() {
}

// Statements


open class Stm : Node() {
}

fun List<Stm>.compact(): Stm = when {
    this.isEmpty() -> EmptyStm()
    this.size == 1 -> this.first()
    else -> Stms(this)
}

data class Stms(val stms: List<Stm>) : Stm() {
    constructor(vararg stms: Stm) : this(stms.toList())
}

data class EmptyStm(val dummy: Unit = Unit) : Stm()

data class ExprStm(val expr: Expr) : Stm()
data class DeclStm(val decl: DeclNode) : Stm()

abstract class LoopStm : Stm() {
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr?, val vardecl: VariableDecls?, val body: Stm? = null, val annotations: List<Node> = emptyList()) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr?, val body: Stm) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
