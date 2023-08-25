package valentia.parser

import valentia.ast.LambdaFunctionExpr
import valentia.ast.StmBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserCallExprTest : StmBuilder {
    @Test
    fun testCallExpr2() {
        assertEquals(
            "listener".id["call"]("a".id, "b".id),
            ValentiaParser.expression("listener.call(a, b)") as? Any?
        )
    }

    @Test
    fun testCallExpr() {
        assertEquals(
            "max".id("a".id, "b".id),
            ValentiaParser.expression("max(a, b)") as? Any?
        )
    }

    @Test
    fun testCallPlusLambdaExpr() {
        assertEquals(
            "max".id("a".id, "b".id, lambdaArg = LAMBDA { STM(1.lit) }),
            ValentiaParser.expression("max(a, b) { 1 }") as? Any?
        )
    }

    @Test
    fun testCallLambdaAloneExpr() {
        assertEquals(
            "max".id(lambdaArg = LAMBDA { STM(1.lit) }),
            ValentiaParser.expression("max { 1 }") as? Any?
        )
    }
}