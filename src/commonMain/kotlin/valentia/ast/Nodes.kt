package valentia.ast

import valentia.ast.NodeBuilder.Companion.type
import valentia.parser.BaseConsumer
import valentia.sema.getAscendantClassByName
import valentia.sema.resolve
import valentia.util.Extra

sealed class Node : Extra by Extra.Mixin() {
    var reader: BaseConsumer? = null
    var spos: Int = -1
    var epos: Int = -1
    val rangeStr: String? get() = reader?.readAbsoluteRange(spos, epos)
    var nodeAnnotations: Annotations? = null
    var parentNode: Node? = null
    val parentDecl: Decl? by lazy { if (parentNode is Decl) parentNode as Decl else parentNode?.parentDecl }
    val currentDecl: Decl? get() = if (this is Decl) this else parentDecl

    fun addNode(item: Node?) {
        item?.parentNode = this
    }

    fun addNode(items: List<Node?>?) {
        if (items == null) return
        for (item in items) addNode(item)
    }

    private var _cachedType: Type? = null
    protected open fun getTypeUncached(): Type =
        TODO("${this::class} $this")
    fun getNodeType(): Type {
        if (_cachedType == null) _cachedType = getTypeUncached()
        return _cachedType!!
    }
}

class Program : Decl("\$program") {
    var semaResolved = false
    val modules = arrayListOf<Module>()
    val modulesById = LinkedHashMap<String?, Module>()

    fun getModule(id: String?): Module = modulesById.getOrPut(id) { Module(this, id).also { modules += it } }

    fun getAvailablePackages(id: Identifier?): List<Package> = modulesById.values.mapNotNull { it.getPackageOrNull(id) }

    fun getDeclsByIdentifier(id: Identifier): List<Decl> = modules.flatMap { it.getDeclsByIdentifier(id) }

    fun getTypeDeclByIdentifierOrNull(id: Identifier): TypeDecl? {
        val types = getDeclsByIdentifier(id).filterIsInstance<TypeDecl>()
        if (types.size > 1) error("More than one type in $id")
        return types.singleOrNull()
    }

    override fun toString(): String = "Program($modulesById)"
}

class Module(val program: Program, val id: String? = null) : Decl("\$module\$$id") {
    init {
        program.addNode(this)
    }
    val packagesById = LinkedHashMap<Identifier?, Package>()

    fun getPackageOrNull(identifier: Identifier?): Package? = packagesById[identifier]

    fun getPackage(identifier: Identifier?): Package {
        return packagesById.getOrPut(identifier) {
            Package(this, identifier)
        }
    }

    fun getDeclsByIdentifier(id: Identifier): List<Decl> {
        val pack = getPackageOrNull(Identifier(id.partsButLast)) ?: return emptyList()
        return pack.symbols[id.lastPart] ?: emptyList()
    }

    fun addFile(file: FileNode) {
        getPackage(file._package).addFile(file)
    }
}
open class Package(val module: Module, val identifier: Identifier?) : Decl("$identifier") {
    init {
        module.addNode(this)
    }
    val program: Program get() = module.program
    val symbols = LinkedHashMap<String, ArrayList<Decl>>()
    val files = arrayListOf<FileNode>()

    private fun getSymbolsByName(name: String): ArrayList<Decl> = symbols.getOrPut(name) { arrayListOf() }

    fun addFile(file: FileNode) {
        files += file
        file.parentNode = this
        file.pack = this
        file.symbolsByName.map { (name, decls) ->
            getSymbolsByName(name).addAll(decls)
        }
    }
}

data class Annotations(val items: List<AnnotationNodes> = emptyList()) {
    companion object {
        val EMPTY = Annotations()
    }
}

sealed interface ModifierLike

data class Modifiers(val items: List<ModifierLike> = emptyList()) {
    companion object {
        val EMPTY = Modifiers()
    }
    constructor(vararg items: ModifierLike) : this(items.toList())
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
    val type: Type,
    val annotations: Annotations = Annotations.EMPTY,
) : Node()

data class LabelNode(val id: String) : Node(), ModifierLike

fun <T : Node> T.annotated(annotations: Annotations): T {
    this.nodeAnnotations = annotations
    return this
}

sealed interface ModifierOrAnnotation : ModifierLike

data class AnnotationNodes(
    val annotations: List<AnnotationNode>,
) : Node(), ModifierOrAnnotation {
    constructor(vararg annotations: AnnotationNode) : this(annotations.toList())
}

fun List<AnnotationNodes>.merge(): AnnotationNodes = AnnotationNodes(this.flatMap { it.annotations })

data class AnnotationNode(
    val type: Type,
    val args: List<Expr>? = null,
    val useSite: AnnotationUseSite = AnnotationUseSite.NULL,
) : Node()

data class ImportNode(
    val identifier: Identifier,
    val alias: String? = null,
    val all: Boolean = false,
) : Node() {

}

data class FileNode(
    val filePath: String = "Unknown.kt",
    val shebang: String? = null,
    val _package: Identifier? = null,
    val fileAnnotations: List<AnnotationNodes> = emptyList(),
    val imports: List<ImportNode> = emptyList(),
    val topLevelDecls: List<Decl> = emptyList(),
) : Decl("\$file\$$filePath"), Extra by Extra.Mixin() {
    val topLevelDeclsByName = topLevelDecls.groupBy { it.declName }

    val importsById by lazy { imports.associateBy { it.identifier.lastPart } }
    val asteriskImports by lazy { imports.filter { it.all } }

    var pack: Package? = null

    init {
        addNode(imports)
        addNode(topLevelDecls)
    }

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
    val type: Type,
    /** : parentType(...args...) */
    val args: List<Expr>? = null,
    /** : Interface by expr */
    val delegate: Expr? = null
) : Node() {
    init {
        addNode(type)
        addNode(args)
        addNode(delegate)
    }
    constructor(type: Type, vararg args: Expr, delegate: Expr? = null) : this(type, args.toList(), delegate)
}

enum class AnnotationUseSite(val id: String) {
    NULL("null"), FIELD("field"), PROPERTY("property"), GET("get"), SET("set"), RECEIVER("receiver"),
    PARAM("param"), SETPARAM("setparam"), DELEGATE("delegate"), FILE("file");
    override fun toString(): String = id
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

// Decl

sealed class Decl(val declName: String) : Node() {
    open val jsName: String get() = declName

    val simpleTypeCache = LinkedHashMap<String, TypeDecl>()
}

data class TypeAliasDecl(
    val id: String,
    val type: Type,
    val types: List<TypeParameter>? = null,
    val modifiers: Modifiers = Modifiers(),
) : TypeDecl(id) {
}
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
    init {
        addNode(exprs)
    }
    var parent: BaseConstructorDecl? = null
    override fun getTypeUncached(): Type =
        FuncType(parent?.parent?.getNodeType(), exprs.map { FuncType.Item(it.getNodeType()) })
}

abstract class BaseConstructorDecl() : CallableDecl("constructor") {
    var parent: ClassLikeDecl? = null
    abstract val modifiers: Modifiers
    abstract val params: List<FuncValueParam>
    open val constructorDelegationCall: ConstructorDelegationCall? = null
    open val body: Stm? = null
    val jsHash by lazy { params.map { it.type }.hashCode() and 0x7FFFFFFF }
    override val jsName by lazy {
        if (params.isEmpty()) "\$constructor" else "\$constructor\$${jsHash.toString(16)}"
    }
    override fun getTypeUncached(): Type {
        val retType = parent?.declName?.type ?: UnknownType
        return FuncType(retType, params.map { it.toNamedTypeNode() })
    }
}

data class PrimaryConstructorDecl(
    val classParams: List<ClassParameter> = emptyList(),
    override val modifiers: Modifiers = Modifiers.EMPTY,
) : BaseConstructorDecl() {
    constructor(vararg classParams: ClassParameter, modifiers: Modifiers = Modifiers.EMPTY) : this(classParams.toList(), modifiers)
    constructor(vararg pairs: Pair<String, Type>, modifiers: Modifiers = Modifiers.EMPTY) : this(pairs.map { ClassParameter(it.first, it.second) }, modifiers)

    init {
        addNode(classParams)
    }

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
        addNode(params)
        addNode(body)
    }
}
data class InitDecl(val stm: Stm) : Decl("init") {
    init {
        addNode(stm)
    }
}
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

sealed class TypeDecl(name: String) : Decl(name) {}

data class UnknownTypeDecl(val name: String) : TypeDecl(name)

abstract class ClassLikeDecl(open val name: String, open val kind: ClassKind) : TypeDecl(name) {
    var fqname: String? = null

    // @TODO: Extract primary constructor val/var to Variable declarations
    open val primaryConstructor: PrimaryConstructorDecl? = null
    open val body: List<Decl>? = null
    open val subTypes: List<SubTypeInfo>? = null

    val primaryVals by lazy {
        primaryConstructor?.classParams
            ?.mapNotNull { it.kind?.let { kind -> VariableDecl(it.id, it.type, it.expr, kind = kind, synth = true) } }
            ?: emptyList()
    }

    val bodyAll: List<Decl> by lazy {
        (primaryVals + listOfNotNull(primaryConstructor) + (body ?: emptyList())).also { list ->
            for (it in list) it.parentNode = this
        }
    }
    val bodyAllByName by lazy {
        bodyAll.groupBy { it.declName }
    }
    val fields by lazy { bodyAll.filterIsInstance<VariableDecl>().filter { it.isField } }
    val properties by lazy { bodyAll.filterIsInstance<VariableDecl>().filter { !it.isField } }
    val constructors: List<BaseConstructorDecl> by lazy {
        bodyAll.filterIsInstance<BaseConstructorDecl>().also {
            for (constructor in it) {
                constructor.parent = this
            }
        }
    }

    val classMembersById: Map<String, List<Decl>> by lazy {
        bodyAll.groupBy { it.declName }
    }
}

data class ClassDecl(
    override val kind: ClassKind,
    override val name: String,
    override val subTypes: List<SubTypeInfo>? = null,
    override val body: List<Decl>? = null,
    override val primaryConstructor: PrimaryConstructorDecl? = null,
) : ClassLikeDecl(name, kind) {
    init {
        addNode(subTypes)
        addNode(body)
        addNode(primaryConstructor)
    }
    override fun getTypeUncached(): Type {
        return SimpleType(name) // @TODO: FqNAme
    }
}
data class ObjectDecl(
    override val name: String,
    override val body: List<Decl>? = null,
    override val subTypes: List<SubTypeInfo>? = null,
) : ClassLikeDecl(name, ClassKind.OBJECT) {
    init {
        addNode(body)
    }
}

sealed class CallableDecl(name: String) : Decl(name)

data class FunDecl constructor(
    val name: String,
    val params: List<FuncValueParam> = emptyList(),
    val retType: Type? = null,
    val where: List<TypeConstraint>? = null,
    val body: Stm? = null,
    val receiver: Type? = null,
    val modifiers: Modifiers = Modifiers(),
) : CallableDecl(name) {
    init {
        addNode(body)
    }

    val jsHash by lazy { params.map { it.type }.hashCode() and 0x7FFFFFFF }
    override val jsName by lazy {
        //if (params.isEmpty()) name else "$name\$${jsHash.toString(16)}"
        "$name\$${jsHash.toString(16)}"
    }

    val isSuspend: Boolean get() = FunctionModifier.SUSPEND in modifiers

    override fun getTypeUncached(): Type {
        return FuncType(UnknownType, params.map { FuncType.Item(it.type) })
        //TODO("${this::class} $this")
    }
}
sealed abstract class VariableDeclBase(
    declName: String,
) : Decl(declName) {
    abstract val modifiers: Modifiers
    abstract val kind: VariableKind
}

enum class VariableKind(val id: String) {
    VAL("val"), VAR("var");
    companion object {
        val BY_ID = entries.associateBy { it.id }
    }
}

data class VariableDecl(
    val id: String,
    val type: Type? = null,
    val expr: Expr? = null,
    val delegation: Boolean = false,
    val receiver: Type? = null,
    val annotations: Annotations = Annotations.EMPTY,
    override val modifiers: Modifiers = Modifiers.EMPTY,
    val getter: FunDecl? = null,
    val setter: FunDecl? = null,
    override val kind: VariableKind = VariableKind.VAL,
    val synth: Boolean = false,
) : VariableDeclBase(id) {
    init {
        addNode(expr)
        addNode(getter)
        addNode(setter)
    }
    val isField get() = !delegation && getter == null && setter == null

    override fun getTypeUncached(): Type {
        return type ?: expr?.getNodeType() ?: UnknownType
    }
}
data class MultiVariableDecl(
    val decls: List<VariableDecl>,
    val expr: Expr? = null,
    val delegation: Boolean = false,
    val type: Type? = null,
    val receiver: Type? = null,
    override val modifiers: Modifiers = Modifiers.EMPTY,
    override val kind: VariableKind = VariableKind.VAL,
) : VariableDeclBase(decls.joinToString(",") { it.declName }) {
    init {
        addNode(decls)
        addNode(expr)
    }
    constructor(vararg decls: VariableDecl, expr: Expr? = null, modifiers: Modifiers = Modifiers.EMPTY, kind: VariableKind = VariableKind.VAL) : this(decls.toList(), expr, modifiers = modifiers, kind = kind)
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
    receiver: Type? = null
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
    val partsButLast: List<String> get() = parts.dropLast(1)
    override fun toString(): String = "Identifier($fqname)"
}

// Expressions

sealed class Expr : ExprOrStm()

data class LambdaFunctionExpr(val stms: List<Stm> = emptyList(), val params: List<VariableDeclBase>? = null) : Expr() {
    init {
        addNode(stms)
        addNode(params)
    }
}

data class AnonymousFunctionExpr(val decl: FunDecl) : Expr()

sealed class AssignableExpr : Expr()

data class TypeArgumentsAssignableSuffixExpr(val expr: Expr, val types: List<Type>) : AssignableExpr()

data class CallableReferenceExt(val type: Type?, val kind: String) : Expr()
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
    data class ConditionIs(val op: String? = null, val type: Type? = null) : ConditionBase
}
data class CollectionLiteralExpr(val items: List<Expr>) : Expr()
data class TryCatchExpr(val body: Node, val catches: List<Catch> = emptyList(), val finally: Stm? = null) : Expr() {
    data class Catch(val local: String, val type: Type, val body: Stm)
}
data class Temp(val type: Type, val id: Int) : Expr() {
    override fun toString(): String = "\$temp\$$id"
}
data class SuperExpr(val label: String? = null, val type: Type? = null) : AssignableExpr() {
    companion object {
        const val ID_UNLABELLED = 31
        const val ID_LABELLED = 32
    }
}
data class TernaryExpr(val cond: Expr, val trueExpr: Expr, val falseExpr: Expr) : Expr() {
    init {
        addNode(cond)
        addNode(trueExpr)
        addNode(falseExpr)
    }
}
data class IfExpr(val cond: Expr, val trueBody: ExprOrStm, val falseBody: ExprOrStm? = null) : Expr() {
    companion object {
        const val ID = 30
    }
    init {
        addNode(cond)
        addNode(trueBody)
        addNode(falseBody)
    }
}
data class BreakExpr(val label: String? = null) : Expr()
data class ContinueExpr(val label: String? = null) : Expr()
data class ReturnExpr(val expr: Expr?, val label: String? = null) : Expr() {
    init {
        addNode(expr)
    }
}
data class ThrowExpr(val expr: Expr) : Expr() {
    init {
        addNode(expr)
    }
}

data class Parameter(val id: String, val type: Type) : Node()
data class ParameterOptType(val id: String, val type: Type?, val modifiers: Modifiers? = null, val expr: Expr? = null) : Node() {
    init {
        addNode(expr)
    }
}
data class FuncValueParam(val id: String, val type: Type?) : Node()
data class TypeParameter(val id: String, val type: Type?) : Node()

fun ParameterOptType.toFuncValueParam(): FuncValueParam = FuncValueParam(id, type ?: UnknownType)
fun FuncValueParam.toNamedTypeNode(): FuncType.Item = FuncType.Item(this.type, this.id)

data class ClassParameter(
    val id: String,
    val type: Type? = null,
    val expr: Expr? = null,
    val kind: VariableKind? = null,
    val modifiers: Modifiers = Modifiers.EMPTY,
) : Node() {
    companion object {
        fun VAL(id: String, type: Type? = null): ClassParameter = ClassParameter(id, type, kind = VariableKind.VAL)
        fun VAR(id: String, type: Type? = null): ClassParameter = ClassParameter(id, type, kind = VariableKind.VAR)

        fun VAL(pair: Pair<String, Type>): ClassParameter = ClassParameter(pair.first, pair.second, kind = VariableKind.VAL)
        fun VAR(pair: Pair<String, Type>): ClassParameter = ClassParameter(pair.first, pair.second, kind = VariableKind.VAR)
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

data class CastExpr(val expr: Expr, val targetType: Type, val safe: Boolean) : Expr() {
    init {
        addNode(expr)
    }
}
sealed class BaseCallExpr : Expr() {
    abstract val params: List<Expr>
    abstract val lambdaArg: Expr?
    abstract val typeArgs: List<Type>?
    val paramsPlusLambda by lazy { params + listOfNotNull(lambdaArg) }

    var resolvedDecl: Decl? = null
    var addThis: Boolean = false

    open fun getFuncType(): FuncType = FuncType(null, params.map { FuncType.Item(it.getNodeType()) })
}
data class CallExpr(val expr: Expr, override val params: List<Expr> = emptyList(), override val lambdaArg: Expr? = null, override val typeArgs: List<Type>? = null) : BaseCallExpr() {
    init {
        addNode(expr)
        addNode(params)
        addNode(lambdaArg)
    }

    override fun getFuncType(): FuncType = FuncType(expr.getNodeType(), params.map { FuncType.Item(it.getNodeType()) })
    //override fun getFuncType(): FuncType = expr.getType() as FuncType
    override fun getTypeUncached(): Type {
        return getFuncType().ret ?: UnknownType
    }

    //var resolvedDecl: Decl? = null
    //var addThis: Boolean = false
}
//data class CallIdExpr(val expr: Expr?, val id: String, val op: String = ".", override val params: List<Expr> = emptyList(), override val lambdaArg: Expr? = null, override val typeArgs: List<TypeNode>? = null) : BaseCallExpr() {
//    var resolvedDecl: Decl? = null
//    var addThis: Boolean = false
//}
data class NavigationExpr(val op: String, val expr: Expr, val key: Any) : AssignableExpr() {
    init {
        addNode(expr)
        if (key is Node) addNode(key)
    }

    var resolvedDecl: Decl? = null

    override fun getTypeUncached(): Type {
        return resolvedDecl?.getNodeType() ?: UnknownType
    }
}
data class IndexedExpr(val expr: Expr, val indices: List<Expr>) : AssignableExpr() {
    init {
        addNode(expr)
    }
}
data class UnaryPostOpExpr(val expr: Expr, val op: UnaryPostOp) : AssignableExpr() {
    init {
        addNode(expr)
    }
}
data class UnaryPreOpExpr(val op: UnaryPreOp, val expr: Expr) : Expr() {
    init {
        addNode(expr)
    }
    override fun getTypeUncached(): Type {
        val type = expr.getNodeType()
        if (type == IntType) return type
        TODO("UnaryPreOpExpr type=$type, expr=$expr")
        return super.getTypeUncached()
    }
}

data class IdentifierExpr(val id: String) : AssignableExpr() {
    var addThis: Boolean = false
    var resolvedDecl: Decl? = null

    override fun getTypeUncached(): Type {
        return resolvedDecl?.getNodeType() ?: resolve(id).firstOrNull()?.getNodeType() ?: UnknownType
    }
}
data class TempExpr(val temp: Temp) : AssignableExpr()

data class OpSeparatedBinaryExprs(val ops: List<String>, val exprs: List<Expr>) : Expr() {
    init {
        addNode(exprs)
    }

    fun toSimpleOps(): Expr {
        check(ops.size == exprs.size - 1)
        var out = exprs.first()
        for (n in ops.indices) {
            out = BinaryOpExpr(out, ops[n], exprs[n + 1])
        }
        return out
    }
}

data class BinaryOpExpr(val left: Expr, val op: String, val right: Expr) : Expr() {
    init {
        addNode(left)
        addNode(right)
    }

    override fun getTypeUncached(): Type {
        // @TODO: Operator overloading
        val leftType = left.getNodeType()
        val rightType = right.getNodeType()
        if (leftType != rightType) {
            println("ERROR: Unsupported BinaryOp type resolution $leftType, $rightType")
            //error("")
            //return UnknownType
        }
        //return leftType
        return rightType
    }
}

sealed class LiteralExpr(val literal: Any?) : Expr()

data class NullLiteralExpr(val dummy: Unit = Unit) : LiteralExpr(null) {
}
data class BoolLiteralExpr(val value: Boolean) : LiteralExpr(value) {
    override fun getTypeUncached(): Type = BoolType
    override fun toString(): String = "$value"
}
data class CharLiteralExpr(val value: Char) : LiteralExpr(value) {
    override fun getTypeUncached(): Type = CharType
    override fun toString(): String = when (value) {
        '\r' -> "'\\r'"
        '\n' -> "'\\n'"
        '\t' -> "'\\t'"
        else -> "'$value'"
    }
}
data class IntLiteralExpr(val value: Long, val isLong: Boolean = false, val isUnsigned: Boolean = false) : LiteralExpr(value) {
    override fun toString(): String = "$value${if (isUnsigned) "U" else ""}${if (isLong) "L" else ""}.lit"
    //override fun toString(): String = "IntLiteralExpr($value${if (isUnsigned) "U" else ""}${if (isLong) "L" else ""})"
    override fun getTypeUncached(): Type = IntType
}
data class StringLiteralExpr(val value: String) : LiteralExpr(value) {
    override fun getTypeUncached(): Type = StringType
}
data class InterpolatedStringExpr(val chunks: List<Chunk>) : Expr() {
    init {
        addNode(chunks)
    }

    sealed class Chunk : Node()
    data class StringChunk(val string: String) : Chunk()
    data class ExpressionChunk(val expr: Expr) : Chunk()
    override fun getTypeUncached(): Type = StringType
}

open class IncompleteExpr(val message: String) : Expr()
open class EmptyExpr : Expr()

data class TypeTestExpr(val base: Expr, val kind: String, val type: Type) : Expr() {
    init {
        addNode(base)
    }
}
data class RangeTestExpr(val base: Expr, val kind: String, val container: Expr) : Expr() {
    init {
        addNode(base)
        addNode(container)
    }
}

data class ThisExpr(val id: String?) : AssignableExpr() {
    override fun getTypeUncached(): Type {
        return this.getAscendantClassByName(id)?.getNodeType() ?: UnknownType
    }
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
    init {
        addNode(body)
        for (catch in catches) addNode(catch.body)
        addNode(finally)
    }
    data class Catch(val local: String, val type: Type, val body: Stm) {
    }
}

data class ReturnStm(val expr: Expr?) : Stm() {
    init {
        addNode(expr)
    }
}
data class IfStm(val cond: Expr, val btrue: Stm, val bfalse: Stm? = null) : Stm() {
    init {
        addNode(cond)
        addNode(btrue)
        addNode(bfalse)
    }
}
data class ThrowStm(val expr: Expr) : Stm() {
    init {

        addNode(expr)
    }
}
data class AssignStm(val lvalue: Expr, val op: String, val expr: Expr) : Stm() {
    init {
        addNode(lvalue)
        addNode(expr)
    }
}
data class BreakStm(val label: String? = null) : Stm()
data class ContinueStm(val label: String? = null) : Stm()

fun List<Stm>.compact(): Stm = when {
    this.isEmpty() -> EmptyStm()
    this.size == 1 -> this.first()
    else -> Stms(this)
}

data class Stms(val stms: List<Stm>) : Stm() {
    constructor(vararg stms: Stm) : this(stms.toList())
    init {
        addNode(stms)
    }
}

data class EmptyStm(val dummy: Unit = Unit) : Stm() {
    override fun toString(): String = "EmptyStm"
}

data class ExprStm(val expr: Expr) : Stm() {
    init {
        addNode(expr)
    }
}
data class DeclStm(val decl: Decl) : Stm() {
    init {
        addNode(decl)
    }
}

sealed class LoopStm(
) : Stm() {
    abstract val modifiers: Modifiers
}

/** Iterates over a collection */
data class ForLoopStm(val expr: Expr, val vardecl: VariableDeclBase?, val body: Stm? = null, val annotations: Annotations = Annotations.EMPTY, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
    init {
        addNode(expr)
        addNode(vardecl)
        addNode(body)
    }
}

/** Executes 0 or more times */
data class WhileLoopStm(val cond: Expr, val body: Stm, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
    init {
        addNode(cond)
        addNode(body)
    }
}

/** Executes 1 or more times */
data class DoWhileLoopStm(val body: Stm?, val cond: Expr?, override val modifiers: Modifiers = Modifiers.EMPTY) : LoopStm() {
    init {
        addNode(body)
        addNode(cond)
    }
}
