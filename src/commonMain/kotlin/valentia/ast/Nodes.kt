package valentia.ast

import valentia.parser.BaseConsumer
import valentia.sema.ResolutionContext
import valentia.sema.SymbolProvider

abstract class Node {
    var reader: BaseConsumer? = null
    var spos: Int = -1
    var epos: Int = -1
    val rangeStr: String? get() = reader?.readAbsoluteRange(spos, epos)
    var nodeAnnotations: Annotations? = null

    private var _cachedType: TypeNode? = null
    protected open fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode =
        TODO("${this::class} $this")
    fun getType(resolutionContext: ResolutionContext): TypeNode {
        if (_cachedType == null) _cachedType = getTypeUncached(resolutionContext)
        return _cachedType!!
    }
}

data class Annotations(val items: List<AnnotationNodes> = emptyList()) {
    companion object {
        val EMPTY = Annotations()
    }
}

data class Modifiers(val items: List<Any> = emptyList()) {
    companion object {
        val EMPTY = Modifiers()
    }
    constructor(vararg items: Any) : this(items.toList())
    val modifiers by lazy { items.filterIsInstance<Modifier>().toSet() }
    val annotations by lazy { Annotations(items.filterIsInstance<AnnotationNodes>()) }
    val labels by lazy { items.filterIsInstance<LabelNode>() }
    fun isEmpty(): Boolean = items.isEmpty()
    operator fun contains(item: Modifier): Boolean = item in modifiers
    val isEnum: Boolean get() = ClassModifier.ENUM in this
}

abstract class ExprOrStm : Node() {
    fun toStm(): Stm = when (this) {
        is Stm -> this
        is Expr -> ExprStm(this)
        else -> TODO()
    }
}

data class TypeConstraint(
    val id: String,
    val type: TypeNode,
    val annotations: Annotations = Annotations.EMPTY,
) : Node()

data class LabelNode(val id: String) : Node()

fun <T : Node> T.annotated(annotations: Annotations): T {
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
    val topLevelDecls: List<Decl> = emptyList(),
) : Node() {
    val symbolsByName by lazy {
        topLevelDecls.groupBy { it.declName }
    }
    val symbolProvider = SymbolProvider {
        symbolsByName[it] ?: emptyList()
    }
}

data class EnumEntry(val id: String) : Node()

data class DummyNode(val dummy: Unit = Unit) : Node()

fun <T : Node> T.enrich(node: Node): T = enrich(node.reader, node.spos, node.epos)
fun <T : Node> T.enrich(reader: BaseConsumer, spos: Int): T = enrich(reader, spos, reader.pos)
fun <T : Node> T.enrich(reader: BaseConsumer?, spos: Int, epos: Int): T {
    this.reader = reader
    this.spos = spos
    this.epos = epos
    return this
}

// Sub Types

abstract class SubTypeInfo : Node()
data class ExplicitDelegation(val type: TypeNode, val delegate: Expr) : SubTypeInfo()
data class ConstructorInvocation(val type: TypeNode, val args: List<Expr> = listOf()) : SubTypeInfo() {
    constructor(type: TypeNode, vararg args: Expr) : this(type, args.toList())
}
data class BasicSubTypeInfo(val type: TypeNode) : SubTypeInfo()

// Type

abstract class TypeNode : Node()

data class DefinitelyNonNullableType(
    val type1: TypeNode,
    val mods1: Modifiers,
    val type2: TypeNode,
    val mods2: Modifiers,
) : TypeNode()

data class NamedTypeNode(val type: TypeNode, val name: String? = null) {
    constructor(param: Parameter) : this(param.type, param.id)
}

data class UnificationExprType(val exprs: List<ExprOrStm>) : TypeNode() {
    constructor(vararg exprs: ExprOrStm?) : this(exprs.filterNotNull())
}
data class FuncTypeNode(val ret: TypeNode, val params: List<NamedTypeNode>, val receiver: TypeNode? = null, val suspendable: Boolean = false) : TypeNode()
data class MultiType(val types: List<TypeNode>) : TypeNode() {
    constructor(vararg types: TypeNode) : this(types.toList())
}
data class SimpleType(val name: String) : TypeNode()
data class GenericType(val type: TypeNode, val generics: List<TypeNode>) : TypeNode()
data class NullableType(val type: TypeNode) : TypeNode()

val UnknownType = SimpleType("unknown")
val DynamicType = SimpleType("dynamic")
val BoolType = SimpleType("Boolean")
val CharType = SimpleType("Char")
val StringType = SimpleType("String")
val IntType = SimpleType("Int")
val FloatType = SimpleType("Float")

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

open class Decl(val declName: String) : Node() {
    open val jsName: String get() = declName
}

data class TypeAliasDecl(
    val id: String,
    val type: TypeNode,
    val types: List<TypeParameter>? = null,
    val modifiers: Modifiers = Modifiers(),
) : Decl(id)
data class ConstructorDelegationCall(val kind: String, val exprs: List<Expr>)
data class ConstructorDecl(
    val params: List<FuncValueParam> = emptyList(),
    val body: Stm? = null,
    val constructorDelegationCall: ConstructorDelegationCall? = null,
) : Decl("constructor")
data class InitDecl(val stm: Stm) : Decl("init")
data class CompanionObjectDecl(val name: String?) : Decl(name ?: "companion object")
data class ClassDecl(
    val kind: String,
    val name: String,
    val subTypes: List<SubTypeInfo>? = null,
    val body: List<Decl>? = null,
) : Decl(name)
data class ObjectDecl(val name: String) : Decl(name)
data class FunDecl constructor(
    val name: String,
    val params: List<FuncValueParam> = emptyList(),
    val retType: TypeNode? = null,
    val where: List<TypeConstraint>? = null,
    val body: Stm? = null,
    val receiver: TypeNode? = null,
    val modifiers: Modifiers = Modifiers(),
) : Decl(name) {
    val jsHash by lazy { params.map { it.type }.hashCode() and 0x7FFFFFFF }
    override val jsName by lazy {
        if (params.isEmpty()) name else "$name\$${jsHash.toString(16)}"
    }

    val isSuspend: Boolean get() = FunctionModifier.SUSPEND in modifiers

    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode {
        return FuncTypeNode(UnknownType, params.map { NamedTypeNode(it.type) })
        //TODO("${this::class} $this")
    }
}
sealed abstract class VariableDeclBase(declName: String) : Decl(declName)
data class VariableDecl(val id: String, val type: TypeNode? = null, val expr: Expr? = null, val delegation: Boolean = false, val annotations: Annotations = Annotations.EMPTY) : VariableDeclBase(id)
data class MultiVariableDecl(
    val decls: List<VariableDecl>,
    val expr: Expr? = null,
    val delegation: Boolean = false,
    val type: TypeNode? = null,
) : VariableDeclBase(decls.joinToString(",") { it.declName }) {
    constructor(vararg decls: VariableDecl, expr: Expr? = null) : this(decls.toList(), expr)
}
fun <T : VariableDeclBase> T.withAssignment(expr: Expr, delegation: Boolean = false): T {
    return when (this) {
        is VariableDecl -> this.copy(expr = expr, delegation = delegation) as T
        is MultiVariableDecl -> this.copy(expr = expr, delegation = delegation) as T
        else -> TODO()
    }
}

// ID

data class Identifier(val parts: List<String>) : Expr() {
    constructor(str: String) : this(str.split("."))
    val fqname: String = parts.joinToString(".")
    override fun toString(): String = "Identifier($fqname)"
}

// Expressions

abstract class Expr : ExprOrStm()

data class LambdaFunctionExpr(val stms: List<Stm> = emptyList(), val params: List<VariableDeclBase>? = null) : Expr() {
}

data class AnonymousFunctionExpr(val decl: FunDecl) : Expr()

abstract class AssignableExpr : Expr()

data class NavigationExpr(val op: String, val expr: Expr, val key: Any) : AssignableExpr()

data class TypeArgumentsAssignableSuffixExpr(val expr: Expr, val types: List<TypeNode>) : AssignableExpr()

data class CallableReferenceExt(val type: TypeNode?, val kind: String) : Expr()
data class ObjectLiteralExpr(
    val delegationSpecifiers: List<SubTypeInfo>? = null,
    val body: List<Decl>? = null,
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
data class TryCatchExpr(val body: Node, val catches: List<Catch> = emptyList(), val finally: Stm? = null) : Expr() {
    data class Catch(val local: String, val type: TypeNode, val body: Stm)
}
data class Temp(val type: TypeNode) : Expr()
data class SuperExpr(val label: String? = null, val type: TypeNode? = null) : AssignableExpr()
data class IfExpr(val cond: Expr, val trueBody: ExprOrStm, val falseBody: ExprOrStm? = null) : Expr()
data class BreakExpr(val label: String? = null) : Expr()
data class ContinueExpr(val label: String? = null) : Expr()
data class ReturnExpr(val expr: Expr?, val label: String? = null) : Expr()
data class ThrowExpr(val expr: Expr) : Expr()

data class Parameter(val id: String, val type: TypeNode)
data class ParameterOptType(val id: String, val type: TypeNode?, val modifiers: Modifiers? = null, val expr: Expr? = null)
data class FuncValueParam(val id: String, val type: TypeNode)
data class TypeParameter(val id: String, val type: TypeNode?)

data class ClassParameter(val id: String)

enum class UnaryPreOp(val str: String) {
    INCR("++"), DECR("--"), MINUS("-"), PLUS("+"), EXCL("!");
    companion object {
        val BY_ID = entries.associateBy { it.str }
    }
}

enum class UnaryPostOp(val str: String) {
    INCR("++"), DECR("--"), NOT_NULL("!!");

    companion object {
        val BY_ID = entries.associateBy { it.str }
    }
}

data class CastExpr(val expr: Expr, val targetType: TypeNode, val kind: String) : Expr()
data class CallExpr(val expr: Expr, val params: List<Expr> = emptyList(), val lambdaArg: Expr? = null, val typeArgs: List<TypeNode>? = null) : Expr()
data class IndexedExpr(val expr: Expr, val indices: List<Expr>) : AssignableExpr()
data class UnaryPostOpExpr(val expr: Expr, val op: UnaryPostOp) : Expr()
data class UnaryPreOpExpr(val op: UnaryPreOp, val expr: Expr) : Expr() {
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode {
        val type = expr.getType(resolutionContext)
        if (type == IntType) return type
        TODO("UnaryPreOpExpr type=$type, expr=$expr")
        return super.getTypeUncached(resolutionContext)
    }
}

data class IdentifierExpr(val id: String) : AssignableExpr()

data class OpSeparatedExprs(val ops: List<String>, val exprs: List<Expr>) : Expr()

open class LiteralExpr(val literal: Any?) : Expr()

data class NullLiteralExpr(val dummy: Unit = Unit) : LiteralExpr(null)
data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value) {
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = BoolType
}
data class CharLiteralExpr(val value: Char) : LiteralExpr(value) {
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = CharType
}
data class IntLiteralExpr(val value: Long, val isLong: Boolean = false, val isUnsigned: Boolean = false) : LiteralExpr(value) {
    override fun toString(): String = "$value${if (isUnsigned) "U" else ""}${if (isLong) "L" else ""}.lit"
    //override fun toString(): String = "IntLiteralExpr($value${if (isUnsigned) "U" else ""}${if (isLong) "L" else ""})"
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = IntType
}
data class StringLiteralExpr(val value: String) : LiteralExpr(value) {
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = StringType
}
data class InterpolatedStringExpr(val chunks: List<Chunk>) : Expr() {
    interface Chunk
    data class StringChunk(val string: String) : Chunk
    data class ExpressionChunk(val expr: Expr) : Chunk
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = StringType
}

open class IncompleteExpr(val message: String) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: TypeNode) : Expr()
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr()

data class ThisExpr(val id: Identifier?) : AssignableExpr() {
}

// Statements


open class Stm : ExprOrStm()

fun Stm.withModifiers(mods: Modifiers): Stm {
    if (mods.isEmpty()) return this
    return when (this) {
        is WhileLoopStm -> this.copy(modifiers = mods)
        else -> {
            //println("TODO: Stm.withModifiers: $mods")
            this
        }
    }
}

/** JavaScript for example only supports try catch statements, not expressions */
data class TryCatchStm(val body: Stm, val catches: List<Catch> = emptyList(), val finally: Stm = EmptyStm()) : Stm() {
    data class Catch(val local: String, val type: TypeNode, val body: Stm)
}

data class ReturnStm(val expr: Expr?) : Stm()
data class IfStm(val cond: Expr, val btrue: Stm, val bfalse: Stm? = null) : Stm()
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
data class DeclStm(val decl: Decl) : Stm()

abstract class LoopStm : Stm() {
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr, val vardecl: VariableDeclBase?, val body: Stm? = null, val annotations: Annotations = Annotations.EMPTY) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr, val body: Stm, val modifiers: Modifiers? = null) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?) : LoopStm() {
}
