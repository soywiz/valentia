package valentia.parser

import valentia.ast.Modifier
import valentia.ast.VariableDecl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserBugTest {
    @Test
    @Ignore
    fun testBug1() {
        ValentiaParser.file("fun main() { level2@for (m in 0 .. 4) for (n in 0 .. m) { if (n == 3) break@level2; console.log(m, n) } }")
    }

    @Test
    fun testBug2() {
        val file = ValentiaParser.file("""
            external class Int
            external val console: dynamic
        """.trimIndent())
        assertEquals(
            true,
            Modifier.Companion.EXTERNAL in (file.topLevelDeclsByName["console"]!!.first() as VariableDecl).modifiers
        )

    }
}