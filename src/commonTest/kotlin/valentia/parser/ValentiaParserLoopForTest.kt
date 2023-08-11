package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserLoopForTest : StmBuilder {
    @Test
    fun testSimplestFor() {
        assertEquals(
            FOR("n", 1.lit),
            ValentiaParser.statement("for (n in 1) ;")
        )
    }

    @Test
    fun testSimpleFor() {
        assertEquals(
            FOR("n", 1.lit) {
                STM("println".id("n".id))
            },
            ValentiaParser.statement("for (n in 1) println(n)")
        )
    }
}