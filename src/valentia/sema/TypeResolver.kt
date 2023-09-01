package valentia.sema

import valentia.ast.*
import valentia.util.Extra


val INode.program: Program? get() = if (this is Program) this else parentDecl?.program

fun SimpleType.resolveSimpleType(): TypeDecl = resolvedDecl ?: this.resolveSimpleType(this).also {
    this.resolvedDecl = it
}

fun Type.getSimpleType(): SimpleType {
    return when (this) {
        is DefinitelyNonNullableType -> this.type1.getSimpleType()
        is FuncType -> SimpleType("Function${this.params.size}")
        is GenericType -> this.base.getSimpleType()
        is MultiType -> this.types.first().getSimpleType()
        is NullableType -> this.type.getSimpleType()
        is SimpleType -> this
        is UnificationExprType -> TODO()
        is TemplateType -> TODO()
    }
}

fun INode.resolveType(type: Type): TypeDecl {
    return resolveSimpleType(type.getSimpleType())
}

fun INode.resolveSimpleType(type: SimpleType): TypeDecl {
    if (currentDecl == null) {
        TODO()
    }
    return currentDecl!!.resolveSimpleType(type)
}

fun IDecl.resolveSimpleType(type: SimpleType): TypeDecl {
    val decl = this
    //println("type=$type -> decl=$decl")
    return decl.simpleTypeCache.getOrPut(type.name) {
        when (decl) {
            is LambdaFunctionExpr -> {
                //TODO("IDecl.resolveSimpleType: $type")
                parentDecl?.resolveSimpleType(type)
            }
            is FunDecl -> {
                decl.typeParamsById[type.name]?.let { return@getOrPut it }
                parentDecl?.resolveSimpleType(type)
            }
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

val ClassLikeDecl.directResolvedSubTypes: List<IDecl> by Extra.PropertyThis {
    val subtypes = (this.subTypes ?: emptyList()).map { it.type }
    //println("ClassLikeDecl.directResolvedSubTypes: $subtypes")
    val decls: List<IDecl> = subtypes.mapNotNull { resolve(it.toString()).firstOrNull() }
    decls.distinct()
}
