package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.ExternalInterface
import valentia.ast.Program
import valentia.compiler.FileWithContents
import valentia.compiler.ValentiaCompiler
import valentia.parser.ValentiaParser
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class JSCodegenTest {
    @Test
    fun testIfTernary() {
        assertEquals(
            "a\nb",
            genAndRunJs("""
                fun main() {
                    val a = 10
                    val b = if (a == 10) "a" else "b"
                    val c = if (a != 10) "a" else "b"
                    console.log(b)
                    console.log(c)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testIterator() {
        assertEquals(
            "7",
            genAndRunJs("""
                external fun println(v: Any?)
                
                interface Iterator<out T> {
                    operator fun next(): T
                    operator fun hasNext(): Boolean
                }
                interface Iterable<out T> {
                    operator fun iterator(): Iterator<T>
                }
                abstract class IntIterator : Iterator<Int> {
                    override final fun next() = nextInt()
                    abstract fun nextInt(): Int
                }
                internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator() {
                    private val finalElement: Int = last
                    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
                    private var next: Int = if (hasNext) first else finalElement
                    override fun hasNext(): Boolean = hasNext
                    override fun nextInt(): Int {
                        val value = next
                        if (value == finalElement) {
                            if (!hasNext) throw kotlin.NoSuchElementException()
                            hasNext = false
                        } else {
                            next += step
                        }
                        return value
                    }
                }
                fun main() {
                    for (n in IntProgressionIterator(0, 10, +4)) {
                        println(n)
                    }
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    @Ignore
    fun testSuspending() {
        println(genFilesJSString("""
            external fun yield(value: Int)
            
            suspend fun test() {
                test@for (n in 0 until 10) {
                    yield(n)
                    break@test
                }
            }
        """.trimIndent()))
        println(genFilesJSString("suspend fun test() { }"))
        println(genFilesJSString("suspend fun test() = 1"))
    }

    @Test
    fun testSimple() {
        assertEquals(
            "1",
            genAndRunJs("""
                external val console: dynamic
                fun main() {
                    console.log(1)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testClassInstantiation() {
        assertEquals(
            "7",
            genAndRunJs("""
                class Demo { val a = 7 }
                fun main() = console.log(Demo().a)
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testClassInstantiationFunCalling() {
        assertEquals(
            "7",
            genAndRunJs("""
                class Demo { fun a() = 7 }
                fun main() = console.log(Demo().a())
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testClassInheritance() {
        assertEquals(
            "8\n9",
            genAndRunJs("""
                class Demo1 {
                    fun b() = 7
                    open fun a() = 1 + this.b() 
                }
                class Demo2 : Demo1 { override fun a() = 2 + this.b() }
                fun main() {
                    console.log(Demo1().a())
                    console.log(Demo2().a())
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testSimpleLoop() {
        assertEquals("0\n1\n2\n3", genAndRunJs("fun main() { for (n in 0 .. 3) console.log(n) }", printJs = true))
        assertEquals("0\n1\n2", genAndRunJs("fun main() { for (n in 0 ..< 3) console.log(n) }", printJs = true))
        assertEquals("0\n1\n2", genAndRunJs("fun main() { for (n in 0 until 3) console.log(n) }", printJs = true))
        assertEquals("3\n2\n1\n0", genAndRunJs("fun main() { for (n in 3 downTo 0) console.log(n) }", printJs = true))
    }

    val RESOURCES_PREFIX = "src/commonTest/resources"

    @Test
    fun testClassInstantiationValGetter() =
        testFile("$RESOURCES_PREFIX/cases/testClassInstantiationValGetter.kt.txt")

    @Test
    fun testClassMultipleConstructor() =
        testFile("$RESOURCES_PREFIX/cases/testClassMultipleConstructor.kt.txt")

    @Test
    fun testAllFiles() {
        val dir = "$RESOURCES_PREFIX/cases"
        val cases = ExternalInterface.fileList(dir)
        println("testAllFiles=$cases")
        for (case in cases) {
            val fpath = "$dir/$case"
            println("CASE: $fpath")
            if (fpath.endsWith(".kt.txt") || fpath.endsWith(".kt")) {
                testFile(fpath, printJs = false)
            }
        }
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
                    console.log(min(+4, -3))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testIntegerAddition() {
        0 until 10
        assertEquals(
            "-1073741824\n2\n949312677\n1018518717\n69206040",
            genAndRunJs("""
                fun main() {
                    console.log(2147483648 + 1073741824)
                    console.log(-2147483647 + -2147483647)
                    console.log(213212312 xor 874512445)
                    console.log(213212312 or 874512445)
                    console.log(213212312 and 874512445)
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

    fun genAndRunJs(@Language("kotlin") vararg filesContent: String, extraArgs: List<Any> = emptyList(), trim: Boolean = true, printJs: Boolean = false): String {
        val jsCode = genFilesJSString(*filesContent)
        if (printJs) println(jsCode)
        return runJsCode(jsCode, *extraArgs.toTypedArray()).let { if (trim) it.trim() else it }
    }

    fun runJsCode(jsCode: String, vararg extraArgs: Any): String {
        val tempFile = "${ExternalInterface.TEMP}/valentia.temp.js"
        ExternalInterface.fileWriteString(tempFile, jsCode)
        ExternalInterface.makeExecutable(tempFile)
        val out = ExternalInterface.exec("deno", "run", "-A", "--unstable", tempFile, *extraArgs.map { it.toString() }.toTypedArray())
        if (out.exitCode != 0) {
            error("ERROR: $out")
        }
        return out.stdout
    }

    fun genFilesJSString(
        @Language("kotlin")
        vararg filesContent: String
    ): String = genFiles(*filesContent).indenter.indentToString()

    fun genFiles(
        @Language("kotlin")
        vararg filesContent: String
    ): JSCodegen {
        return ValentiaCompiler.compile(filesContent.map { FileWithContents(it) }).codegen
        /*
        val gen = JSCodegen()
        val program = Program()
        val module = program.getModule(null)
        for (content in filesContent) {
            module.addFile(ValentiaParser.file("$content\nexternal val console: dynamic\n"))
        }
        gen.generateProgram(program)
        return gen
        */
    }

    private fun testFile(file: String, printJs: Boolean = true) {
        val res = ExternalInterface.fileReadString(file)
        val filesContents = res.split("//////\n")
        val result = Regex("^//(\\s*)expected:(.*)").find(res) ?: error("Can't find // expected")
        val expected = result.groupValues.last()
        assertEquals(
            expected,
            genAndRunJs(*filesContents.toTypedArray(), printJs = printJs)
        )
    }
}