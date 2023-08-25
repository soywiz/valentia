package valentia.sema

import valentia.ast.*
import valentia.ast.cfg.BasicBlock
import valentia.ast.cfg.BasicBlockBuilder
import valentia.ast.cfg.CFG

class SemaResolver : NodeTransformer() {
    companion object {
        fun resolve(program: Program): Program {
            return SemaResolver().transform(program)
        }
    }

    override fun transform(program: Program): Program {
        return super.transform(program).also {
            it.semaResolved = true
        }
    }

    override fun transform(body: FunctionBody): FunctionBody {
        //println("transform.FunctionBody: ${body.parentNode}")
        body.cfg
        return super.transform(body)
    }

    // @TODO: Is this required?
    var currentClassDecl: ClassLikeDecl? = null

    override fun transform(decl: ClassLikeDecl): Decl {
        val old = currentClassDecl
        try {
            decl.fqname = Identifier(((decl.parentDecl as? FileNode?)?._package?.parts ?: emptyList()) + decl.name).fqname
            currentClassDecl = decl
            return super.transform(decl)
        } finally {
            currentClassDecl = old
        }
    }

    override fun transform(expr: LambdaFunctionExpr): Expr {
        return super.transform(expr)
    }

    override fun transform(expr: IdentifierExpr): AssignableExpr {
        // Might be already resolved by the CallExpr
        if (expr.resolvedDecl == null) {
            //val resolved = currentResolutionContext.resolve(expr.id)
            val decls = expr.resolve(expr.id)
            val firstDecl = decls.firstOrNull()
            expr.resolvedDecl = firstDecl
            if (firstDecl != null) {
                val cfgExpr = expr.currentBasicBlock?.cfg
                val cfgDecl = firstDecl.currentBasicBlock?.cfg
                if (cfgExpr !== cfgDecl && cfgDecl != null) {
                    //println("RESOLVE.id : ${expr.id} : $firstDecl : $cfgExpr, $cfgDecl")
                    if (firstDecl is VariableDecl) {
                        expr.currentFunctionBody?.varCaptures?.add(firstDecl)
                    }
                }
            }
            if (firstDecl?.parentNode == currentClassDecl && currentClassDecl != null) {
                expr.addThis = true
            }
        }
        //println("expr.addThis=${expr.addThis}, expr=$expr, firstDecl=$firstDecl, currentClassDecl=$currentClassDecl, firstDecl.parentNode=${firstDecl?.parentNode}")
        return super.transform(expr)
    }

    //override fun visit(expr: BinaryOpExpr) {
    //    if (expr.op == "+") {
    //        val leftTypeDecl = expr.resolveType(leftType)
    //        for (resolve in leftTypeDecl.resolve("plus") + this.resolve("plus")) {
    //            println("resolve")
    //        }
    //    }
    //}

    override fun transform(expr: CallExpr): Expr {
        val cexpr = expr.expr
        val funcType = expr.getFuncType()
        val res = super.transform(expr)
        when (cexpr) {
            is IdentifierExpr -> {
                val decls = expr.resolve(cexpr.id).distinct()
                val realDecls = decls
                    .flatMap { if (it is ClassLikeDecl) it.constructors else listOf(it) }
                    .filterIsInstance<CallableDecl>()
                val decls2 = DeclCollection(realDecls.toList())
                val resolved = decls2.findMatch(funcType)
                cexpr.resolvedDecl = resolved ?: decls.filterIsInstance<ClassLikeDecl>().firstOrNull()
                if (resolved?.parentNode == currentClassDecl && currentClassDecl != null) {
                    cexpr.addThis = true
                }
            }
            is NavigationExpr -> {
                val key = cexpr.key
                if (key is String) {
                    val exprType = cexpr.expr.getNodeType()
                    val exprResolvedDecl = cexpr.resolve(exprType.toString()).firstOrNull()
                    val decls = DeclCollection(exprResolvedDecl?.resolve(key)?.toList())
                    //val decls = currentResolutionContext.resolve(cexpr.key.toString())
                    val resolved = decls.findMatch(funcType)
                    if (exprResolvedDecl is ClassLikeDecl) {
                        val resolvedSubTypes = exprResolvedDecl.directResolvedSubTypes
                        //println("resolvedSubTypes=$resolvedSubTypes")
                    }
                    cexpr.resolvedDecl = resolved
                    expr.resolvedDecl = resolved
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
        return res
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

    override fun transform(expr: CallableReferenceExt): Expr {
        val res = super.transform(expr)
        if (expr.type is SimpleType) {
            // Check if not a type callable reference but an expression callable reference. We couldn't know while parsing.
            // @TODO: Transform into [NavigationExpr]
            expr._variableDecl = expr.resolve(expr.type.name).filterIsInstance<VariableDeclBase>().firstOrNull()
            if (expr._variableDecl != null) {
                return NavigationExpr("::", IdentifierExpr(expr.type.toString()), "class")
            }
        }
        return res
    }
}