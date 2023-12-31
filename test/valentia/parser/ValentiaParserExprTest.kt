package valentia.parser

import valentia.ast.BinaryOpExpr
import valentia.ast.IntLiteralExpr
import valentia.ast.OpSeparatedBinaryExprs
import valentia.ast.StmBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class ValentiaParserExprTest : StmBuilder {
    @Test
    fun testSimplestExpr() {
        assertEquals(
            1.lit + 5.lit,
            ValentiaParser.expression("1 + \n 5")
        )
    }
    @Test
    fun testSimplestExprInvalid() {
        assertFailsWith<EofNotFoundException> { ValentiaParser.expression("1 \n + 5") }
    }

    @Test
    fun testSimpleExpr() {
        assertEquals(
            BinaryOpExpr(1000.lit, "*", 200.lit),
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
            1.lit _is "Int".type,
            ValentiaParser.expression("1 is Int")
        )
    }

    @Test
    fun testNotIs() {
        assertEquals(
            1.lit _notIs "Int".type,
            ValentiaParser.expression("1 !is Int")
        )
    }

    @Test
    fun testAs() {
        assertEquals(
            1.lit.safeCastTo("Float".type),
            ValentiaParser.expression("1 as? Float")
        )
        assertEquals(
            1.lit.castTo("Float".type),
            ValentiaParser.expression("1 as Float")
        )
    }

    @Test
    fun testExpressionFail() {
        val invalid = "}"
        assertFailsWith<IllegalStateException> {
            ValentiaParser.expression(invalid)
        }
        //val e = assertFailsWith<IllegalStateException> {
        //    ValentiaParser.expression(invalid)
        //}
        //assertTrue(e.message!!.contains("Not an expression"))
    }

    @Test
    fun test2() {
        ValentiaParser.statement("(c - 'a') + 10")
    }
}