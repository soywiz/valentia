package valentia.ast

import valentia.parser.BaseReader

open class Node {
    var reader: BaseReader? = null
    var spos: Int = -1
    var epos: Int = -1
    val rangeStr: String? get() = reader?.readAbsoluteRange(spos, epos)

    var nodeAnnotations: List<AnnotationNodes>? = null
}

fun <T : Node> T.annotated(annotations: List<AnnotationNodes>): T {
    this.nodeAnnotations = annotations
    return this
}

data class AnnotationNodes(
    val annotations: List<AnnotationNode>,
) : Node() {
    constructor(vararg annotations: AnnotationNode) : this(annotations.toList())
}

data class AnnotationNode(
    val name: TypeNode,
    val args: List<Expr>? = null,
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

// Sub Types

abstract class SubTypeInfo : Node()
data class ExplicitDelegation(val type: TypeNode, val delegate: Expr) : SubTypeInfo()
data class ConstructorInvocation(val type: TypeNode, val args: List<Expr>) : SubTypeInfo()
data class BasicSubTypeInfo(val type: TypeNode) : SubTypeInfo()

// Type

abstract class TypeNode : Node()

data class FuncTypeNode(val suspendable: Boolean = false) : TypeNode()
data class MultiType(val types: List<TypeNode>) : TypeNode() {
    constructor(vararg types: TypeNode) : this(types.toList())
}
object UnknownType : TypeNode()
object DynamicType : TypeNode()
data class SimpleType(val name: String) : TypeNode()
data class GenericType(val type: TypeNode, val generics: List<TypeNode>) : TypeNode()
data class NullableType(val type: TypeNode) : TypeNode()

fun FuncTypeNode.suspendable(): FuncTypeNode = this.copy(suspendable = true)

fun TypeNode.withModifiers(modifiers: List<Any>): TypeNode {
    if (modifiers.isNotEmpty()) {
        println("TODO: TypeNode.withModifiers: $modifiers")
    }
    return this
}

fun TypeNode.nullable(): NullableType {
    return if (this is NullableType) this else NullableType(this)
}

// Modifiers

interface Modifier {
    val id: String
}
enum class ClassModifier(override val id: String) : Modifier {
    ENUM("enum"), SEALED("sealed"), ANNOTATION("annotation"), DATA("data"), INNER("inner"), VALUE("value");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class MemberModifier(override val id: String) : Modifier {
    OVERRIDE("override"), LATE_INIT("lateinit");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VisibilityModifier(override val id: String) : Modifier {
    PUBLIC("public"), PRIVATE("private"), INTERNAL("internal"), PROTECTED("protected");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VarianceModifier(override val id: String) : Modifier {
    IN("in"), OUT("out");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class FunctionModifier(override val id: String) : Modifier {
    TAILREC("tailrec"), OPERATOR("operator"), INFIX("infix"), INLINE("inline"), EXTERNAL("external"), SUSPEND("suspend");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PropertyModifier(override val id: String) : Modifier {
    CONST("const");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class InheritanceModifier(override val id: String) : Modifier {
    ABSTRACT("abstract"), FINAL("final"), OPEN("open");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ParameterModifier(override val id: String) : Modifier {
    VARARG("vararg"), NOINLINE("noinline"), CROSSINLINE("crossinline");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ReificationModifier(override val id: String) : Modifier {
    REIFIED("reified");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PlatformModifier(override val id: String) : Modifier {
    EXPECT("expect"), ACTUAL("actual");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

enum class AnnotationUseSite(override val id: String) : Modifier {
    FIELD("field"), PROPERTY("property"), GET("get"), SET("set"), RECEIVER("receiver"),
    PARAM("param"), SETPARAM("setparam"), DELEGATE("delegate"), FILE("file");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

// Decl

open class DeclNode : Node()

data class TypeAliasDecl(
    val id: String,
    val type: TypeNode,
    val types: List<TypeParameter>? = null,
    val modifiers: List<Any> = emptyList()
) : DeclNode()
data class ConstructorDecl(val params: List<FuncValueParam>, val body: Stm?) : DeclNode()
data class InitDecl(val stm: Stm) : DeclNode()
data class CompanionObjectDecl(val name: String?) : DeclNode()
data class ClassDecl(val kind: String, val name: String, val subTypes: List<SubTypeInfo>? = null) : DeclNode()
data class ObjectDecl(val name: String) : DeclNode()
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

abstract class Expr : Node()

abstract class AssignableExpr : Expr()

data class TypeArgumentsAssignableSuffixExpr(val expr: Expr, val types: List<TypeNode>) : AssignableExpr()

data class CallableReferenceExt(val type: TypeNode?, val kind: String) : Expr()
data class ObjectLiteralExpr(
    val delegationSpecifiers: List<SubTypeInfo>? = null,
    val body: List<DeclNode>? = null,
    val isData: Boolean = false,
) : Expr()
data class WhenExpr(
    val subject: Subject? = null,
    val entries: List<Entry> = emptyList(),
) : Expr() {
    data class Subject(val expr: Expr? = null, val decl: VariableDecl? = null)
    data class Entry(val conditions: List<Condition>? = null, val body: Stm = EmptyStm())
    data class Condition(val op: String? = null, val expr: Expr = EmptyExpr())
}
data class CollectionLiteralExpr(val items: List<Expr>) : Expr()
data class TryCatchExpr(val body: Node, val catches: List<Catch> = emptyList(), val finally: Stm = EmptyStm()) : Expr() {
    data class Catch(val local: String, val type: TypeNode, val body: Stm)
}
data class SuperExpr(val label: String? = null, val type: TypeNode? = null) : Expr()
data class IfExpr(val cond: Expr?, val trueBody: Node? = null, val falseBody: Node? = null) : Expr()
data class BreakExpr(val label: String? = null) : Expr()
data class ContinueExpr(val label: String? = null) : Expr()
data class ReturnExpr(val expr: Expr?, val label: String? = null) : Expr()
data class ThrowExpr(val expr: Expr) : Expr()
data class Parameter(val id: String, val type: TypeNode)
data class FuncValueParam(val id: String, val type: TypeNode)
data class TypeParameter(val id: String, val type: TypeNode?)

data class ClassParameter(val id: String)

data class CastExpr(val expr: Expr, val targetType: TypeNode, val kind: String) : Expr()
data class CallExpr(val expr: Expr, val params: List<Expr>, val lambdaArg: Expr? = null, val typeArgs: List<TypeNode>? = null) : Expr()
data class IndexedExpr(val expr: Expr, val indices: List<Expr>) : AssignableExpr()
data class UnaryPostOpExpr(val expr: Expr, val op: String) : Expr()
data class UnaryPreOpExpr(val op: String, val expr: Expr) : Expr()

data class IdentifierExpr(val id: String) : AssignableExpr()

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

open class LiteralExpr(val literal: Any?) : Expr()

data class NullLiteralExpr(val dummy: Unit = Unit) : LiteralExpr(null)
data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value)
data class CharLiteralExpr(val value: Char) : LiteralExpr(value)
data class IntLiteralExpr(val value: Long, val isLong: Boolean = false, val isUnsigned: Boolean = false) : LiteralExpr(value) {
    override fun toString(): String = "IntLiteralExpr($value${if (isUnsigned) "U" else ""}${if (isLong) "L" else ""})"
}
data class StringLiteralExpr(val value: String) : LiteralExpr(value)
data class InterpolatedStringExpr(val chunks: List<Chunk>) : Expr() {
    interface Chunk
    data class StringChunk(val string: String) : Chunk
    data class ExpressionChunk(val expr: Expr) : Chunk
}

open class IncompleteExpr(val message: String) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: TypeNode) : Expr()
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr()

data class ThisExpr(val id: Identifier?) : Expr() {
}

// Statements


open class Stm : Node()

/** JavaScript for example only supports try catch statements, not expressions */
data class TryCatchStm(val body: Stm, val catches: List<Catch> = emptyList(), val finally: Stm = EmptyStm()) : Stm() {
    data class Catch(val local: String, val type: TypeNode, val body: Stm)
}

data class ReturnStm(val expr: Expr?) : Stm() {
}

data class AssignStm(val lvalue: Expr, val op: String, val expr: Expr) : Stm()

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
data class WhileLoopStm(val cond: Expr, val body: Stm) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
