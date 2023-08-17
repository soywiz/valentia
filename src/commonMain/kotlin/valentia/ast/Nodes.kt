package valentia.ast

import valentia.ast.NodeBuilder.Companion.type
import valentia.parser.BaseConsumer
import valentia.sema.ResolutionContext
import valentia.util.Extra

sealed class Node : Extra by Extra.Mixin() {
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
    val modifiersSet by lazy { ModifiersSet(*modifiers.toTypedArray()) }
    val annotations by lazy { Annotations(items.filterIsInstance<AnnotationNodes>()) }
    val labels by lazy { items.filterIsInstance<LabelNode>() }
    val label by lazy { labels.firstOrNull() }
    fun isEmpty(): Boolean = items.isEmpty()
    //operator fun contains(item: Modifier): Boolean = item in modifiers
    operator fun contains(item: Modifier): Boolean = item in modifiersSet
    val isEnum: Boolean get() = ClassModifier.ENUM in this
}

sealed class ExprOrStm : Node() {
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

interface ModifierOrAnnotation

data class AnnotationNodes(
    val annotations: List<AnnotationNode>,
) : Node(), ModifierOrAnnotation {
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
) : Node(), Extra by Extra.Mixin() {
    val symbolsByName by lazy {
        topLevelDecls.groupBy { it.declName }
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

data class SubTypeInfo(
    /** : parentType */
    val type: TypeNode,
    /** : parentType(...args...) */
    val args: List<Expr>? = null,
    /** : Interface by expr */
    val delegate: Expr? = null
) : Node() {
    constructor(type: TypeNode, vararg args: Expr, delegate: Expr? = null) : this(type, args.toList(), delegate)
}

// Type

abstract class TypeNode : Node()

data class DefinitelyNonNullableType(
    val type1: TypeNode,
    val mods1: Modifiers,
    val type2: TypeNode,
    val mods2: Modifiers,
) : TypeNode()

data class NamedTypeNode(val type: TypeNode?, val name: String? = null) {
    constructor(param: Parameter) : this(param.type, param.id)
    override fun toString(): String = if (name != null) "$name: $type" else "$type"
}

data class UnificationExprType(val exprs: List<ExprOrStm>) : TypeNode() {
    constructor(vararg exprs: ExprOrStm?) : this(exprs.filterNotNull())
}
data class FuncTypeNode(val ret: TypeNode?, val params: List<NamedTypeNode>, val receiver: TypeNode? = null, val suspendable: Boolean = false) : TypeNode() {
    override fun toString(): String = "(${params.joinToString(", ")}) -> $ret"
}
data class MultiType(val types: List<TypeNode>) : TypeNode() {
    constructor(vararg types: TypeNode) : this(types.toList())
}
data class SimpleType(val name: String) : TypeNode() {
    override fun toString(): String = name
}
data class GenericType(val type: TypeNode, val generics: List<TypeNode>) : TypeNode()
data class NullableType(val type: TypeNode) : TypeNode()

val DynamicType = SimpleType("dynamic")
val NothingType = SimpleType("Nothing")
val UnknownType = SimpleType("Unknown")
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

inline class ModifiersSet(val values: Int) {
    operator fun plus(other: ModifiersSet): ModifiersSet = ModifiersSet(this.values or other.values)
    operator fun contains(item: Modifier): Boolean = (values and (1 shl item.index)) != 0
    companion object {
        operator fun invoke(vararg modifiers: Modifier): ModifiersSet {
            var out = 0
            for (mod in modifiers) out = out or (1 shl mod.index)
            return ModifiersSet(out)
        }
    }
}

interface Modifier : ModifierOrAnnotation {
    val id: String
    val index: Int
}
enum class ClassModifier(override val id: String, override val index: Int) : Modifier {
    ENUM("enum", 0), SEALED("sealed", 1), ANNOTATION("annotation", 2), DATA("data", 3), INNER("inner", 4), VALUE("value", 5);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class MemberModifier(override val id: String, override val index: Int) : Modifier {
    OVERRIDE("override", 6), LATE_INIT("lateinit", 7);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VisibilityModifier(override val id: String, override val index: Int) : Modifier {
    PUBLIC("public", 8), PRIVATE("private", 9), INTERNAL("internal", 10), PROTECTED("protected", 11);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class VarianceModifier(override val id: String, override val index: Int) : Modifier {
    IN("in", 12), OUT("out", 13);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class FunctionModifier(override val id: String, override val index: Int) : Modifier {
    TAILREC("tailrec", 14), OPERATOR("operator", 15), INFIX("infix", 16), INLINE("inline", 17), EXTERNAL("external", 18), SUSPEND("suspend", 19);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PropertyModifier(override val id: String, override val index: Int) : Modifier {
    CONST("const", 20);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class InheritanceModifier(override val id: String, override val index: Int) : Modifier {
    ABSTRACT("abstract", 21), FINAL("final", 22), OPEN("open", 23);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ParameterModifier(override val id: String, override val index: Int) : Modifier {
    VARARG("vararg", 24), NOINLINE("noinline", 25), CROSSINLINE("crossinline", 26);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class ReificationModifier(override val id: String, override val index: Int) : Modifier {
    REIFIED("reified", 27);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
enum class PlatformModifier(override val id: String, override val index: Int) : Modifier {
    EXPECT("expect", 28), ACTUAL("actual", 29);
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

enum class AnnotationUseSite(val id: String) {
    FIELD("field"), PROPERTY("property"), GET("get"), SET("set"), RECEIVER("receiver"),
    PARAM("param"), SETPARAM("setparam"), DELEGATE("delegate"), FILE("file");
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

// Decl

sealed class Decl(val declName: String) : Node() {
    open val jsName: String get() = declName
}

data class TypeAliasDecl(
    val id: String,
    val type: TypeNode,
    val types: List<TypeParameter>? = null,
    val modifiers: Modifiers = Modifiers(),
) : Decl(id)
enum class DelegationCallKind(val id: String) {
    THIS("this"), SUPER("super");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}
data class ConstructorDelegationCall(
    val kind: DelegationCallKind?,
    val exprs: List<Expr>,
) : Node() {
    var parent: BaseConstructorDecl? = null
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode =
        FuncTypeNode(parent?.parent?.getType(resolutionContext), exprs.map { NamedTypeNode(it.getType(resolutionContext)) })
}

abstract class BaseConstructorDecl() : Decl("constructor") {
    var parent: ClassOrObjectDecl? = null
    abstract val modifiers: Modifiers
    abstract val params: List<FuncValueParam>
    open val constructorDelegationCall: ConstructorDelegationCall? = null
    open val body: Stm? = null
    val jsHash by lazy { params.map { it.type }.hashCode() and 0x7FFFFFFF }
    override val jsName by lazy {
        if (params.isEmpty()) "\$constructor" else "\$constructor\$${jsHash.toString(16)}"
    }
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode {
        val retType = parent?.declName?.type ?: UnknownType
        return FuncTypeNode(retType, params.map { it.toNamedTypeNode() })
    }
}

data class PrimaryConstructorDecl(
    val classParams: List<ClassParameter> = emptyList(),
    override val modifiers: Modifiers = Modifiers.EMPTY,
) : BaseConstructorDecl() {
    constructor(vararg classParams: ClassParameter, modifiers: Modifiers = Modifiers.EMPTY) : this(classParams.toList(), modifiers)
    constructor(vararg pairs: Pair<String, TypeNode>, modifiers: Modifiers = Modifiers.EMPTY) : this(pairs.map { ClassParameter(it.first, it.second) }, modifiers)

    override val params: List<FuncValueParam> = classParams.map {
        FuncValueParam(it.id, it.type)
    }
}
data class ConstructorDecl(
    override val params: List<FuncValueParam> = emptyList(),
    override val body: Stm? = null,
    override val constructorDelegationCall: ConstructorDelegationCall? = null,
    override val modifiers: Modifiers = Modifiers.EMPTY,
) : BaseConstructorDecl() {
    init {
        constructorDelegationCall?.parent = this
    }
}
data class InitDecl(val stm: Stm) : Decl("init")
data class CompanionObjectDecl(val name: String?) : Decl(name ?: "companion object")

enum class ClassKind(
    val isInterface: Boolean = false,
) {
    OBJECT,
    CLASS,
    ENUM_CLASS,
    INTERFACE(isInterface = true),
    FUN_INTERFACE(isInterface = true);
}

abstract class ClassOrObjectDecl(open val name: String, open val kind: ClassKind) : Decl(name) {
    // @TODO: Extract primary constructor val/var to Variable declarations
    open val primaryConstructor: PrimaryConstructorDecl? = null
    open val body: List<Decl>? = null
    open val subTypes: List<SubTypeInfo>? = null
    val bodyAll by lazy { listOfNotNull(primaryConstructor) + (body ?: emptyList()) }
    val constructors: List<BaseConstructorDecl> by lazy {
        bodyAll.filterIsInstance<BaseConstructorDecl>().also {
            for (constructor in it) {
                constructor.parent = this
            }
        }
    }
}
data class ClassDecl(
    override val kind: ClassKind,
    override val name: String,
    override val subTypes: List<SubTypeInfo>? = null,
    override val body: List<Decl>? = null,
    override val primaryConstructor: PrimaryConstructorDecl? = null,
) : ClassOrObjectDecl(name, kind) {
}
data class ObjectDecl(
    override val name: String,
    override val body: List<Decl>? = null,
    override val subTypes: List<SubTypeInfo>? = null,
) : ClassOrObjectDecl(name, ClassKind.OBJECT)
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
sealed abstract class VariableDeclBase(
    declName: String,
    modifiers: Modifiers = Modifiers.EMPTY,
) : Decl(declName)
data class VariableDecl(
    val id: String,
    val type: TypeNode? = null,
    val expr: Expr? = null,
    val delegation: Boolean = false,
    val receiver: TypeNode? = null,
    val annotations: Annotations = Annotations.EMPTY,
    val modifiers: Modifiers = Modifiers.EMPTY,
    val getter: FunDecl? = null,
    val setter: FunDecl? = null,
) : VariableDeclBase(id, modifiers)
data class MultiVariableDecl(
    val decls: List<VariableDecl>,
    val expr: Expr? = null,
    val delegation: Boolean = false,
    val type: TypeNode? = null,
    val receiver: TypeNode? = null,
    val modifiers: Modifiers = Modifiers.EMPTY,
) : VariableDeclBase(decls.joinToString(",") { it.declName }, modifiers) {
    constructor(vararg decls: VariableDecl, expr: Expr? = null, modifiers: Modifiers = Modifiers.EMPTY) : this(decls.toList(), expr, modifiers = modifiers)
}

fun <T : VariableDeclBase> T.withSetter(
    setter: FunDecl?
): T = when (this) {
    is VariableDecl -> this.copy(setter = setter) as T
    is MultiVariableDecl -> TODO("Unsupported setter for destructuring")
    else -> TODO()
}

fun <T : VariableDeclBase> T.withGetter(
    getter: FunDecl?
): T = when (this) {
    is VariableDecl -> this.copy(getter = getter) as T
    is MultiVariableDecl -> TODO("Unsupported setter for destructuring")
    else -> TODO()
}

fun <T : VariableDeclBase> T.withAssignment(
    expr: Expr,
    delegation: Boolean = false,
    receiver: TypeNode? = null
): T {
    return when (this) {
        is VariableDecl -> this.copy(expr = expr, delegation = delegation, receiver = receiver) as T
        is MultiVariableDecl -> this.copy(expr = expr, delegation = delegation, receiver = receiver) as T
        else -> TODO()
    }
}

// ID

data class Identifier(val parts: List<String>) : Expr() {
    constructor(str: String) : this(str.split("."))
    val fqname: String = parts.joinToString(".")
    val lastPart: String get() = parts.lastOrNull() ?: ""
    override fun toString(): String = "Identifier($fqname)"
}

// Expressions

sealed class Expr : ExprOrStm()

data class LambdaFunctionExpr(val stms: List<Stm> = emptyList(), val params: List<VariableDeclBase>? = null) : Expr() {
}

data class AnonymousFunctionExpr(val decl: FunDecl) : Expr()

sealed class AssignableExpr : Expr()

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
    data class Entry(val conditions: List<ConditionBase>? = null, val body: Stm = EmptyStm())
    sealed interface ConditionBase
    data class Condition(val expr: Expr = EmptyExpr()) : ConditionBase
    data class ConditionIn(val op: String? = null, val expr: Expr = EmptyExpr()) : ConditionBase
    data class ConditionIs(val op: String? = null, val type: TypeNode? = null) : ConditionBase
}
data class CollectionLiteralExpr(val items: List<Expr>) : Expr()
data class TryCatchExpr(val body: Node, val catches: List<Catch> = emptyList(), val finally: Stm? = null) : Expr() {
    data class Catch(val local: String, val type: TypeNode, val body: Stm)
}
data class Temp(val type: TypeNode, val id: Int) : Expr() {
    override fun toString(): String = "\$temp\$$id"
}
data class SuperExpr(val label: String? = null, val type: TypeNode? = null) : AssignableExpr()
data class TernaryExpr(val cond: Expr, val trueExpr: Expr, val falseExpr: Expr) : Expr()
data class IfExpr(val cond: Expr, val trueBody: ExprOrStm, val falseBody: ExprOrStm? = null) : Expr()
data class BreakExpr(val label: String? = null) : Expr()
data class ContinueExpr(val label: String? = null) : Expr()
data class ReturnExpr(val expr: Expr?, val label: String? = null) : Expr()
data class ThrowExpr(val expr: Expr) : Expr()

data class Parameter(val id: String, val type: TypeNode)
data class ParameterOptType(val id: String, val type: TypeNode?, val modifiers: Modifiers? = null, val expr: Expr? = null)
data class FuncValueParam(val id: String, val type: TypeNode?)
data class TypeParameter(val id: String, val type: TypeNode?)

fun ParameterOptType.toFuncValueParam(): FuncValueParam = FuncValueParam(id, type ?: UnknownType)
fun FuncValueParam.toNamedTypeNode(): NamedTypeNode = NamedTypeNode(this.type, this.id)

data class ClassParameter(
    val id: String,
    val type: TypeNode? = null,
    val expr: Expr? = null,
    val valOrVar: String? = null,
    val modifiers: Modifiers = Modifiers.EMPTY,
) {
    companion object {
        fun VAL(id: String, type: TypeNode? = null): ClassParameter = ClassParameter(id, type, valOrVar = "val")
        fun VAR(id: String, type: TypeNode? = null): ClassParameter = ClassParameter(id, type, valOrVar = "var")

        fun VAL(pair: Pair<String, TypeNode>): ClassParameter = ClassParameter(pair.first, pair.second, valOrVar = "val")
        fun VAR(pair: Pair<String, TypeNode>): ClassParameter = ClassParameter(pair.first, pair.second, valOrVar = "var")
    }
}

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
data class CallExpr(val expr: Expr, val params: List<Expr> = emptyList(), val lambdaArg: Expr? = null, val typeArgs: List<TypeNode>? = null) : Expr() {
    val paramsPlusLambda by lazy { params + listOfNotNull(lambdaArg) }
}
data class IndexedExpr(val expr: Expr, val indices: List<Expr>) : AssignableExpr()
data class UnaryPostOpExpr(val expr: Expr, val op: UnaryPostOp) : AssignableExpr()
data class UnaryPreOpExpr(val op: UnaryPreOp, val expr: Expr) : Expr() {
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode {
        val type = expr.getType(resolutionContext)
        if (type == IntType) return type
        TODO("UnaryPreOpExpr type=$type, expr=$expr")
        return super.getTypeUncached(resolutionContext)
    }
}

data class IdentifierExpr(val id: String) : AssignableExpr()
data class TempExpr(val temp: Temp) : AssignableExpr()

data class OpSeparatedBinaryExprs(val ops: List<String>, val exprs: List<Expr>) : Expr() {
    fun toSimpleOps(): Expr {
        check(ops.size == exprs.size - 1)
        var out = exprs.first()
        for (n in ops.indices) {
            out = BinaryOpExpr(out, ops[n], exprs[n + 1])
        }
        return out
    }
}

data class BinaryOpExpr(val left: Expr, val op: String, val right: Expr) : Expr()

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
    sealed interface Chunk
    data class StringChunk(val string: String) : Chunk
    data class ExpressionChunk(val expr: Expr) : Chunk
    override fun getTypeUncached(resolutionContext: ResolutionContext): TypeNode = StringType
}

open class IncompleteExpr(val message: String) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: TypeNode) : Expr()
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr()

data class ThisExpr(val id: String?) : AssignableExpr() {
}

// Statements


sealed class Stm : ExprOrStm()

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
data class ThrowStm(val expr: Expr) : Stm()
data class AssignStm(val lvalue: Expr, val op: String, val expr: Expr) : Stm()
data class BreakStm(val label: String? = null) : Stm()
data class ContinueStm(val label: String? = null) : Stm()

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

sealed class LoopStm(
) : Stm() {
    abstract val modifiers: Modifiers
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr, val vardecl: VariableDeclBase?, val body: Stm? = null, val annotations: Annotations = Annotations.EMPTY, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr, val body: Stm, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
}
