package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserFunTest : DeclBuilder, StmBuilder {
    @Test
    fun testFun() {
        assertEquals(
            STM(FUN("test", IntType, "a" to IntType, "b" to IntType) {
                RETURN(WHEN("a".id + "b".id) {
                    CASE(1.lit, "a".lit)
                    CASE(2.lit, 'b'.lit)
                })
            }),
            ValentiaParser.statement("""
                fun test(a: Int, b: Int): Int {
                    return when (a + b) {
                        1 -> "a"
                        2 -> 'b'
                    }
                }
            """)
        )
    }

    @Test
    fun testFunWithWhere() {
        assertEquals(
            STM(FUN("test", IntType, "a" to "T".type, "b" to "T2".type) {
                RETURN(1.lit)
            }.copy(where = listOf(TypeConstraint("T", IntType), TypeConstraint("Int", IntType)))),
            ValentiaParser.statement("""
                fun <reified T : Int, T2 : String> test(a:T, b: T2): Int where T : Int, Int: Int {
                    return 1
                }
            """) as? Any?
        )
    }

    @Test
    fun testSuspendFunWithWhere() {
        assertEquals(
            FUN("demo") {
            }.copy(modifiers = Modifiers(FunctionModifier.SUSPEND)),
            ValentiaParser.topLevelDecl("suspend fun demo() { }") as? Any?
        )
    }
    @Test
    fun testFunArgs() {
        assertEquals(
            FUN("demo", null, "a" to IntType, "b" to StringType) {
            },
            ValentiaParser.topLevelDecl("fun demo(a: Int, b: String, ) { }") as? Any?
        )
    }

    @Test
    fun testFun2() {
        ValentiaParser.topLevelDecl("""
            private inline fun <T> parserEOF(str: String, checkEOF: Boolean = true, block: ValentiaParser.() -> T): T {
                return ValentiaParser(str).let { parser -> block(parser).also { if (checkEOF) parser.EOF() } }
            }
        """.trimIndent())
    }

    @Test
    fun testFun3() {
        ValentiaParser.file("""
            @Test
            fun testNode() {
                assertEquals("hello world", runJsCode("console.log('hello world')").trim())
            }
            fun genAndRunJs(vararg filesContent: String, extraArgs: List<Any> = emptyList(), trim: Boolean = true, printJs: Boolean = false): String {
                val jsCode = genFilesJSString(*filesContent)
                if (printJs) println(jsCode)
                return runJsCode(jsCode, *extraArgs.toTypedArray()).let { if (trim) it.trim() else it }
            }
            fun genFilesJSString(
                @Language("kotlin")
                vararg filesContent: String
            ): String = genFiles(*filesContent).indentToString()
        """.trimIndent())
    }
}