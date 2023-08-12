package valentia.parser

import valentia.ast.StmBuilder
import kotlin.test.Test

class ValentiaParserFullExamplesTest : StmBuilder {
    val DOLLAR = "\$"

    @Test
    fun test() {
        ValentiaParser.file("""
            package valentia.parser

            import java.io.File
            import kotlin.test.Test

            class ParseValentiaSrc {
                @Test
                fun test() {
                    val files = File(".").walkBottomUp().filter { it.extension == "kt" }
                    for (file in files) {
                        println("FILE: ${DOLLAR}file : ")
                        val text = file.readText()
                        val nodes = ValentiaParser(text).valentiaFile()
                        println("   -> topLevelDecls=${DOLLAR}{nodes.topLevelDecls.size}")
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun test2() {
        ValentiaParser.file("""
            package kotlins.parser

            import org.antlr.v4.runtime.*
            import kotlin.test.Test

            class KotlinParserTest {
                @Test
                fun test() {
                    val listener = object : KotlinParserBaseListener() {
                        override fun enterPackageHeader(ctx: KotlinParser.PackageHeaderContext?) {
                            super.enterPackageHeader(ctx)
                        }
                    }
                    val stream = CharStreams.fromString(
                        // language=kotlin
                        ""${'"'}
                            package test

                            import a.b

                            fun test() {                
                            }

                        ""${'"'}.trimIndent()
                    )
                    val ctx = KotlinParser(CommonTokenStream(KotlinLexer(stream))).kotlinFile()
                    listener.enterKotlinFile(ctx)
                    println(ctx.packageHeader())
                }
            }
        """.trimIndent())
    }

    @Test
    fun test3() {
        ValentiaParser.file("""
            package valentia.util

            fun Char.isLetterOrUndescore(): Boolean = isLetter() || this == '_'
            fun Char.isLetterOrDigitOrUndescore(): Boolean = isLetter() || isDigit() || this == '_'
            fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'
        """.trimIndent())
    }

    @Test
    fun test4() {
        ValentiaParser.file("""
            package valentia.util

            fun <T> whileMap(cond: (Int) -> Boolean, gen: () -> T): List<T> {
            //fun <T> whileMap(cond: Int, gen: Int): List<T> {
                val out = arrayListOf<T>()
                while (cond(out.size)) out += gen()
                return out
            }
        """.trimIndent())
    }

    @Test
    fun test5() {
        ValentiaParser.file("""
            package valentia.util

            interface Indenter {
                class Impl : Indenter {
                    private var level: Int = 0
                    private val lines: ArrayList<Line> = arrayListOf()

                    override fun line(str: String): Line {
                        return Line(level, str).also { lines += it }
                    }

                    override fun indent() {
                        level++
                    }

                    override fun unindent() {
                        --level
                    }

                    override fun indentToString(): String = lines.joinToString("\n")

                    override fun toString(): String = indentToString()
                }

                companion object {
                    @PublishedApi internal val INDENT_LEVELS = Array(128) { "  ".repeat(it) }
                    operator fun invoke(): Indenter = Impl()
                }

                data class Line(val indentLevel: Int, var str: String) {
                    override fun toString(): String = "${DOLLAR}{INDENT_LEVELS[indentLevel]}${DOLLAR}str"
                }

                fun line(str: String): Line
                fun indent()
                fun unindent()

                fun line(str: String, suffix: String = " {", newline: String = "}", block: () -> Unit): Line {
                    line("${DOLLAR}str${DOLLAR}suffix")
                    indent {
                        block()
                    }
                    return line(newline)
                }

                fun indentToString(): String

                //override fun toString(): String = indentToString()
            }


            inline fun <T> Indenter.indent(block: () -> T): T {
                indent()
                try {
                    return block()
                } finally {
                    unindent()
                }
            }
        """.trimIndent())
    }
}