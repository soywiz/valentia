package valentia.parser

import valentia.ast.IntLiteralExpr
import valentia.ast.OpSeparatedExprs
import valentia.ast.StmBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserExprTest : StmBuilder {
    @Test
    fun testSimplestExpr() {
        assertEquals(
            OpSeparatedExprs(listOf("+"), listOf(IntLiteralExpr(1), IntLiteralExpr(5))),
            ValentiaParser.expression("1 \n + 5")
        )
    }

    @Test
    fun testSimpleExpr() {
        assertEquals(
            OpSeparatedExprs(listOf("*"), listOf(IntLiteralExpr(1000), IntLiteralExpr(200))),
            ValentiaParser.expression("1_000 * 2_0_0")
        )
    }

    @Test
    fun testExpr() {
        assertEquals(
            2.lit * (3.lit + 4.lit),
            ValentiaParser.expression("2 * (3 + 4)")
        )
    }

    @Test
    fun testInfix() {
        assertEquals(
            1.lit shl 5.lit,
            ValentiaParser.expression("1 shl 5")
        )
    }

    @Test
    fun testIn() {
        assertEquals(
            1.lit _in 5.lit,
            ValentiaParser.expression("1 in 5")
        )
    }

    @Test
    fun testNotIn() {
        assertEquals(
            1.lit _notIn 5.lit,
            ValentiaParser.expression("1 !in 5")
        )
    }

    @Test
    fun testIs() {
        assertEquals(
            1.lit _is "Int".userType,
            ValentiaParser.expression("1 is Int")
        )
    }

    @Test
    fun testNotIs() {
        assertEquals(
            1.lit _notIs "Int".userType,
            ValentiaParser.expression("1 !is Int")
        )
    }

    @Test
    fun testAs() {
        assertEquals(
            1.lit.safeCastTo("Float".type.user),
            ValentiaParser.expression("1 as? Float")
        )
        assertEquals(
            1.lit.castTo("Float".type.user),
            ValentiaParser.expression("1 as Float")
        )
    }
}