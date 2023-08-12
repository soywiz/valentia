package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserObjectLiteralExprTest : StmBuilder {

    @Test
    fun testObjectLiteral() {
        assertEquals(
            ObjectLiteralExpr(body = emptyList()),
            ValentiaParser.expression("""object { }""")
        )
        assertEquals(
            ObjectLiteralExpr(body = listOf(FunDecl(name="test", params=emptyList(), body= Stms()))),
            ValentiaParser.expression("""object { fun test() { } }""")
        )
    }

    @Test
    fun testObjectLiteral2() {
        assertEquals(
            //ObjectLiteralExpr(body = listOf(FunDecl(name="test", params=emptyList(), body= Stms()))),
            null,
            ValentiaParser.statements("""
                val listener = object : KotlinParserBaseListener() { 
                }
                val stream = CharStreams
            """.trimIndent()) as Any?
        )
    }

}