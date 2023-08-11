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
        }
        return null to expr
    }
}