package valentia.transform

import valentia.ast.*

class TransformUnsupportedNodes(val supported: (Node) -> Boolean) {
    fun transform(stm: Stm): Stm {
        return stm
    }

    fun ensure(expr: Expr): Pair<Stm?, Expr?> {
        when (expr) {
            is ReturnExpr -> {
                return ReturnStm(expr.expr) to null
            }
            is IfExpr -> {
                if (expr.falseBody == null) {
                    return IfStm(expr.cond, expr.trueBody.toStm()) to null
                }
                val type = UnificationExprType(expr.trueBody, expr.falseBody)
                val temp = Temp(type)
                //TempExpr(temp)
                val tempExpr = IdentifierExpr("temp")
                val btrue = AssignStm(tempExpr, "=", (expr.trueBody as ExprStm).expr)
                val bfalse = expr.falseBody?.let { AssignStm(tempExpr, "=", (it as ExprStm).expr) }
                return IfStm(expr.cond, btrue, bfalse) to tempExpr
            }
            is BreakExpr -> return BreakStm(expr.label) to null
            is ContinueExpr -> return ContinueStm(expr.label) to null
        }
        return null to expr
    }
}