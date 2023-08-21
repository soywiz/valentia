package valentia.ast

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
    var resolvedDecl: Decl? = null
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
