package valentia.parser

import valentia.ast.FileNode
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class ParseValentiaSrc {
    var count = 0
    @Test
    fun testRuntime() {
        doTestFolder("./runtime")
    }

    @Test
    fun testSrc() {
        //doTestFolder("..")
        doTestFolder("./src")
    }

    @Test
    fun testKorge() {
        doTestFolder("../korge")
    }

    @Test
    fun testParser() {
        doTest("./src/commonMain/kotlin/valentia/parser/KotlinParser.kt")
    }

    fun doTestFolder(folder: String) {
        val files = File(folder).walkBottomUp().filter { it.extension == "kt" }
        for (file in files) {
            doTest(file.path)
        }
    }

    fun doTest(file: String) {
        val file = File(file)
        println("FILE[${count++}]: $file : ")
        val text = file.readText()
        var _timeTokenize: Duration? = null
        var _timeParse: Duration? = null
        var _nodes: FileNode? = null
        val startTime = TimeSource.Monotonic.markNow()
        try {
            val (tokens, timeTokenize) = measureTimedValue { ValentiaTokenizer(text).tokenize() }
            _timeTokenize = timeTokenize
            val (nodes, timeParse) = measureTimedValue { ValentiaParser(tokens).valentiaFile() }
            _nodes = nodes
            _timeParse = timeParse
        } finally {
            val endTime = TimeSource.Monotonic.markNow()
            println("   -> tokenize=$_timeTokenize, parse=$_timeParse, total=${endTime - startTime}, topLevelDecls=${_nodes?.topLevelDecls?.size}")
        }
    }
}