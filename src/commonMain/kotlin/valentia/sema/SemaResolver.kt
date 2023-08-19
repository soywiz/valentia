package valentia.sema

import valentia.ast.*

class SemaResolver : NodeVisitor() {
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

    var currentClassDecl: ClassOrObjectDecl? = null

    override fun visit(decl: ClassOrObjectDecl) {
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
        //val resolved = currentResolutionContext.resolve(expr.id)
        expr.resolutionContext = currentResolutionContext
        val decls = currentResolutionContext.resolve(expr.id)
        val firstDecl = decls.decls.firstOrNull()
        if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
            expr.addThis = true
        }
        //println("expr.addThis=${expr.addThis}, expr=$expr, firstDecl=$firstDecl, currentClassDecl=$currentClassDecl, firstDecl.parentNode=${firstDecl?.parentNode}")
    }

    override fun visit(expr: CallIdExpr) {
        super.visit(expr)
        val decls = currentResolutionContext.resolve(expr.id)
        val funcType = expr.getFuncType(currentResolutionContext)
        expr.resolvedDecl = decls.findMatch(funcType)
        val firstDecl = decls.decls.firstOrNull()
        if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
            expr.addThis = true
        }
    }

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