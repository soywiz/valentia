package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.ExternalInterface
import valentia.parser.ValentiaParser
import valentia.sema.Program
import kotlin.test.Test
import kotlin.test.assertEquals

class JSCodegenTest {
    @Test
    fun test() {
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

    fun genAndRunJs(vararg filesContent: String, extraArgs: List<Any> = emptyList(), trim: Boolean = true): String {
        return runJsCode(genFilesJSString(*filesContent)).let { if (trim) it.trim() else it }
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