package valentia.ast

open class NodeTransformer {
    open fun transform(program: Program): Program {
        val modules = program.modulesById.values.map { transform(it) }
        return if (program.modulesById.values.toList() == modules) program else TODO()
    }

    open fun transform(module: Module): Module {
        val packages = module.packagesById.values.map { transform(it) }
        return if (module.packagesById.values.toList() == packages) module else TODO()
    }

    open fun transform(pack: Package): Package {
        val files = pack.files.map { transform(it) }
        return if (files == pack.files) pack else TODO()
    }

    open fun transform(file: FileNode): FileNode {
        return file.copy(
            imports = file.imports.map { transform(it) },
            topLevelDecls = file.topLevelDecls.mapNotNull { transform(it) },
        )
    }

    open fun transformNode(node: Node): Node {
        return when (node) {
            is Stm -> transform(node)
            is Decl -> transform(node)
            is Expr -> transform(node)
            else -> TODO()
        }
    }

    open fun <T : Node> transform(list: List<T>): List<T> {
        val nodes = list.map {
            val node = transformNode(it)
            node.also { if (node !== it) node.copyFrom(it) }
        }
        return if (nodes == list) nodes as List<T> else list
    }

    open fun transform(import: ImportNode): ImportNode {
        return import
    }

    open fun transformNull(decl: Decl?): Decl? {
        if (decl == null) return null
        return transform(decl)
    }

    open fun transform(decl: Decl): Decl {
        return when (decl) {
            is BaseConstructorDecl -> transform(decl)
            is ClassLikeDecl -> transform(decl)
            is CompanionObjectDecl -> transform(decl)
            is FunDecl -> transform(decl)
            is InitDecl -> transform(decl)
            is TypeAliasDecl -> transform(decl)
            is MultiVariableDecl -> transform(decl)
            is VariableDecl -> transform(decl)
            is Module -> TODO()
            is Package -> TODO()
            is Program -> TODO()
            is FileNode -> TODO()
            is TypeDecl -> TODO()
        }
    }

    open fun transform(decl: BaseConstructorDecl): Decl {
        return decl
    }
    open fun transform(decl: ClassLikeDecl): Decl {
        for (decl in (decl.body ?: emptyList())) transform(decl)
        return decl
    }
    open fun transform(decl: CompanionObjectDecl): Decl {
        return decl
    }
    open fun transform(decl: FunDecl): Decl {
        transformNull(decl.body)
        return decl
    }
    open fun transform(decl: InitDecl): Decl {
        return decl
    }
    open fun transform(decl: TypeAliasDecl): Decl {
        return decl
    }
    open fun transform(decl: MultiVariableDecl): Decl {
        for (decl in decl.decls) transform(decl)
        return decl
    }
    open fun transform(decl: VariableDecl): Decl {
        transformNull(decl.expr)
        transformNull(decl.getter)
        transformNull(decl.setter)
        return decl
    }
    open fun transformExprOrStm(exprOrStm: ExprOrStm?): ExprOrStm? {
        return when (exprOrStm) {
            null -> null
            is Stm -> transform(exprOrStm)
            is Expr -> transform(exprOrStm)
        }
    }

    open fun transformNull(stm: Stm?): Stm? {
        if (stm == null) return null
        return transform(stm)
    }

    open fun transform(stm: Stm): Stm {
        return when (stm) {
            is AssignStm -> transform(stm)
            is BreakStm -> transform(stm)
            is ContinueStm -> transform(stm)
            is DeclStm -> transform(stm)
            is EmptyStm -> transform(stm)
            is ExprStm -> transform(stm)
            is IfStm -> transform(stm)
            is DoWhileLoopStm -> transform(stm)
            is ForLoopStm -> transform(stm)
            is WhileLoopStm -> transform(stm)
            is ReturnStm -> transform(stm)
            is Stms -> transform(stm)
            is TryCatchStm -> transform(stm)
            is ThrowStm -> transform(stm)
        }
    }

    open fun transform(stm: AssignStm): Stm {
        return AssignStm(transform(stm.lvalue)!!, stm.op, transform(stm.expr)!!).copyFrom(stm)
    }
    open fun transform(stm: BreakStm): BreakStm {
        return stm
    }
    open fun transform(stm: ContinueStm): ContinueStm {
        return stm
    }
    open fun transform(stm: DeclStm): Stm {
        return DeclStm(transform(stm.decl)!!).copyFrom(stm)
    }
    open fun transform(stm: EmptyStm): Stm {
        return stm
    }

    open fun transform(stm: ExprStm): Stm {
        val expr = transform(stm.expr)
        return if (expr === stm.expr) stm else ExprStm(transform(stm.expr) ?: EmptyExpr()).copyFrom(stm)
        //return ExprStm(transform(stm.expr) ?: EmptyExpr())
    }
    open fun transform(stm: IfStm): Stm {
        val cond = transform(stm.cond)
        val btrue = transform(stm.btrue)
        val bfalse = transformNull(stm.bfalse)
        return if (cond === stm.cond && btrue === stm.btrue && bfalse === stm.bfalse) stm else IfStm(cond!!, btrue!!, bfalse).copyFrom(stm)
    }
    open fun transform(stm: DoWhileLoopStm): Stm {
        val cond = transform(stm.cond)
        val body = transformNull(stm.body)
        return if (cond === stm.cond && body === stm.body) stm else DoWhileLoopStm(body, cond!!, stm.modifiers).copyFrom(stm)
    }
    open fun transform(stm: ForLoopStm): Stm {
        transformNull(stm.vardecl)
        transform(stm.expr)
        transformNull(stm.body)
        return stm
    }
    open fun transform(stm: WhileLoopStm): Stm {
        transform(stm.cond)
        transform(stm.body)
        return stm
    }
    open fun transform(stm: ReturnStm): Stm {
        transformNull(stm.expr)
        return stm
    }
    open fun transform(stm: Stms): Stm {
        for (stm in stm.stms) transform(stm)
        return stm
    }
    open fun transform(stm: TryCatchStm): Stm {
        transform(stm.body)
        for (catch in stm.catches) {
            transform(catch.body)
        }
        transform(stm.finally)
        return stm
    }
    open fun transform(stm: ThrowStm): Stm {
        transform(stm.expr)
        return stm
    }

    open fun transformNull(expr: Expr?): Expr? {
        return when (expr) {
            null -> null
            else -> transform(expr)
        }
    }

    open fun transform(expr: Expr): Expr {
        return when (expr) {
            is AnonymousFunctionExpr -> transform(expr)
            is AssignableExpr -> transform(expr)
            is BinaryOpExpr -> transform(expr)
            is BreakExpr -> transform(expr)
            is BaseCallExpr -> transform(expr)
            is CallableReferenceExt -> transform(expr)
            is CastExpr -> transform(expr)
            is CollectionLiteralExpr -> transform(expr)
            is ContinueExpr -> transform(expr)
            is EmptyExpr -> transform(expr)
            is Identifier -> transform(expr)
            is IfExpr -> transform(expr)
            is IncompleteExpr -> transform(expr)
            is InterpolatedStringExpr -> transform(expr)
            is LambdaFunctionExpr -> transform(expr)
            is LiteralExpr -> transform(expr)
            is ObjectLiteralExpr -> transform(expr)
            is OpSeparatedBinaryExprs -> transform(expr)
            is RangeTestExpr -> transform(expr)
            is ReturnExpr -> transform(expr)
            is Temp -> transform(expr)
            is ThrowExpr -> transform(expr)
            is TryCatchExpr -> transform(expr)
            is TypeTestExpr -> transform(expr)
            is UnaryPreOpExpr -> transform(expr)
            is WhenExpr -> transform(expr)
            is TernaryExpr -> transform(expr)
        }
    }
    open fun transform(expr: AnonymousFunctionExpr): Expr {
        return expr
    }

    open fun transform(expr: BinaryOpExpr): Expr {
        transform(expr.left)
        transform(expr.right)
        return expr
    }
    open fun transform(expr: BreakExpr): Expr {
        return expr
    }
    open fun transform(expr: BaseCallExpr): Expr {
        when (expr) {
            is CallExpr -> transform(expr)
            //is CallIdExpr -> transform(expr)
        }
        transform(expr.paramsPlusLambda)
        return expr
    }
    open fun transform(expr: CallExpr): Expr {
        transform(expr.expr)
        return expr
    }
    //open fun transform(expr: CallIdExpr) {
    //}
    open fun transform(expr: CallableReferenceExt): Expr {
        return expr
    }
    open fun transform(expr: CastExpr): Expr {
        transform(expr.expr)
        return expr
    }
    open fun transform(expr: CollectionLiteralExpr): Expr {
        transform(expr.items)
        return expr
    }
    open fun transform(expr: ContinueExpr): Expr {
        return expr
    }
    open fun transform(expr: EmptyExpr): Expr {
        return expr
    }
    open fun transform(expr: Identifier): Expr {
        return expr
    }
    open fun transform(expr: IfExpr): Expr {
        transform(expr.cond)
        transformExprOrStm(expr.trueBody)
        transformExprOrStm(expr.falseBody)
        return expr
    }
    open fun transform(expr: TernaryExpr): Expr {
        transform(expr.cond)
        transformExprOrStm(expr.trueExpr)
        transformExprOrStm(expr.falseExpr)
        return expr
    }
    open fun transform(expr: IncompleteExpr): Expr {
        return expr
    }
    open fun transform(expr: InterpolatedStringExpr): Expr {
        for (chunk in expr.chunks) {
            when (chunk) {
                is InterpolatedStringExpr.ExpressionChunk -> transform(chunk.expr)
                is InterpolatedStringExpr.StringChunk -> Unit
            }
        }
        return expr
    }
    open fun transform(expr: LambdaFunctionExpr): Expr {
        return expr
    }
    open fun transform(expr: LiteralExpr): Expr {
        return expr
    }
    open fun transform(expr: ObjectLiteralExpr): Expr {
        return expr
    }
    open fun transform(expr: OpSeparatedBinaryExprs): Expr {
        transform(expr.exprs)
        return expr
    }
    open fun transform(expr: RangeTestExpr): Expr {
        transform(expr.base)
        transform(expr.container)
        return expr
    }
    open fun transform(expr: ReturnExpr): Expr {
        transformNull(expr.expr)
        return expr
    }
    open fun transform(expr: Temp): Expr {
        return expr
    }
    open fun transform(expr: ThrowExpr): Expr {
        transform(expr.expr)
        return expr
    }
    open fun transform(expr: TryCatchExpr): Expr {
        transformNode(expr.body)
        for (catch in expr.catches) {
            transform(catch.body)
        }
        transformNull(expr.finally)
        return expr
    }
    open fun transform(expr: TypeTestExpr): Expr {
        transform(expr.base)
        return expr
    }
    open fun transform(expr: UnaryPreOpExpr): Expr {
        transform(expr.expr)
        return expr
    }
    open fun transform(expr: WhenExpr): Expr {
        transformNull(expr.subject?.decl)
        transformNull(expr.subject?.expr)
        for (entry in expr.entries) {
            //transform(entry.conditions) // @TODO
            transform(entry.body)
        }
        return expr
    }

    //////////////

    open fun transform(expr: AssignableExpr): AssignableExpr {
        return when (expr) {
            is IdentifierExpr -> transform(expr)
            is IndexedExpr -> transform(expr)
            is NavigationExpr -> transform(expr)
            is SuperExpr -> transform(expr)
            is TempExpr -> transform(expr)
            is ThisExpr -> transform(expr)
            is TypeArgumentsAssignableSuffixExpr -> transform(expr)
            is UnaryPostOpExpr -> transform(expr)
        }
    }

    open fun transform(expr: IdentifierExpr): AssignableExpr {
        return expr
    }
    open fun transform(expr: IndexedExpr): AssignableExpr {
        val cexpr = transform(expr.expr)
        val indices = transform(expr.indices)
        return if (cexpr === expr.expr && indices === expr.indices) expr else IndexedExpr(cexpr, indices).copyFrom(expr)
    }
    open fun transform(expr: NavigationExpr): AssignableExpr {
        val cexpr = transform(expr.expr)
        return if (cexpr === expr.expr) expr else NavigationExpr(expr.op, cexpr, expr.key)
    }
    open fun transform(expr: SuperExpr): AssignableExpr {
        return expr
    }
    open fun transform(expr: TempExpr): AssignableExpr {
        return expr
    }
    open fun transform(expr: ThisExpr): AssignableExpr {
        return expr
    }
    open fun transform(expr: TypeArgumentsAssignableSuffixExpr): AssignableExpr {
        return expr
    }
    open fun transform(expr: UnaryPostOpExpr): AssignableExpr {
        val cexpr = transform(expr.expr)
        return if (cexpr === expr.expr) expr else UnaryPostOpExpr(cexpr, expr.op)
    }

    protected fun <T : Node> T.copyFrom(other: Node): T {
        this.parentNode = other.parentNode
        return this
    }
}