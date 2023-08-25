package valentia.ast

interface DeclBuilder : NodeBuilder {
    companion object {
        fun buildDeclList(block: DeclBuilder.() -> Unit): List<Decl> {
            val decls = arrayListOf<Decl>()
            val builder = object : DeclBuilder {
                override fun addDecl(decl: Decl) {
                    decls += decl
                }
            }
            block(builder)
            return decls
        }
    }

    fun addDecl(decl: Decl) {
    }

    fun VAL(name: String, expr: Expr? = null, type: Type? = null, delegation: Boolean = false): VariableDecl = VariableDecl(name, type, expr, delegation = delegation, kind = VariableKind.VAL).also { addDecl(it) }
    fun VAR(name: String, expr: Expr? = null, type: Type? = null, delegation: Boolean = false): VariableDecl = VariableDecl(name, type, expr, delegation = delegation, kind = VariableKind.VAR).also { addDecl(it) }
    fun CLASS(
        name: String,
        vararg subTypes: SubTypeInfo,
        data: Boolean = false,
        primaryConstructorDecl: PrimaryConstructorDecl? = null,
        modifiers: Modifiers = Modifiers.EMPTY,
        block: DeclBuilder.() -> Unit = {},
    ): ClassDecl =
        ClassDecl(
            ClassKind.CLASS,
            name,
            if (subTypes.isNotEmpty()) subTypes.toList() else emptyList(),
            buildDeclList { block() },
            primaryConstructor = primaryConstructorDecl,
            modifiers = modifiers
        ).also { addDecl(it) }
    fun INTERFACE(name: String, vararg subTypes: SubTypeInfo, block: DeclBuilder.() -> Unit = {}): ClassDecl =
        ClassDecl(ClassKind.INTERFACE, name, if (subTypes.isEmpty()) null else subTypes.toList(), buildDeclList { block() }, primaryConstructor = null).also { addDecl(it) }
    fun FUN(
        name: String,
        ret: Type? = null,
        vararg params: Pair<String, Type>,
        block: (StmBuilder.() -> Unit)? = null
    ): FunDecl =
        FunDecl(name, params.map { FuncValueParam(it.first, it.second) }, retType = ret, body = block?.let { func -> StmBuilder.buildStms { func() } }).also { addDecl(it) }

    fun FILE(
        shebang: String? = null,
        _package: Identifier? = null,
        fileAnnotations: List<AnnotationNodes> = emptyList(),
        imports: List<ImportNode> = emptyList(),
        filePath: String = "Unknown.kt",
        block: DeclBuilder.() -> Unit
    ): FileNode {
        return FileNode(
            filePath, shebang, _package, fileAnnotations, imports,
            topLevelDecls = buildDeclList { block() },
        )
    }
}

interface StmBuilder : DeclBuilder {
    companion object {
        fun buildStms(block: StmBuilder.() -> Unit): Stms = Stms(buildStmList(block))
        fun buildStm(compact: Boolean, block: StmBuilder.() -> Unit): Stm = if (compact) buildStmCompact(block) else buildStms(block)
        fun buildStmCompact(block: StmBuilder.() -> Unit): Stm = buildStmList(block).compact()
        fun buildStmList(block: StmBuilder.() -> Unit): List<Stm> {
            val stms = arrayListOf<Stm>()
            val builder = object : StmBuilder {
                override fun addStm(stm: Stm) {
                    stms += stm
                }
            }
            block(builder)
            return stms
        }
    }

    fun addStm(stm: Stm) {
    }

    fun <T : Stm> T.addStm(): T {
        addStm(this)
        return this
    }
    fun <T : Expr> T.addStm(): T {
        addStm(ExprStm(this))
        return this
    }

    fun ASSIGN(lvalue: AssignableExpr, expr: Expr, op: String = "="): AssignStm = AssignStm(lvalue, op, expr).addStm()
    fun ASSIGN(lvalue: AssignableExpr, op: String, expr: Expr): AssignStm = AssignStm(lvalue, op, expr).addStm()

    fun STM(expr: Expr): ExprStm = ExprStm(expr).addStm()
    fun STM(decl: Decl): DeclStm = DeclStm(decl).addStm()
    fun WHILE(expr: Expr, compact: Boolean = true, block: StmBuilder.() -> Unit): WhileLoopStm = WhileLoopStm(expr, buildStm(compact) { block() }).addStm()
    fun FOR(id: String, expr: Expr, block: (StmBuilder.() -> Unit)? = null): ForLoopStm =
        ForLoopStm(expr, VariableDecl(id), body = block?.let { buildStmCompact { block() } })
    fun RETURN(expr: Expr? = null, label: String? = null): ReturnExpr = ReturnExpr(expr, label).addStm()
    fun THROW(expr: Expr): ThrowExpr = ThrowExpr(expr).addStm()
    fun WHEN(expr: Expr? = null, block: WhenBuilder.() -> Unit): WhenExpr {
        val builder = WhenBuilder().also(block)
        return WhenExpr(WhenExpr.Subject(expr), builder.entries)
    }
    fun LAMBDA(vararg params: VariableDeclBase, block: StmBuilder.() -> Unit = {}): LambdaFunctionExpr {
        return LambdaFunctionExpr(Stms(buildStmList { block() }), if (params.isNotEmpty()) params.toList() else null)
    }

}

class WhenBuilder {
    val entries = arrayListOf<WhenExpr.Entry>()
    fun CASE(expr: Expr, body: ExprOrStm): WhenExpr.Entry {
        return WhenExpr.Entry(listOf(WhenExpr.Condition(expr = expr)), body.toStm()).also {
            entries += it
        }
    }
}

interface NodeBuilder {
    companion object : NodeBuilder {
        operator fun <T> invoke(block: NodeBuilder.() -> T): T = block(NodeBuilder)
    }

    fun STR(vararg chunks: Any?): Expr {
        if (chunks.isEmpty()) return StringLiteralExpr("")
        if (chunks.size == 1 && chunks.first() is String) return StringLiteralExpr(chunks[0].toString())
        return InterpolatedStringExpr(chunks.map {
            when (it) {
                is String -> InterpolatedStringExpr.StringChunk(it)
                is Expr -> InterpolatedStringExpr.ExpressionChunk(it)
                is InterpolatedStringExpr.Chunk -> it
                else -> TODO()
            }
        })
    }
    val IntType: Type get() = "Int".type
    val Boolean.lit: BoolLiteralExpr get() = BoolLiteralExpr(this)
    val Int.lit: IntLiteralExpr get() = IntLiteralExpr(this.toLong())
    val Char.lit: CharLiteralExpr get() = CharLiteralExpr(this)
    val Long.lit: IntLiteralExpr get() = IntLiteralExpr(this, isLong = true)
    val String.lit: StringLiteralExpr get() = StringLiteralExpr(this)
    val String.id: IdentifierExpr get() = IdentifierExpr(this)
    val String.multiType: MultiType get() = this.type.multi
    fun Type.generic(vararg generics: Type): GenericType = GenericType(this, generics.toList())
    val String.type: SimpleType get() = SimpleType(this)
    val Type.multi: MultiType get() = MultiType(this)
    val List<Type>.multi: MultiType get() = MultiType(this)
    fun collection(vararg items: Expr): CollectionLiteralExpr = CollectionLiteralExpr(items.toList())
    fun Expr.notNull(): UnaryPostOpExpr = UnaryPostOpExpr(this, UnaryPostOp.NOT_NULL)
    operator fun Expr.get(vararg indices: Expr): IndexedExpr = IndexedExpr(this, indices.toList())
    operator fun Expr.get(dot: String): NavigationExpr = NavigationExpr(".", this, dot)
    operator fun Expr.invoke(vararg params: Expr, lambdaArg: Expr? = null, typeArgs: List<Type>? = null): BaseCallExpr =
        when (this) {
            //is IdentifierExpr -> CallIdExpr(null, this.id, ".", params.toList(), lambdaArg, typeArgs)
            else -> CallExpr(this, params.toList(), lambdaArg, typeArgs)
        }
    fun Expr.infix(key: String, expr: Expr): Expr = BinaryOpExpr(this, key, expr)
    operator fun Expr.unaryMinus(): UnaryPreOpExpr = UnaryPreOpExpr(UnaryPreOp.MINUS, this)
    operator fun Expr.unaryPlus(): UnaryPreOpExpr = UnaryPreOpExpr(UnaryPreOp.PLUS, this)
    fun Expr.castTo(targetType: Type, safe: Boolean = false): Expr =
        CastExpr(this, targetType, safe)
    fun Expr.safeCastTo(targetType: Type, safe: Boolean = true): Expr = castTo(targetType, safe)
    operator fun Expr.plus(that: Expr): Expr = BinaryOpExpr(this, "+", that)
    operator fun Expr.minus(that: Expr): Expr = BinaryOpExpr(this, "-", that)
    operator fun Expr.times(that: Expr): Expr = BinaryOpExpr(this, "*", that)
    operator fun Expr.div(that: Expr): Expr = BinaryOpExpr(this, "/", that)
    infix fun Expr.shl(that: Expr): Expr = BinaryOpExpr(this, "shl", that)
    infix fun Expr.shr(that: Expr): Expr = BinaryOpExpr(this, "shr", that)
    infix fun Expr._in(that: Expr): Expr = RangeTestExpr(this, "in", that)
    infix fun Expr._notIn(that: Expr): Expr = RangeTestExpr(this, "!in", that)
    infix fun Expr._is(that: Type): Expr = TypeTestExpr(this, "is", that)
    infix fun Expr._notIs(that: Type): Expr = TypeTestExpr(this, "!is", that)
    val THIS: ThisExpr get() = ThisExpr(null)
    fun OBJECT_LIT(vararg subtypes: SubTypeInfo, isData: Boolean = false, block: DeclBuilder.() -> Unit): ObjectLiteralExpr =
        ObjectLiteralExpr(subtypes.toList(), body = DeclBuilder.buildDeclList { block() }, isData = isData)
    fun IF(cond: Expr, trueBody: Stm): IfExpr = IfExpr(cond, trueBody)
    fun IF(cond: Expr, trueBody: StmBuilder.() -> Unit): IfExpr = IfExpr(cond, StmBuilder.buildStmCompact { trueBody() })
    infix fun IfExpr.ELSE(falseBody: Stm): IfExpr = copy(falseBody = falseBody)
    infix fun IfExpr.ELSE(falseBody: StmBuilder.() -> Unit): IfExpr = copy(falseBody = StmBuilder.buildStmCompact { falseBody() })
}
