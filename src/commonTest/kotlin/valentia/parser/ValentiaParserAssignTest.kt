package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserAssignTest : StmBuilder {
    @Test
    fun testAssign() {
        assertEquals(
            AssignStm(lvalue = "a".id, op = "=", expr = 10.lit),
            ValentiaParser.statement("""a = 10""")
        )
    }

    @Test
    fun testIndexerAssign() {
        assertEquals(
            AssignStm(lvalue = "a".id[11.lit], op = "+=", expr = 12.lit),
            ValentiaParser.assignment("""a[11] += 12""")
        )
    }

    @Test
    fun testAssignThis() {
        assertEquals(
            ASSIGN(THIS["symbolProvider"], "parent".id),
            ValentiaParser.assignment("""this.symbolProvider = parent""") as? Any?
        )
    }

}