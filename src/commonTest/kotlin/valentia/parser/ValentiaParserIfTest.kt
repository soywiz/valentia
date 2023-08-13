package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserIfTest : StmBuilder {
    @Test
    fun testIfCommaExpression() {
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody = EmptyStm()),
            ValentiaParser.expression("if (true) ;")
        )
    }

    @Test
    fun testIfExprExpression() {
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody = ExprStm(expr=IntLiteralExpr(1))),
            ValentiaParser.expression("if (true) 1")
        )
    }

    @Test
    fun testIfElseExpression() {
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody=ExprStm(expr=IntLiteralExpr(1)), falseBody=ExprStm(expr=IntLiteralExpr(2))),
            ValentiaParser.expression("""
                if (true) 1 else 2
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun testIfElseWithCommaExpression() {
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody=ExprStm(expr=IntLiteralExpr(1)), falseBody=ExprStm(expr=IntLiteralExpr(2))),
            ValentiaParser.expression("""
                if (true) 1; else 2;
            """.trimIndent()) as? Any?
        )
    }

}