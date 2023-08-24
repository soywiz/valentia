package valentia.sema

import valentia.ast.*

class SemaResolver : NodeVisitor() {
    companion object {
        fun resolve(program: Program): Program {
            SemaResolver().visit(program)
            return program
        }
    }

    override fun visit(pack: Package) {
        pushResolutionContext(PackageResolutionContext(pack)) {
            super.visit(pack)
        }
    }

    override fun visit(file: FileNode) {
        pushResolutionContext(FileResolutionContext(file)) {
            super.visit(file)
        }
    }

    var currentClassDecl: ClassLikeDecl? = null

    override fun visit(decl: ClassLikeDecl) {
        val old = currentClassDecl
        try {
            currentClassDecl = decl
            pushResolutionContext(ClassResolutionContext(decl)) {
                super.visit(decl)
            }
        } finally {
            currentClassDecl = old
        }
    }

    override fun visit(decl: FunDecl) {
        pushResolutionContext(FunResolutionContext(decl)) {
            super.visit(decl)
        }
    }

    override fun visit(expr: IdentifierExpr) {
        super.visit(expr)
        // Might be already resolved by the CallExpr
        if (expr.resolvedDecl == null) {
            //val resolved = currentResolutionContext.resolve(expr.id)
            expr.resolutionContext = currentResolutionContext
            val decls = currentResolutionContext.resolve(expr.id)
            val firstDecl = decls.decls.firstOrNull()
            expr.resolvedDecl = firstDecl
            if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
                expr.addThis = true
            }
        }
        //println("expr.addThis=${expr.addThis}, expr=$expr, firstDecl=$firstDecl, currentClassDecl=$currentClassDecl, firstDecl.parentNode=${firstDecl?.parentNode}")
    }

    override fun visit(expr: CallExpr) {
        val cexpr = expr.expr
        val funcType = expr.getFuncType(currentResolutionContext)
        super.visit(expr)
        when (cexpr) {
            is IdentifierExpr -> {
                val decls = expr.resolve(cexpr.id).toList().distinct()
                val realDecls: List<CallableDecl> = decls
                    .flatMap { if (it is ClassLikeDecl) it.constructors else listOf(it) }
                    .filterIsInstance<CallableDecl>()
                val decls2 = DeclCollection(realDecls)
                val resolved = decls2.findMatch(funcType)
                cexpr.resolvedDecl = resolved ?: decls.filterIsInstance<ClassLikeDecl>().firstOrNull()
                if (resolved?.parentNode == currentClassDecl && currentClassDecl != null) {
                    cexpr.addThis = true
                }
            }
            is NavigationExpr -> {
                val key = cexpr.key
                if (key is String) {
                    val exprType = cexpr.expr.getType(currentResolutionContext)
                    val exprResolvedDecl = cexpr.resolve(exprType.toString()).firstOrNull()
                    val decls = DeclCollection(exprResolvedDecl?.resolve(key)?.toList())
                    //val decls = currentResolutionContext.resolve(cexpr.key.toString())
                    val resolved = decls.findMatch(funcType)
                    if (exprResolvedDecl is ClassLikeDecl) {
                        val resolvedSubTypes = exprResolvedDecl.directResolvedSubTypes
                        println("resolvedSubTypes=$resolvedSubTypes")
                    }
                    cexpr.resolvedDecl = resolved
                } else {
                    TODO("NavigationExpr key=${cexpr.key}")
                }
            }
            else -> {
                TODO("key=$cexpr")
            }
        }
        /*
        val decls = currentResolutionContext.resolve(expr.id)
        val funcType = expr.getFuncType(currentResolutionContext)
        expr.resolvedDecl = decls.findMatch(funcType)
        val firstDecl = decls.decls.firstOrNull()
        if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
            expr.addThis = true
        }
         */
    }

    //override fun visit(expr: CallIdExpr) {
    //    super.visit(expr)
    //    val decls = currentResolutionContext.resolve(expr.id)
    //    val funcType = expr.getFuncType(currentResolutionContext)
    //    expr.resolvedDecl = decls.findMatch(funcType)
    //    val firstDecl = decls.decls.firstOrNull()
    //    if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
    //        expr.addThis = true
    //    }
    //}

    var currentResolutionContext: ResolutionContext = DummyResolutionContext

    fun <T> pushResolutionContext(resolutionContext: ResolutionContext, block: () -> T): T {
        val oldResolutionContext = currentResolutionContext
        try {
            currentResolutionContext = resolutionContext + currentResolutionContext
            return block()
        } finally {
            currentResolutionContext = oldResolutionContext
        }
    }
}