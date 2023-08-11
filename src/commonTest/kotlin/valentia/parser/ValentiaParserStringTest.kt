package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserStringTest : StmBuilder {
    @Test
    fun testString1() {
        assertEquals(
            STR("hello".id, " world"),
            ValentiaParser.expression("\"\$hello world\"") as Any
        )
    }

    @Test
    fun testString2() {
        assertEquals(
            STR(1.lit + 2.lit, " world"),
            ValentiaParser.expression("\"\${1 + 2} world\"") as Any
        )
    }
}