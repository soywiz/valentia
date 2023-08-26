package valentia.sema

import valentia.ast.*

fun INode.resolve(id: String): Sequence<IDecl> = sequence {
    val node = this@resolve
    val body = this@resolve.currentFunctionBody
    if (body != null) {
        body.cfg
        var currentNode: Node? = node.currentNodeWithBasicBlock
        var basicBlock = currentNode?._basicBlock
        //println("RESOLVE: '$id' in ${node._basicBlock} : $body")
        while (basicBlock != null) {
            //println(" -> $basicBlock")
            val vv = basicBlock.locateVar(id)
            vv?.decl?.let { yield(it) }
            //println(" -> vv=$vv")
            currentNode = currentNode?.parentNodeWithBasicBlock
            basicBlock = currentNode?._basicBlock
            //println(" -> $currentNode")
        }
        //TODO()
    }
    yieldAll(currentDecl?.resolve(id) ?: emptySequence())
}

fun Stm.getDecls(): List<IDecl> {
    return when (this) {
        is Stms -> this.stms.flatMap { it.getDecls() }
        is DeclStm -> listOf(this.decl)
        else -> emptyList()
    }
}

fun IDecl.resolve(id: String): Sequence<IDecl> = sequence<IDecl> {
    val decl = this@resolve
    //println("type=$type -> decl=$decl")
    when (decl) {
        is LambdaFunctionExpr -> {
            if (id == "it") {
                yield(decl.implicitIdDecl)
            }
            //TODO("id=$id")
            parentDecl?.resolve(id)?.let { yieldAll(it) }
        }
        is FunDecl -> {
            decl.allParamsById[id]?.let { yield(it) }
            parentDecl?.resolve(id)?.let { yieldAll(it) }
        }
        is ClassDecl -> {
            // Check subtypes
            val potential = decl.bodyAllByName[id]
            if (potential != null) {
                yieldAll(potential)
            }

            // Check parameters without this if we are in the right place (init block, or expressions in body)
            decl.primaryConstructor?.paramsById?.get(id)?.let { yield(it) } // Add this

            //println("ClassDecl: $potential")
            parentDecl?.resolve(id)?.let { yieldAll(it) }
        }

        is FileNode -> {
            // check in the same file
            decl.topLevelDeclsByName[id]?.let {
                yieldAll(it)
            }

            // check simple in imports
            decl.importsById[id]?.let {
                yieldAll(program!!.getDeclsByIdentifier(Identifier(it.identifier.parts + id)))
            }
            // check asterisk imports
            for (import in decl.asteriskImports) {
                val id = Identifier(import.identifier.parts + id)
                yieldAll(program!!.getDeclsByIdentifier(id))
            }
            parentDecl?.resolve(id)?.let {
                yieldAll(it)
            }
        }

        is Package -> {
            decl.symbols[id]?.let {
                yieldAll(it)
            }
        }
        else -> parentDecl?.resolve(id)?.let {
            yieldAll(it)
        }
    }
}
