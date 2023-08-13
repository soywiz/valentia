package valentia.parser

import java.io.File
import kotlin.test.Test
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class ParseValentiaSrc {
    @Test
    fun test() {
        val files = File(".").walkBottomUp().filter { it.extension == "kt" }
        for (file in files) {
            println("FILE: $file : ")
            val text = file.readText()
            val (tokens, timeTokenize) = measureTimedValue { ValentiaTokenizer(text).tokenize() }
            println("   -> tokenize=$timeTokenize")
            val (nodes, timeParse) = measureTimedValue { ValentiaParser(tokens).valentiaFile() }
            println("   -> parse=$timeParse")
            println("   -> topLevelDecls=${nodes.topLevelDecls.size}")
        }
    }
}