package valentia.ast

interface StmBuilder {
    companion object {
        fun buildStms(block: StmBuilder.() -> Unit): Stms = Stms(buildStmList(block))
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
    fun WHILE(expr: Expr, block: StmBuilder.() -> Unit): WhileLoopStm = WhileLoopStm(expr, buildStmCompact { block() }).addStm()
    fun FUN(name: String, ret: TypeNode, vararg params: Pair<String, TypeNode>, block: StmBuilder.() -> Unit = {}): FunDecl =
        FunDecl(name, params.map { FuncValueParam(it.first, it.second) }, body = buildStms { block() })
    fun RETURN(expr: Expr? = null, label: String? = null): ReturnExpr = ReturnExpr(expr, label).addStm()
}

interface NodeBuilder {
    companion object : NodeBuilder {
        operator fun <T> invoke(block: NodeBuilder.() -> T): T = block(NodeBuilder)
    }

    val IntType: SimpleType get() = "Int".type
    val Boolean.lit: BoolLiteralExpr get() = BoolLiteralExpr(this)
    val Int.lit: IntLiteralExpr get() = IntLiteralExpr(this.toLong())
    val Long.lit: IntLiteralExpr get() = IntLiteralExpr(this, isLong = true)
    val String.lit: StringLiteralExpr get() = StringLiteralExpr(this)
    val String.id: IdentifierExpr get() = IdentifierExpr(this)
    val String.type: SimpleType get() = SimpleType(this)
    operator fun Expr.invoke(vararg params: Expr, lambdaArg: Expr? = null, typeArgs: List<TypeNode>? = null): CallExpr = CallExpr(this, params.toList(), lambdaArg, typeArgs)
    operator fun Expr.unaryMinus(): UnaryPreOpExpr = UnaryPreOpExpr("-", this)
    operator fun Expr.unaryPlus(): UnaryPreOpExpr = UnaryPreOpExpr("+", this)
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
