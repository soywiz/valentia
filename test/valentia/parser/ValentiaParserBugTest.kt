package valentia.parser

import kotlin.test.Ignore
import kotlin.test.Test

class ValentiaParserBugTest {
    @Test
    @Ignore
    fun testBug1() {
        ValentiaParser.file("fun main() { level2@for (m in 0 .. 4) for (n in 0 .. m) { if (n == 3) break@level2; console.log(m, n) } }")
    }
}