package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserStmTest : StmBuilder {
    @Test
    fun testOneCall() {
        assertEquals(
            listOf(STM("demo".id["call1"]())),
            ValentiaParser.statements("""
                demo.call1()
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun testTwoCalls() {
        assertEquals(
            listOf(STM("demo".id["call1"]()), STM("demo".id["call2"]())),
            ValentiaParser.statements("""
                demo.call1()
                demo.call2()
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun testTwoCallsWithInfix() {
        assertEquals(
            listOf(
                STM("demo".id["call1"]().infix("shl", 1.lit)),
                STM("demo".id["call2"]())
            ),
            ValentiaParser.statements("""
                demo.call1() shl 1
                demo.call2()
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun testUnaryPrefix() {
        assertEquals(
            STM(UnaryPreOpExpr(UnaryPreOp.DECR, "myvar".id)),
            ValentiaParser.statement("""
                --myvar
            """.trimIndent()) as? Any?
        )
    }
}