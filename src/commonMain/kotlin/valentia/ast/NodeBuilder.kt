package valentia.ast

interface StmBuilder : NodeBuilder {
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

    fun STM(expr: Expr): ExprStm = ExprStm(expr).addStm()
    fun STM(decl: Decl): DeclStm = DeclStm(decl).addStm()
    fun WHILE(expr: Expr, compact: Boolean = true, block: StmBuilder.() -> Unit): WhileLoopStm = WhileLoopStm(expr, buildStm(compact) { block() }).addStm()
    fun FOR(id: String, expr: Expr, block: (StmBuilder.() -> Unit)? = null): ForLoopStm =
        ForLoopStm(expr, VariableDecl(id), body = block?.let { buildStmCompact { block() } })
    fun FUN(
        name: String,
        ret: TypeNode? = null,
        vararg params: Pair<String, TypeNode>,
        block: StmBuilder.() -> Unit = {}
    ): FunDecl =
        FunDecl(name, params.map { FuncValueParam(it.first, it.second) }, retType = ret, body = buildStms { block() })
    fun RETURN(expr: Expr? = null, label: String? = null): ReturnExpr = ReturnExpr(expr, label).addStm()
    fun WHEN(expr: Expr? = null, block: WhenBuilder.() -> Unit): WhenExpr {
        val builder = WhenBuilder().also(block)
        return WhenExpr(WhenExpr.Subject(expr), builder.entries)
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
    val IntType: TypeNode get() = "Int".type
    val Boolean.lit: BoolLiteralExpr get() = BoolLiteralExpr(this)
    val Int.lit: IntLiteralExpr get() = IntLiteralExpr(this.toLong())
    val Char.lit: CharLiteralExpr get() = CharLiteralExpr(this)
    val Long.lit: IntLiteralExpr get() = IntLiteralExpr(this, isLong = true)
    val String.lit: StringLiteralExpr get() = StringLiteralExpr(this)
    val String.id: IdentifierExpr get() = IdentifierExpr(this)
    val String.multiType: MultiType get() = this.type.multi
    fun TypeNode.generic(vararg generics: TypeNode): GenericType = GenericType(this, generics.toList())
    val String.type: SimpleType get() = SimpleType(this)
    val TypeNode.multi: MultiType get() = MultiType(this)
    val List<TypeNode>.multi: MultiType get() = MultiType(this)
    fun collection(vararg items: Expr): CollectionLiteralExpr = CollectionLiteralExpr(items.toList())
    operator fun Expr.get(vararg indices: Expr): IndexedExpr = IndexedExpr(this, indices.toList())
    operator fun Expr.invoke(vararg params: Expr, lambdaArg: Expr? = null, typeArgs: List<TypeNode>? = null): CallExpr = CallExpr(this, params.toList(), lambdaArg, typeArgs)
    operator fun Expr.unaryMinus(): UnaryPreOpExpr = UnaryPreOpExpr(UnaryPreOp.MINUS, this)
    operator fun Expr.unaryPlus(): UnaryPreOpExpr = UnaryPreOpExpr(UnaryPreOp.PLUS, this)
    fun Expr.castTo(targetType: TypeNode, safe: Boolean = false): Expr =
        CastExpr(this, targetType, if (safe) "as?" else "as")
    fun Expr.safeCastTo(targetType: TypeNode, safe: Boolean = true): Expr = castTo(targetType, safe)
    operator fun Expr.plus(that: Expr): Expr = OpSeparatedExprs(listOf("+"), listOf(this, that))
    operator fun Expr.minus(that: Expr): Expr = OpSeparatedExprs(listOf("-"), listOf(this, that))
    operator fun Expr.times(that: Expr): Expr = OpSeparatedExprs(listOf("*"), listOf(this, that))
    operator fun Expr.div(that: Expr): Expr = OpSeparatedExprs(listOf("/"), listOf(this, that))
    infix fun Expr.shl(that: Expr): Expr = OpSeparatedExprs(listOf("shl"), listOf(this, that))
    infix fun Expr.shr(that: Expr): Expr = OpSeparatedExprs(listOf("shr"), listOf(this, that))
    infix fun Expr._in(that: Expr): Expr = RangeTestExpr(this, "in", that)
    infix fun Expr._notIn(that: Expr): Expr = RangeTestExpr(this, "!in", that)
    infix fun Expr._is(that: TypeNode): Expr = TypeTestExpr(this, "is", that)
    infix fun Expr._notIs(that: TypeNode): Expr = TypeTestExpr(this, "!is", that)
}
