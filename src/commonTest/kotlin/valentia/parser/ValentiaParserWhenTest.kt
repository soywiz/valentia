package valentia.parser

import valentia.ast.ExprStm
import valentia.ast.NodeBuilder.Companion.lit
import valentia.ast.VariableDecl
import valentia.ast.WhenExpr
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserWhenTest {

    @Test
    fun testWhenSimplest() {
        assertEquals(
            WhenExpr(subject=null, entries=listOf(WhenExpr.Entry(conditions = null, body = ExprStm(expr = 1.lit)))),
            ValentiaParser.expression("""
                when {
                    else -> 1
                }
            """.trimIndent())
        )
    }

    @Test
    fun testWhenSubjectSimplest() {
        assertEquals(
            WhenExpr(subject= WhenExpr.Subject(expr = 1.lit), entries=listOf(WhenExpr.Entry(conditions = null, body = ExprStm(expr = 1.lit)))),
            ValentiaParser.expression("""
                when (1) {
                    else -> 1
                }
            """.trimIndent())
        )
    }

    @Test
    fun testWhen1() {
        assertEquals(
            WhenExpr(subject= WhenExpr.Subject(expr = 1.lit, decl= VariableDecl(id="a", type=null)), entries=listOf(
                WhenExpr.Entry(conditions = null, body = ExprStm(expr = 1.lit)))),
            ValentiaParser.expression("""
                when (val a = 1) {
                    else -> 1
                }
            """.trimIndent())
        )
    }

}