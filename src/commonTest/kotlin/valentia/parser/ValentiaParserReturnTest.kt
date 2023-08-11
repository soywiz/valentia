package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserReturnTest : StmBuilder {

    @Test
    fun testReturn() {
        assertEquals(
            RETURN(1.lit),
            ValentiaParser.expression("return 1")
        )
    }

    @Test
    fun testReturnAt() {
        assertEquals(
            RETURN(1.lit, label = "test"),
            ValentiaParser.expression("return@test 1")
        )
    }

}