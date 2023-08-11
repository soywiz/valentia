package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.ExternalInterface
import valentia.parser.ValentiaParser
import valentia.sema.Program
import kotlin.test.Test
import kotlin.test.assertEquals

class JSCodegenTest {
    @Test
    fun testSuspend() {
        println(genFilesJSString("suspend fun test() { }"))
        println(genFilesJSString("suspend fun test() = 1"))
    }

    @Test
    fun testOverload() {
        assertEquals(
            "String\nChar",
            genAndRunJs("""
                fun foverload(a: Int): String { return "Int" }
                fun foverload(a: String): String { return "String" }
                fun foverload(a: Char): String { return "Char" }
                fun foverload(a: Float): String { return "Float" }
                fun main() {
                    console.log(foverload("a"))
                    console.log(foverload('a'))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testReturnIf() {
        assertEquals(
            "1\n-3",
            genAndRunJs("""
                fun min(a: Int, b: Int): Int { return if (a < b) a else b }
                fun main() {
                    console.log(min(1, 2))
                    console.log(min(4, -3))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testSimpleGen() {
        assertEquals(
            "12",
            genAndRunJs("""
                fun sum(a: Int, b: Int): Int { return a + b }
                fun main() {
                    console.log(sum(1, 3) * 3)
                }
            """.trimIndent())
        )
    }

    @Test
    fun testNode() {
        assertEquals("hello world", runJsCode("console.log('hello world')").trim())
    }

    fun genAndRunJs(vararg filesContent: String, extraArgs: List<Any> = emptyList(), trim: Boolean = true, printJs: Boolean = false): String {
        val jsCode = genFilesJSString(*filesContent)
        if (printJs) println(jsCode)
        return runJsCode(jsCode, *extraArgs.toTypedArray()).let { if (trim) it.trim() else it }
    }

    fun runJsCode(jsCode: String, vararg extraArgs: Any): String {
        val tempFile = "${ExternalInterface.TEMP}/valentia.temp.js"
        ExternalInterface.fileWriteString(tempFile, jsCode)
        val out = ExternalInterface.exec("node", tempFile, *extraArgs.map { it.toString() }.toTypedArray())
        if (out.exitCode != 0) {
            error("ERROR: $out")
        }
        return out.stdout
    }

    fun genFilesJSString(
        @Language("kotlin")
        vararg filesContent: String
    ): String = genFiles(*filesContent).indentToString()

    fun genFiles(
        @Language("kotlin")
        vararg filesContent: String
    ): JSCodegen {
        val gen = JSCodegen()
        val program = Program()
        val module = program.getModule(null)
        for (content in filesContent) {
            module.addFile(ValentiaParser.file(content))
        }
        gen.generateProgram(program)
        return gen
    }
}