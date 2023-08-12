package valentia.parser

import java.io.File
import kotlin.test.Test

class ParseValentiaSrc {
    @Test
    fun test() {
        val files = File(".").walkBottomUp().filter { it.extension == "kt" }
        for (file in files) {
            println("FILE: $file : ")
            val text = file.readText()
            val nodes = ValentiaParser(text).valentiaFile()
            println("   -> topLevelDecls=${nodes.topLevelDecls.size}")
        }
    }
}