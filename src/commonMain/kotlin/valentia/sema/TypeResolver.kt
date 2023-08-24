package valentia.sema

import valentia.ast.*


val Node.program: Program? get() = if (this is Program) this else parentDecl?.program

fun SimpleType.resolveSimpleType(): TypeDecl = resolvedDecl ?: this.resolveSimpleType(this).also {
    this.resolvedDecl = it
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
