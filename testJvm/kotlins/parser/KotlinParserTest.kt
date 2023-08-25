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
            """
                package test

                import a.b

                fun test() {                
                }

            """.trimIndent()
        )
        val ctx = KotlinParser(CommonTokenStream(KotlinLexer(stream))).kotlinFile()
        listener.enterKotlinFile(ctx)
        println(ctx.packageHeader())
    }
}