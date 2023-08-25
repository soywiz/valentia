package valentia.sema

import valentia.ast.*

fun Node.resolve(id: String): Sequence<Decl> = sequence {
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

fun Stm.getDecls(): List<Decl> {
    return when (this) {
        is Stms -> this.stms.flatMap { it.getDecls() }
        is DeclStm -> listOf(this.decl)
        else -> emptyList()
    }
}

fun Decl.resolve(id: String): Sequence<Decl> = sequence<Decl> {
    val decl = this@resolve
    //println("type=$type -> decl=$decl")
    when (decl) {
        //is FunDecl -> {
        //    decl.body?.getDecls()?.let { yieldAll(it) }
        //    parentDecl?.resolve(id)?.let { yieldAll(it) }
        //}
        is ClassDecl -> {
            // Check subtypes
            val potential = decl.bodyAllByName[id]
            if (potential != null) {
                yieldAll(potential)
            }
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
