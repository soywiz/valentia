package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.parser.ValentiaParser
import valentia.sema.Program
import kotlin.test.Test

class JSCodegenTest {
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

    @Test
    fun test() {
        val gen = genFiles(
            """
                class Test {
                }

                fun test(a: Int, b: String) {       
                    while (true) {
                        return 1
                    }
                }
            """.trimIndent()
        )
        println(gen.indentToString())
    }
}