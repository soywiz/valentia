package valentia.transform

import valentia.ast.*

class TransformUnsupportedNodes(val supported: (Node) -> Boolean) {
    fun transform(stm: Stm): Stm {
        return stm
    }

    class TransformContext {
        var id = 0
        val temps = arrayListOf<Temp>()
        fun createTemp(type: TypeNode): Temp {
            return Temp(type, id++).also { temps += it }
        }
    }

    fun ensure(expr: Expr, ctx: TransformContext): Pair<Stm?, Expr?> {
        when (expr) {
            is ReturnExpr -> {
                return ReturnStm(expr.expr) to null
            }
            is IfExpr -> {
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