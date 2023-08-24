package valentia.sema

import valentia.ast.*


val Node.program: Program? get() = if (this is Program) this else parentDecl?.program

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
                    // @TODO: DO asterisk
                    //println("FileNode.import: $validImport")
                    if (validImport != null) {
                        program!!.getTypeDeclByIdentifierOrNull(validImport.identifier)
                            ?.let { return@getOrPut it }
                        //val packages = program!!.getAvailablePackages(Identifier(validImport.identifier.partsButLast))
                        //for (pack in packages) {
                        //    pack.symbols[type.name]
                        //        ?.filterIsInstance<TypeDecl>()
                        //        ?.firstOrNull()
                        //        ?.let { return@getOrPut it }
                        //}
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
                // check in the same package
                run {

                }
                parentDecl?.resolveSimpleType(type)
            }
            is Package -> {
                decl.symbols[type.name]?.filterIsInstance<TypeDecl>()?.firstOrNull()
            }
            //is BaseConstructorDecl -> TODO()
            //is ClassOrObjectDecl -> TODO()
            //is CompanionObjectDecl -> TODO()
            //is FileNode -> TODO()
            //is FunDecl -> TODO()
            //is InitDecl -> TODO()
            //is Module -> TODO()
            //is Program -> TODO()
            //is TypeAliasDecl -> TODO()
            //is MultiVariableDecl -> TODO()
            //is VariableDecl -> TODO()
            else -> parentDecl?.resolveSimpleType(type)
        } ?: UnknownTypeDecl("${type.name}\$Unresolved")
    }
}
