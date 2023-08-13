package valentia.parser

import valentia.ast.Decl
import valentia.ast.FileNode
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class ParseValentiaSrc {
    @Test
    fun test() {
        val files = File(".").walkBottomUp().filter { it.extension == "kt" }
        for (file in files) {
            doTest(file)
        }
    }

    @Test
    fun testParser() {
        doTest(File("./src/commonMain/kotlin/valentia/parser/KotlinParser.kt"))
    }

    fun doTest(file: File) {
        println("FILE: $file : ")
        val text = file.readText()
        var _timeTokenize: Duration? = null
        var _timeParse: Duration? = null
        var _nodes: FileNode? = null
        try {
            val (tokens, timeTokenize) = measureTimedValue { ValentiaTokenizer(text).tokenize() }
            _timeTokenize = timeTokenize
            val (nodes, timeParse) = measureTimedValue { ValentiaParser(tokens).valentiaFile() }
            _nodes = nodes
            _timeParse = timeParse
        } finally {
            println("   -> tokenize=$_timeTokenize, parse=$_timeParse, topLevelDecls=${_nodes?.topLevelDecls?.size}")
        }
    }
}