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

// Decl

open class DeclNode : Node()

data class VariableDecl(val id: String, val type: TypeNode?) : DeclNode()
data class VariableDecls(val decls: List<VariableDecl>) : DeclNode() {
    constructor(vararg decls: VariableDecl) : this(decls.toList())
}

// ID

data class Identifier(val parts: List<String>) : Node() {
    constructor(str: String) : this(str.split("."))
    val fqname: String = parts.joinToString(".")
    override fun toString(): String = "Identifier($fqname)"
}

// Expressions

open class Expr : Node() {
}

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

open class LiteralExpr(val literal: Any?) : Expr()

data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value)
data class IntLiteralExpr(val value: Long) : LiteralExpr(value)
data class LongLiteralExpr(val value: Long) : LiteralExpr(value)

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

data class EmptyStm(val dummy: Unit = Unit) : Stm()

abstract class LoopStm : Node() {
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr?, val vardecl: VariableDecls?, val body: Stm?, val annotations: List<Node> = emptyList()) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr?, val body: Stm) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
