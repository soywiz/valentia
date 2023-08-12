package valentia.parser

import valentia.ast.StmBuilder
import kotlin.test.Test

class ValentiaParserFullExamplesTest : StmBuilder {
    @Test
    fun test() {
        val DOLLAR = "\$"
        ValentiaParser.file("""
            package valentia.parser

            import java.io.File
            import kotlin.test.Test

            class ParseValentiaSrc {
                @Test
                fun test() {
                    val files = File(".").walkBottomUp().filter { it.extension == "kt" }
                    for (file in files) {
                        println("FILE: ${DOLLAR}file : ")
                        val text = file.readText()
                        val nodes = ValentiaParser(text).valentiaFile()
                        println("   -> topLevelDecls=${DOLLAR}{nodes.topLevelDecls.size}")
                    }
                }
            }
        """.trimIndent())
    }
}