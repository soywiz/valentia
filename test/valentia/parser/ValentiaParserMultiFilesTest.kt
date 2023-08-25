package valentia.parser

import valentia.ast.Identifier
import valentia.ast.StmBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserMultiFilesTest : StmBuilder {
    @Test
    fun test() {
        val files = ValentiaParser.files("""
            package test
            class Test { }
            package demo
            class Test { }
        """.trimIndent())
        assertEquals(2, files.size)
        assertEquals(Identifier("test"), files[0]._package)
        assertEquals(1, files[0].topLevelDecls.size)
        assertEquals(Identifier("demo"), files[1]._package)
        assertEquals(1, files[1].topLevelDecls.size)
    }
}