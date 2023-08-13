package valentia.parser

import valentia.ast.StmBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserCallExprTest : StmBuilder {
    @Test
    fun testCallExpr() {
        assertEquals(
            null,
            ValentiaParser.expression("max(a, b)") as? Any?
        )
    }

    @Test
    fun testCallPlusLambdaExpr() {
        assertEquals(
            null,
            ValentiaParser.expression("max(a, b) { 1 }") as? Any?
        )
    }

    @Test
    fun testCallLambdaAloneExpr() {
        assertEquals(
            null,
            ValentiaParser.expression("max { 1 }") as? Any?
        )
    }
}