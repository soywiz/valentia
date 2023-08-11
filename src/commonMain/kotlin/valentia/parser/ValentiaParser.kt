package valentia.parser

import org.intellij.lang.annotations.Language
import valentia.ast.*

open class ValentiaParser(
    str: String
) : StrReader(str), KotlinParser {
    override fun reportError(e: Throwable) {
        debug("reportError: PARSER ERROR: $e")
        e.printStackTrace()
    }

    fun valentiaFile(): FileNode = kotlinFile()

    companion object {
        fun topLevelDecl(@Language("kotlin") str: String): Decl =
            ValentiaParser(str).topLevelObject()
        fun file(@Language("kotlin") str: String): FileNode =
            ValentiaParser(str).valentiaFile()
        fun expression(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Expr =
            ValentiaParser(str).expression()
        fun assignment(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            ValentiaParser(str).assignment()
        fun statement(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            ValentiaParser(str).statement()
        fun statements(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): List<Stm> =
            ValentiaParser(str).statements()
        fun type(@Language("kotlin", prefix = "typealias Example = ", suffix = "") str: String): TypeNode =
            ValentiaParser(str).type()

    }
}