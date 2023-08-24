package valentia.sema

import valentia.ast.*
import valentia.util.Extra


val Node.program: Program? get() = if (this is Program) this else parentDecl?.program

fun SimpleType.resolveSimpleType(): TypeDecl = resolvedDecl ?: this.resolveSimpleType(this).also {
    this.resolvedDecl = it
}

fun Type.getSimpleType(): SimpleType {
    return when (this) {
        //is DefinitelyNonNullableType -> this.type1.getSimpleType()
        is DefinitelyNonNullableType -> TODO()
        //is FuncType -> SimpleType("Function1")
        is FuncType -> TODO()
        is GenericType -> this.base.getSimpleType()
        is MultiType -> this.types.first().getSimpleType()
        is NullableType -> this.type.getSimpleType()
        is SimpleType -> this
        is UnificationExprType -> TODO()
    }
}

fun Node.resolveType(type: Type): TypeDecl {
    return resolveSimpleType(type.getSimpleType())
}

fun Node.resolveSimpleType(type: SimpleType): TypeDecl = currentDecl!!.resolveSimpleType(type)

fun Decl.resolveSimpleType(type: SimpleType): TypeDecl {
    val decl = this
    //println("type=$type -> decl=$decl")
    return decl.simpleTypeCache.getOrPut(type.name) {
        when (decl) {
            is ClassDecl -> {
                // Check subtypes
                val potential = decl.bodyAllByName[type.name]?.filterIsInstance<TypeDecl>()
                if (potential?.isNotEmpty() == true) {
                    potential?.first()?.let { return@getOrPut it }
                }
                //println("ClassDecl: $potential")
                parentDecl?.resolveSimpleType(type)
            }

            is FileNode -> {
                // check in the same file
                run {
                    val potential = decl.topLevelDeclsByName[type.name]?.filterIsInstance<TypeDecl>() ?: emptyList()
                    //println("FileNode.topLevel: $potential")
                    if (potential.isNotEmpty()) {
                        return@getOrPut potential.first()
                    }
                }
                // check simple in imports
                run {
                    val validImport = decl.importsById[type.name]
                    if (validImport != null) {
                        program!!.getTypeDeclByIdentifierOrNull(validImport.identifier)
                            ?.let { return@getOrPut it }
                    }
                }
                // check asterisk imports
                run {
                    for (import in decl.asteriskImports) {
                        val id = Identifier(import.identifier.parts + type.name)
                        program!!.getTypeDeclByIdentifierOrNull(id)
                            ?.let { return@getOrPut it }
                    }
                }
                parentDecl?.resolveSimpleType(type)
            }

            is Package -> {
                decl.symbols[type.name]?.filterIsInstance<TypeDecl>()?.firstOrNull()
            }
            else -> parentDecl?.resolveSimpleType(type)
        } ?: UnknownTypeDecl("${type.name}\$Unresolved")
    }
}

val ClassLikeDecl.directResolvedSubTypes: List<Decl> by Extra.PropertyThis {
    val subtypes = (this.subTypes ?: emptyList()).map { it.type }
    println(subtypes)
    val decls: List<Decl> = subtypes.mapNotNull { resolve(it.toString()).firstOrNull() }
    decls.distinct()
}
