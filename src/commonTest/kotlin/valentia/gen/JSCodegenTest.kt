package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.parser.ValentiaParser
import valentia.sema.Program
import kotlin.test.Test

class JSCodegenTest {
    @Test
    fun test() {
        val gen = genFiles(
            """
                //val a = 10

                class Test {
                    fun demo() = 1
                }

                fun test(a: Int, b: String) {   
                    a++
                    for (n in 0 until 10) {
                        println(n)
                        if (n == 5) println("test")
                    }
                    while (true) {
                        return 1
                    }
                }
            """.trimIndent(),
        )
        println(gen.indentToString())
    }

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