package valentia.ast

open class NodeVisitor {
    open fun visit(file: FileNode) {
        for (import in file.imports) visit(import)
        for (decl in file.topLevelDecls) visit(decl)
    }

    open fun visitNode(node: Node) {
        when (node) {
            is Stm -> visit(node)
            is Decl -> visit(node)
            is Expr -> visit(node)
            else -> TODO()
        }
    }

    open fun visit(list: List<Node>) {
        for (n in list) visitNode(n)
    }

    open fun visit(import: ImportNode) {
    }

    open fun visit(decl: Decl?) {
        when (decl) {
            null -> Unit
            is BaseConstructorDecl -> visit(decl)
            is ClassOrObjectDecl -> visit(decl)
            is CompanionObjectDecl -> visit(decl)
            is FunDecl -> visit(decl)
            is InitDecl -> visit(decl)
            is TypeAliasDecl -> visit(decl)
            is MultiVariableDecl -> visit(decl)
            is VariableDecl -> visit(decl)
        }
    }

    open fun visit(decl: BaseConstructorDecl) {
    }
    open fun visit(decl: ClassOrObjectDecl) {
        for (decl in (decl.body ?: emptyList())) visit(decl)
    }
    open fun visit(decl: CompanionObjectDecl) {
    }
    open fun visit(decl: FunDecl) {
        visit(decl.body)
    }
    open fun visit(decl: InitDecl) {
    }
    open fun visit(decl: TypeAliasDecl) {
    }
    open fun visit(decl: MultiVariableDecl) {
        for (decl in decl.decls) visit(decl)
    }
    open fun visit(decl: VariableDecl) {
    }
    open fun visitExprOrStm(exprOrStm: ExprOrStm?) {
        when (exprOrStm) {
            null -> Unit
            is Stm -> visit(exprOrStm)
            is Expr -> visit(exprOrStm)
        }
    }


    open fun visit(stm: Stm?) {
        when (stm) {
            null -> Unit
            is AssignStm -> visit(stm)
            is BreakStm -> visit(stm)
            is ContinueStm -> visit(stm)
            is DeclStm -> visit(stm)
            is EmptyStm -> visit(stm)
            is ExprStm -> visit(stm)
            is IfStm -> visit(stm)
            is DoWhileLoopStm -> visit(stm)
            is ForLoopStm -> visit(stm)
            is WhileLoopStm -> visit(stm)
            is ReturnStm -> visit(stm)
            is Stms -> visit(stm)
            is TryCatchStm -> visit(stm)
        }
    }

    open fun visit(stm: AssignStm) {
        visit(stm.lvalue)
        visit(stm.expr)
    }
    open fun visit(stm: BreakStm) { }
    open fun visit(stm: ContinueStm) { }
    open fun visit(stm: DeclStm) {
        visit(stm.decl)
    }
    open fun visit(stm: EmptyStm) { }
    open fun visit(stm: ExprStm) {
        visit(stm.expr)
    }
    open fun visit(stm: IfStm) {
        visit(stm.cond)
        visit(stm.btrue)
        visit(stm.bfalse)
    }
    open fun visit(stm: DoWhileLoopStm) {
        visit(stm.cond)
        visit(stm.body)
    }
    open fun visit(stm: ForLoopStm) {
        visit(stm.vardecl)
        visit(stm.expr)
        visit(stm.body)
    }
    open fun visit(stm: WhileLoopStm) {
        visit(stm.cond)
        visit(stm.body)
    }
    open fun visit(stm: ReturnStm) {
        visit(stm.expr)
    }
    open fun visit(stm: Stms) {
        for (stm in stm.stms) visit(stm)
    }
    open fun visit(stm: TryCatchStm) {
        visit(stm.body)
        for (catch in stm.catches) {
            visit(catch.body)
        }
        visit(stm.finally)
    }

    open fun visit(expr: Expr?) {
        when (expr) {
            null -> Unit
            is AnonymousFunctionExpr -> visit(expr)
            is AssignableExpr -> visit(expr)
            is BinaryOpExpr -> visit(expr)
            is BreakExpr -> visit(expr)
            is CallExpr -> visit(expr)
            is CallableReferenceExt -> visit(expr)
            is CastExpr -> visit(expr)
            is CollectionLiteralExpr -> visit(expr)
            is ContinueExpr -> visit(expr)
            is EmptyExpr -> visit(expr)
            is Identifier -> visit(expr)
            is IfExpr -> visit(expr)
            is IncompleteExpr -> visit(expr)
            is InterpolatedStringExpr -> visit(expr)
            is LambdaFunctionExpr -> visit(expr)
            is LiteralExpr -> visit(expr)
            is ObjectLiteralExpr -> visit(expr)
            is OpSeparatedBinaryExprs -> visit(expr)
            is RangeTestExpr -> visit(expr)
            is ReturnExpr -> visit(expr)
            is Temp -> visit(expr)
            is ThrowExpr -> visit(expr)
            is TryCatchExpr -> visit(expr)
            is TypeTestExpr -> visit(expr)
            is UnaryPreOpExpr -> visit(expr)
            is WhenExpr -> visit(expr)
        }
    }
    open fun visit(expr: AnonymousFunctionExpr) { }

    open fun visit(expr: BinaryOpExpr) {
        visit(expr.left)
        visit(expr.right)
    }
    open fun visit(expr: BreakExpr) { }
    open fun visit(expr: CallExpr) {
        visit(expr.expr)
        visit(expr.paramsPlusLambda)
    }
    open fun visit(expr: CallableReferenceExt) { }
    open fun visit(expr: CastExpr) {
        visit(expr.expr)
    }
    open fun visit(expr: CollectionLiteralExpr) {
        visit(expr.items)
    }
    open fun visit(expr: ContinueExpr) { }
    open fun visit(expr: EmptyExpr) { }
    open fun visit(expr: Identifier) { }
    open fun visit(expr: IfExpr) {
        visit(expr.cond)
        visitExprOrStm(expr.trueBody)
        visitExprOrStm(expr.falseBody)
    }
    open fun visit(expr: IncompleteExpr) { }
    open fun visit(expr: InterpolatedStringExpr) {
        for (chunk in expr.chunks) {
            when (chunk) {
                is InterpolatedStringExpr.ExpressionChunk -> visit(chunk.expr)
                is InterpolatedStringExpr.StringChunk -> Unit
            }
        }
    }
    open fun visit(expr: LambdaFunctionExpr) { }
    open fun visit(expr: LiteralExpr) { }
    open fun visit(expr: ObjectLiteralExpr) { }
    open fun visit(expr: OpSeparatedBinaryExprs) { }
    open fun visit(expr: RangeTestExpr) {
        visit(expr.base)
        visit(expr.container)
    }
    open fun visit(expr: ReturnExpr) {
        visit(expr.expr)
    }
    open fun visit(expr: Temp) { }
    open fun visit(expr: ThrowExpr) {
        visit(expr.expr)
    }
    open fun visit(expr: TryCatchExpr) {
        visitNode(expr.body)
        for (catch in expr.catches) {
            visit(catch.body)
        }
        visit(expr.finally)
    }
    open fun visit(expr: TypeTestExpr) {
        visit(expr.base)
    }
    open fun visit(expr: UnaryPreOpExpr) {
        visit(expr.expr)
    }
    open fun visit(expr: WhenExpr) {
        visit(expr.subject?.decl)
        visit(expr.subject?.expr)
        for (entry in expr.entries) {
            //visit(entry.conditions) // @TODO
            visit(entry.body)
        }
    }

    //////////////

    open fun visit(expr: AssignableExpr) {
        when (expr) {
            is IdentifierExpr -> visit(expr)
            is IndexedExpr -> visit(expr)
            is NavigationExpr -> visit(expr)
            is SuperExpr -> visit(expr)
            is TempExpr -> visit(expr)
            is ThisExpr -> visit(expr)
            is TypeArgumentsAssignableSuffixExpr -> visit(expr)
            is UnaryPostOpExpr -> visit(expr)
        }
    }

    open fun visit(expr: IdentifierExpr) { }
    open fun visit(expr: IndexedExpr) {
        visit(expr.expr)
        visit(expr.indices)
    }
    open fun visit(expr: NavigationExpr) {
        visit(expr.expr)
    }
    open fun visit(expr: SuperExpr) { }
    open fun visit(expr: TempExpr) { }
    open fun visit(expr: ThisExpr) { }
    open fun visit(expr: TypeArgumentsAssignableSuffixExpr) { }
    open fun visit(expr: UnaryPostOpExpr) {
        visit(expr.expr)
    }
}