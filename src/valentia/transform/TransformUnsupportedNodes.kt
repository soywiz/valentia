package valentia.transform

import valentia.ast.*

class TransformUnsupportedNodes(val supported: (Node) -> Boolean) {
    fun transform(stm: Stm): Stm {
        return stm
    }

    class TransformContext {
        var id = 0
        val temps = arrayListOf<Temp>()
        fun createTemp(type: Type): Temp {
            return Temp(type, id++).also { temps += it }
        }
    }

    fun ensure(stm: Stm?, ctx: TransformContext): Stm? {
        return when (stm) {
            null -> null
            is ExprStm -> {
                val expr = stm.expr
                when (expr) {
                    is IfExpr -> IfStm(expr.cond, expr.trueBody.toStm(), expr.falseBody?.toStm()).copyFrom(stm)
                    is ThrowExpr -> ThrowStm(expr.expr).copyFrom(stm)
                    else -> stm
                }
            }
            is Stms -> {
                Stms(stm.stms.mapNotNull { ensure(it, ctx) }).copyFrom(stm)
            }
            else -> return stm
        }
    }

    fun ensure(expr: Expr, ctx: TransformContext): Pair<Stm?, Expr?> {
        when (expr) {
            is ReturnExpr -> {
                return ReturnStm(expr.expr) to null
            }
            is IfExpr -> {
                if (
                    (expr.trueBody is ExprStm || expr.trueBody is Expr) &&
                    (expr.falseBody is ExprStm || expr.falseBody is Expr)
                ) {
                    val trueBody = expr.trueBody
                    val falseBody = expr.falseBody
                    return null to TernaryExpr(
                        expr.cond,
                        if (trueBody is ExprStm) trueBody.expr else trueBody as Expr,
                        if (falseBody is ExprStm) falseBody.expr else falseBody as Expr,
                    )
                }
                if (expr.trueBody is Expr && expr.falseBody is Expr) {
                    return null to TernaryExpr(expr.cond, expr.trueBody, expr.falseBody)
                }
                if (expr.falseBody == null) {
                    return IfStm(expr.cond, expr.trueBody.toStm()) to null
                }
                val type = UnificationExprType(expr.trueBody, expr.falseBody)
                val temp = ctx.createTemp(type)
                //TempExpr(temp)
                val tempExpr = TempExpr(temp)
                val btrue = AssignStm(tempExpr, "=", (expr.trueBody as ExprStm).expr)
                val bfalse = expr.falseBody?.let { AssignStm(tempExpr, "=", (it as ExprStm).expr) }
                return IfStm(expr.cond, btrue, bfalse) to tempExpr
            }
            is BreakExpr -> return BreakStm(expr.label) to null
            is ContinueExpr -> return ContinueStm(expr.label) to null
            else -> Unit
        }
        return null to expr
    }
}