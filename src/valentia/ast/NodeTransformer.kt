package valentia.ast

open class NodeTransformer {
    open fun transform(program: Program): Program {
        var mod = false
        val modules = program.modulesById.values.map {
            val node = transform(it)
            if (node !== it) mod = true
            if (node !== it) node.copyFrom(it) else node
        }
        return if (!mod) program else Program().addModules(modules)
    }

    open fun transform(module: Module): Module {
        var mod = false
        val packages: List<Package> = module.packagesById.values.map {
            val node = transform(it)
            if (node !== it) mod = true
            if (node !== it) node.copyFrom(it) else node
        }
        return if (!mod) module else Module(module.program, module.id).addPackages(packages)
    }

    open fun transform(pack: Package): Package {
        var mod = false
        val files = pack.files.map {
            val node = transform(it)
            if (node !== it) mod = true
            if (node !== it) node.copyFrom(it) else node
        }
        return if (!mod) pack else Package(pack.module, pack.identifier).addFiles(files)
    }

    open fun transform(file: FileNode): FileNode {
        val imports = file.imports.map { transform(it) }
        val topLevelDecls = file.topLevelDecls.map { transform(it) }
        return when {
            imports == file.imports && topLevelDecls == file.topLevelDecls -> file
            else -> file.copy(imports = imports, topLevelDecls = topLevelDecls).copyFrom(file)
        }
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
        var mod = false
        val nodes = list.map {
            val node = transformNode(it)
            if (node !== it) mod = true
            node.also { if (node !== it) node.copyFrom(it) }
        }
        return if (mod) nodes as List<T> else list
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

    open fun transformNull(body: FunctionBody?): FunctionBody? {
        if (body == null) return null
        return transform(body)
    }

    open fun transform(body: FunctionBody): FunctionBody {
        val stms = transform(body.stms)
        return if (stms !== body.stms) FunctionBody(stms).copyFrom(body) else body
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
        val body = transformNull(decl.body)
        return if (body !== decl.body) decl else decl.copy(body = body).copyFrom(decl)
    }
    open fun transform(decl: InitDecl): Decl {
        return decl
    }
    open fun transform(decl: TypeAliasDecl): Decl {
        return decl
    }
    open fun transform(decl: MultiVariableDecl): Decl {
        var mod = false
        val decls = decl.decls.map {
            val node = transform(it)
            if (node !== it) mod = true
            node as VariableDecl
        }
        for (decl in decl.decls) transform(decl)
        return if (!mod) decl else decl.copy(decls = decls).copyFrom(decl)
    }
    open fun transform(decl: VariableDecl): Decl {
        val cexpr = transformNull(decl.expr)
        val getter = transformNull(decl.getter) as? FunDecl?
        val setter = transformNull(decl.setter) as? FunDecl?
        return if (cexpr === decl.expr && getter === decl.getter && setter === decl.setter) decl else decl.copy(expr = cexpr, getter = getter, setter = setter).copyFrom(decl)
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
        return if (expr === stm.expr) stm else ExprStm(transform(stm.expr)).copyFrom(stm)
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
        val vardecl = transformNull(stm.vardecl) as? VariableDeclBase?
        val cexpr = transform(stm.expr)
        val body = transformNull(stm.body)
        return if (vardecl === stm.vardecl && cexpr === stm.expr && body === stm.body) stm else ForLoopStm(cexpr, vardecl, body, stm.annotations, stm.modifiers).copyFrom(stm)
    }
    open fun transform(stm: WhileLoopStm): Stm {
        val cond = transform(stm.cond)
        val body = transform(stm.body)
        return if (cond === stm.cond && body === stm.body) stm else WhileLoopStm(cond, body, stm.modifiers).copyFrom(stm)
    }
    open fun transform(stm: ReturnStm): Stm {
        val cexpr = transformNull(stm.expr)
        return if (cexpr === stm.expr) stm else ReturnStm(cexpr).copyFrom(stm)
    }
    open fun transform(stm: Stms): Stm {
        var mod = false
        val stms = stm.stms.map {
            val node = transform(it)
            if (node !== it) mod = true
            node.copyFrom(it)
        }
        return if (!mod) stm else Stms(stms)
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
        val left = transform(expr.left)
        val right = transform(expr.right)
        return if (left === expr.left && right === expr.right) expr else BinaryOpExpr(left, expr.op, right).copyFrom(expr)
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
        val cexpr = transform(expr.expr)
        val params = transform(expr.params)
        val lambdaArg = transformNull(expr.lambdaArg)
        return if (cexpr === expr.expr && params == expr.params && lambdaArg === expr.lambdaArg) expr else CallExpr(cexpr, params, lambdaArg, expr.typeArgs)
    }
    //open fun transform(expr: CallIdExpr) {
    //}
    open fun transform(expr: CallableReferenceExt): Expr {
        return expr
    }
    open fun transform(expr: CastExpr): Expr {
        val cexpr = transform(expr.expr)
        return if (cexpr === expr.expr) expr else CastExpr(cexpr, expr.targetType, expr.safe).copyFrom(expr)
    }
    open fun transform(expr: CollectionLiteralExpr): Expr {
        val items = transform(expr.items)
        return if (items === expr.items) expr else expr.copy(items).copyFrom(expr)
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
        val cond = transform(expr.cond)
        val trueBody = transformExprOrStm(expr.trueBody)
        val falseBody = transformExprOrStm(expr.falseBody)
        return if (cond === expr.cond && trueBody === expr.trueBody && falseBody === expr.falseBody) expr else IfExpr(cond, trueBody!!, falseBody).copyFrom(expr)
    }
    open fun transform(expr: TernaryExpr): Expr {
        val cond = transform(expr.cond)
        val trueBody = transform(expr.trueExpr)
        val falseBody = transform(expr.falseExpr)
        return if (cond === expr.cond && trueBody === expr.trueExpr && falseBody === expr.falseExpr) expr else TernaryExpr(cond, trueBody, falseBody).copyFrom(expr)
    }
    open fun transform(expr: IncompleteExpr): Expr {
        return expr
    }
    open fun transform(expr: InterpolatedStringExpr): Expr {
        var mod = false
        val chunks = expr.chunks.map {
            val node = transform(it)
            if (node !== it) mod = true
            node
        }
        return if (!mod) expr else InterpolatedStringExpr(chunks).copyFrom(expr)
    }

    open fun transform(chunk: InterpolatedStringExpr.Chunk): InterpolatedStringExpr.Chunk {
        return chunk
    }

    open fun transform(expr: LambdaFunctionExpr): Expr {
        val body = transform(expr.body)
        return if (body === expr.body) expr else LambdaFunctionExpr(body, expr.params).copyFrom(expr)
    }
    open fun transform(expr: LiteralExpr): Expr {
        return expr
    }
    open fun transform(expr: ObjectLiteralExpr): Expr {
        return expr
    }
    open fun transform(expr: OpSeparatedBinaryExprs): Expr {
        val exprs = transform(expr.exprs)
        return if (exprs === expr.exprs) expr else OpSeparatedBinaryExprs(expr.ops, exprs)
    }
    open fun transform(expr: RangeTestExpr): Expr {
        val base = transform(expr.base)
        val container = transform(expr.container)
        return if (base === expr.base && container === expr.container) expr else RangeTestExpr(base, expr.kind, container).copyFrom(expr)
    }
    open fun transform(expr: ReturnExpr): Expr {
        val cexpr = transformNull(expr.expr)
        return if (cexpr === expr.expr) expr else ReturnExpr(cexpr).copyFrom(expr)
    }
    open fun transform(expr: Temp): Expr {
        return expr
    }
    open fun transform(expr: ThrowExpr): Expr {
        val cexpr = transform(expr.expr)
        return if (cexpr === expr.expr) expr else ThrowExpr(cexpr).copyFrom(expr)
    }
    open fun transform(expr: TryCatchExpr): Expr {
        val body = transformNode(expr.body)
        val catches = expr.catches.map { transform(it) }
        val finally = transformNull(expr.finally)
        return if (body === expr.body && finally === expr.finally && catches == expr.catches) expr else TryCatchExpr(body, catches, finally).copyFrom(expr)
    }
    open fun transform(catch: TryCatchExpr.Catch): TryCatchExpr.Catch {
        val body = transform(catch.body)
        return if (body === catch.body) catch else TryCatchExpr.Catch(catch.local, catch.type, body).copyFrom(catch)
    }
    open fun transform(expr: TypeTestExpr): Expr {
        val base = transform(expr.base)
        return if (base === expr.base) expr else TypeTestExpr(base, expr.kind, expr.type)
    }
    open fun transform(expr: UnaryPreOpExpr): Expr {
        val cexpr = transform(expr.expr)
        return if (cexpr === expr) expr else UnaryPreOpExpr(expr.op, cexpr).copyFrom(expr)
    }
    open fun transform(expr: WhenExpr): Expr {
        val subject = transformNull(expr.subject)
        val entries = expr.entries.map { transform(it) }
        return if (subject === expr.subject && entries == expr.entries) expr else WhenExpr(expr.subject)
    }

    open fun transformNull(entry: WhenExpr.Subject?): WhenExpr.Subject? {
        println("TODO: NodeTransformer.WhenExpr.Subject")
        return entry
    }

    open fun transform(entry: WhenExpr.Entry): WhenExpr.Entry {
        println("TODO: NodeTransformer.WhenExpr.Entry")
        return entry
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
}