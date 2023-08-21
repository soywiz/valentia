package valentia.ast

// Type

abstract class TypeNode : Node()

data class SimpleType(val name: String) : TypeNode() {
    var fqname: String? = null
    var resolvedDecl: Decl? = null
    override fun toString(): String = name
    companion object {
        const val ID = 0
    }
}
data class NullableType(val type: TypeNode) : TypeNode() {
    companion object {
        const val ID = 1
    }
}
data class GenericType(val base: TypeNode, val generics: List<TypeNode>) : TypeNode() {
    companion object {
        const val ID = 2
    }
}

data class FuncTypeNode(val ret: TypeNode?, val params: List<Item>, val receiver: TypeNode? = null, val suspendable: Boolean = false) : TypeNode() {
    data class Item(val type: TypeNode?, val name: String? = null) {
        constructor(param: Parameter) : this(param.type, param.id)
        override fun toString(): String = if (name != null) "$name: $type" else "$type"
    }

    override fun toString(): String = "(${params.joinToString(", ")}) -> $ret"

    companion object {
        const val ID = 3
    }
}
data class MultiType(val types: List<TypeNode>) : TypeNode() {
    constructor(vararg types: TypeNode) : this(types.toList())

    companion object {
        const val ID = 4
    }
}

data class DefinitelyNonNullableType(
    val type1: TypeNode,
    val mods1: Modifiers,
    val type2: TypeNode,
    val mods2: Modifiers,
) : TypeNode() {
    companion object {
        const val ID = 5
    }
}

// @TODO: This should be resolved instead of serialized
data class UnificationExprType(val exprs: List<ExprOrStm>) : TypeNode() {
    constructor(vararg exprs: ExprOrStm?) : this(exprs.filterNotNull())
    companion object {
        const val ID = 6
    }
}

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
    if (modifiers.isNotEmpty()) println("TODO: TypeNode.withModifiers: $modifiers")
    return this
}

fun TypeNode.nullable(): NullableType = if (this is NullableType) this else NullableType(this)
