package valentia.gen

import org.intellij.lang.annotations.Language
import valentia.ExternalInterface
import valentia.compiler.FileWithContents
import valentia.compiler.ValentiaCompiler
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class JSCodegenTest {
    @Test
    fun testPropertyWithReceiver() {
        assertEquals(
            "42",
            genAndRunJs("""
                external class Int
                external val console: dynamic
                
                val Int.twice: Int get() = this * 2
                fun main() {
                    console.log(21.twice)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testFuncParam() {
        assertEquals(
            "7",
            genAndRunJs("""
                external class Int
                external val console: dynamic
                
                fun Int.sample(): Int = this - 2
                fun test(demo: Int): Int {
                    return demo.sample()
                }
                fun main() {
                    console.log(test(9))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testFuncGeneric() {
        assertEquals(
            "7",
            genAndRunJs("""
                external val console: dynamic
                
                class Demo(val a: Int) {
                    fun a(): Int = a
                }
                inline fun <reified T : Demo> test(demo: T): Int {
                    //console.log(T::class)
                    return demo.a()
                }
                fun main() {
                    console.log(test(Demo(7)))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testIfTernary() {
        assertEquals(
            "10\na\nb",
            genAndRunJs("""
                external val console: dynamic
                fun main() {
                    val a = 10
                    val b = if (a == 10) "a" else "b"
                    val c = if (a != 10) "a" else "b"
                    console.log(a)
                    console.log(b)
                    console.log(c)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testSimpleReceiver() {
        assertEquals(
            "9",
            genAndRunJs("""
                external val console: dynamic
                external class Int
                fun Int.squared(): Int = this * this                 

                fun main() {
                    console.log(3.squared())
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testOperatorOverloading() {
        assertEquals(
            "ab",
            genAndRunJs("""
                external val console: dynamic
                class Demo(val a: String) {
                    operator fun plus(other: Demo): String = a + other.a
                }                

                fun main() {
                    console.log(Demo("a") + Demo("b"))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testGetClassInstanceFromType() {
        assertEquals(
            "[class mypackage\$Demo]",
            genAndRunJs("""
                package mypackage
                external val console: dynamic
                class Demo
                fun main() {
                    console.log(Demo::class)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testGetClassInstanceFromExpression() {
        assertEquals(
            "true",
            genAndRunJs("""
                external val console: dynamic
                class Demo
                val demo = Demo()
                fun main() {
                    console.log(demo::class == Demo::class)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testIterator() {
        assertEquals(
            "0\n2\n4\n6\n8\n10",
            genAndRunJs("""
                external val console: dynamic
                external fun println(v: Any?)
                external class NoSuchElementException
                
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
                        if (value >= finalElement) {
                            if (!hasNext) throw NoSuchElementException()
                            hasNext = false
                        } else {
                            next += step
                        }
                        return value
                    }
                }
                fun main() {
                    for (n in IntProgressionIterator(0, 10, +2)) {
                        console.log(n)
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
                external val console: dynamic
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
                external val console: dynamic
                class Demo { fun a() = 7 }
                fun main() = console.log(Demo().a())
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    @Ignore
    fun testClassInheritance() {
        assertEquals(
            "8\n9",
            genAndRunJs("""
                external val console: dynamic
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
    fun testSimpleForLoop() {
        assertEquals("0\n1\n2\n3", genAndRunJs("external val console: dynamic\nfun main() { for (n in 0 .. 3) console.log(n) }", printJs = true))
        assertEquals("0\n1\n2", genAndRunJs("external val console: dynamic\nfun main() { for (n in 0 ..< 3) console.log(n) }", printJs = true))
        assertEquals("0\n1\n2", genAndRunJs("external val console: dynamic\nfun main() { for (n in 0 until 3) console.log(n) }", printJs = true))
        assertEquals("3\n2\n1\n0", genAndRunJs("external val console: dynamic\nfun main() { for (n in 3 downTo 0) console.log(n) }", printJs = true))
    }

    @Test
    fun testForLoopEvaluatesOnce() {
        assertEquals(
            "0\n1\n2\n3\n1 1",
            genAndRunJs("""
                external val console: dynamic
                var count1 = 0
                var count2 = 0
                fun func1(): Int {
                    count1++
                    return 0
                }
                fun func2(): Int {
                    count2++
                    return 3
                }
    
                fun main() { 
                    for (n in func1() .. func2()) console.log(n)
                    console.log(count1, count2)
                }
            """, printJs = true)
        )
    }

    @Test
    fun testLabelledBreak() {
        // @TODO: This doesn't parse
        //assertEquals("0\n1\n2\n3", genAndRunJs("fun main() { level2@for (m in 0 .. 4) for (n in 0 .. m) { if (n == 3) break@level2; console.log(m, n) } }", printJs = true))
        assertEquals("""
            0 0
            1 0
            1 1
            2 0
            2 1
        """.trimIndent(), genAndRunJs("""
            external val console: dynamic
            fun main() {
                level2@for (m in 0 .. 4) {
                    for (n in 0 .. m) {
                        if (n == 2) break@level2
                        console.log(m, n)
                    }
                }
            }
        """, printJs = true))
    }

    @Test
    fun testIntArray() {
        // @TODO: This doesn't parse
        //assertEquals("0\n1\n2\n3", genAndRunJs("fun main() { level2@for (m in 0 .. 4) for (n in 0 .. m) { if (n == 3) break@level2; console.log(m, n) } }", printJs = true))
        assertEquals("""
            1
            12
            123
        """.trimIndent(), genAndRunJs("""
            external val console: dynamic
            @JsName("Int32Array")
            @JsBody("Int32Array.prototype.hashCode = () => 0;")
            external class IntArray {
                constructor(size: Int)
            }
            fun main() {
                val array = IntArray(10)
                array[0] = 1
                array[1] = 12
                array[2] = 123
                console.log(array[0])
                console.log(array[1])
                console.log(array[2])
            }
        """, printJs = true))
    }

    @Test
    fun testWhileLoop() {
        assertEquals("1\n2\n3\n4", genAndRunJs("external val console: dynamic\nfun main() { var n = 0; while (n++ < 4) console.log(n) }", printJs = true))
        assertEquals("1\n2\n3", genAndRunJs("external val console: dynamic\nfun main() { var n = 0; while (++n < 4) console.log(n) }", printJs = true))
    }

    @Test
    fun testDoWhileLoop() {

        assertEquals("0\n1\n2\n3\n4", genAndRunJs("external val console: dynamic\nfun main() { var n = 0; do console.log(n) while (n++ < 4) }", printJs = true))
        assertEquals("0\n1\n2\n3", genAndRunJs("external val console: dynamic\nfun main() { var n = 0; do console.log(n) while (++n < 4) }", printJs = true))
    }

    //val RESOURCES_PREFIX = "src/commonTest/resources"
    val RESOURCES_PREFIX = "testresources"

    @Test
    fun testClassInstantiationValGetter() =
        testFile("$RESOURCES_PREFIX/cases/testClassInstantiationValGetter.kt.txt")

    @Test
    fun testClassMultipleConstructor() =
        testFile("$RESOURCES_PREFIX/cases/testClassMultipleConstructor.kt.txt")

    @Test
    fun testClassMultipleConstructor2() {
        assertEquals(
            "10",
            genAndRunJs("""
                external val console: dynamic
        
                class Demo(val a: String) {
                    constructor(a: Int) : this("${'$'}{a * 2}")
                }
            """.trimIndent(), """
                fun main() = console.log(Demo(5).a)
            """.trimIndent(), printJs = true)
        )
    }

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
                external val console: dynamic
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
                external val console: dynamic
                fun min(a: Int, b: Int): Int { return if (a < b) a else b }
                fun main() {
                    console.log(min(1, 2))
                    console.log(min(+4, -3))
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testMultipleInitBlock() {
        assertEquals(
            "27",
            genAndRunJs("""
                external val console: dynamic
                class Demo() {
                    var a = 1
                    init { this.a += 3 }
                    init { this.a *= 7 }
                    init { this.a -= 1 }
                }
                fun main() {
                    console.log(Demo().a)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    @Ignore
    fun testMultipleInitBlock2() {
        assertEquals(
            "27",
            genAndRunJs("""
                class Demo {
                    var a = 1
                    init { a += 3 }
                    init { a *= 7 }
                    init { a -= 1 }
                }
                fun main() {
                    console.log(Demo().a)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testIntegerOps() {
        assertEquals(
            "-1073741824\n2\n949312677\n1018518717\n69206040\n2\n44\n-1\n1073741823",
            genAndRunJs("""
                external val console: dynamic
                fun main() {
                    console.log(2147483648 + 1073741824)
                    console.log(-2147483647 + -2147483647)
                    console.log(213212312 xor 874512445)
                    console.log(213212312 or 874512445)
                    console.log(213212312 and 874512445)
                    console.log(1 shl 1)
                    console.log(179 shr 2)
                    console.log((-1) shr 2)
                    console.log((-1) ushr 2)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testBoolOps() {
        assertEquals(
            "false\ntrue\ntrue",
            genAndRunJs("""
                external val console: dynamic
                fun main() {
                    console.log(true && false)
                    console.log(true || false)
                    console.log(true xor false)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testSimpleGen() {
        assertEquals(
            "12",
            genAndRunJs("""
                external val console: dynamic
                fun sum(a: Int, b: Int): Int { return a + b }
                fun main() {
                    console.log(sum(1, 3) * 3)
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testFuncArgument() {
        assertEquals(
            "22",
            genAndRunJs("""
                external val console: dynamic
                external class Int
                fun Int.intApply2(func: (Int) -> Int): Int = func(this) * 2
                fun main() {
                    //console.log(10.intApply2 { if (it > 10) it * 4 else it * 3 })
                    console.log(10.intApply2 { it + 1 })
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testFuncArgumentWithCapture() {
        assertEquals(
            "42",
            genAndRunJs("""
                external val console: dynamic
                external class Int
                fun main() {
                    var captured = 10
                    val func: (Int) -> Int = { it + 1 + captured }
                    captured = 20 // Should be ignored because of the capture
                    console.log(func(31) )
                }
            """.trimIndent(), printJs = true)
        )
    }

    @Test
    fun testGenericArraySimple() {
        assertEquals(
            "hello world",
            genAndRunJs("""
                external val console: dynamic
                external class Array<T>
                fun main() {
                    val array = Array<String>(10)
                    array[0] = "hello"
                    array[1] = "world"
                    console.log(array[0], array[1])
                }
            """.trimIndent(), printJs = true)
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