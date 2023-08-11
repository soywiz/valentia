package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserLoopWhileTest : StmBuilder {
    @Test
    fun testSimplestWhile() {
        assertEquals(
            WHILE(1.lit) { },
            ValentiaParser.statement("while (1) ;")
        )
    }
}