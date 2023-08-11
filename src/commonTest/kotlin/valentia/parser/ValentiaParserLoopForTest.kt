package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserLoopForTest : StmBuilder {
    @Test
    fun testSimplestFor() {
        assertEquals(
            ForLoopStm(IntLiteralExpr(1), vardecl = VariableDecls(VariableDecl(id = "n", type = null)), body = null),
            ValentiaParser.statement("for (n in 1) ;")
        )
    }
}