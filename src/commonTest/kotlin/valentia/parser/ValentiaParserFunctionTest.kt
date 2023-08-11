package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserFunctionTest : StmBuilder {

    @Test
    fun testSimpleFunction() {
        assertEquals(
            //"println".id("Hello World!".lit),
            FUN("sum", IntType, "a" to IntType, "b" to IntType) {
                RETURN("a".id + "b".id)
            },
            ValentiaParser.topLevelDecl("fun sum(a: Int, b: Int): Int { return a + b }")
        )
    }

    @Test
    fun testPrintHelloWorld() {
        assertEquals(
            "println".id("Hello World!".lit),
            ValentiaParser.expression("println(\"Hello World!\")")
        )
    }

    @Test
    fun testSimplestFunctionCall() {
        assertEquals(
            "exit".id(),
            ValentiaParser.expression("exit()")
        )
    }

    @Test
    fun testSimplerFunctionCall() {
        assertEquals(
            "exit".id(-(1).lit),
            ValentiaParser.expression("exit(-1)")
        )
    }

    @Test
    fun testSimpleFunctionCall() {
        assertEquals(
            "max".id(12.lit, 34.lit),
            ValentiaParser.expression("max(12, 34)")
        )
    }

}