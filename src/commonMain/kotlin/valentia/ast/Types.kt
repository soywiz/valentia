package valentia.ast

// Type

abstract class Type : Node()

//data class PackageName(val parts: List<String>)
//data class FqName(val parts: List<String>)

data class SimpleType(val name: String) : Type() {
    var fqname: String? = null
    //val _package: String? = null
    //val _simpleName: String get() = name
    val fullName: String get() = fqname ?: name
    var resolvedDecl: Decl? = null
    override fun toString(): String = name
    companion object {
        const val ID = 1
    }
}
data class NullableType(val type: Type) : Type() {
    companion object {
        const val ID = 2
    }
}
data class GenericType(val base: Type, val generics: List<Type>) : Type() {
    constructor(base: Type, vararg generics: Type) : this(base, generics.toList())
    companion object {
        const val ID = 3
    }
}

data class FuncType(val ret: Type?, val params: List<Item>, val receiver: Type? = null, val suspendable: Boolean = false) : Type() {
    val flags = if (suspendable) 1 else 0

    data class Item(val type: Type?, val name: String? = null) {
        constructor(param: Parameter) : this(param.type, param.id)
        override fun toString(): String = if (name != null) "$name: $type" else "$type"
    }

    override fun toString(): String = "(${params.joinToString(", ")}) -> $ret"

    companion object {
        const val ID = 4
    }
}
data class MultiType(val types: List<Type>) : Type() {
    constructor(vararg types: Type) : this(types.toList())

    companion object {
        const val ID = 5
    }
}

data class DefinitelyNonNullableType(
    val type1: Type,
    val mods1: Modifiers,
    val type2: Type,
    val mods2: Modifiers,
) : Type() {
    companion object {
        const val ID = 6
    }
}

// @TODO: This should be resolved instead of serialized
data class UnificationExprType(val exprs: List<ExprOrStm>) : Type() {
    constructor(vararg exprs: ExprOrStm?) : this(exprs.filterNotNull())
    companion object {
        const val ID = 255
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

fun FuncType.suspendable(): FuncType = this.copy(suspendable = true)

fun Type.withModifiers(modifiers: List<Any>): Type {
    if (modifiers.isNotEmpty()) println("TODO: TypeNode.withModifiers: $modifiers")
    return this
}

fun Type.nullable(): NullableType = if (this is NullableType) this else NullableType(this)
