package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTryCatchTest : StmBuilder {
    @Test
    fun testTryCatch() {
        assertEquals(
            TryCatchExpr(body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(1)))), catches=listOf(TryCatchExpr.Catch(local="e", type="Throwable".type, body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(2)))))), finally=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(3))))),
            ValentiaParser.expression("try { 1 } catch (e: Throwable) { 2 } finally { 3 }")
        )
    }

    @Test
    fun testTryFinallyCatch() {
        assertEquals(
            TryCatchExpr(body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(1)))), finally=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(2))))),
            ValentiaParser.expression("try { 1 } finally { 2 }")
        )
    }
}