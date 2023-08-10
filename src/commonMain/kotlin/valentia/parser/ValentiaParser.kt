package valentia.parser

import org.intellij.lang.annotations.Language
import valentia.ast.*

open class ValentiaParser(
    str: String
) : StrReader(str), KotlinParser {
    fun valentiaFile(): FileNode = kotlinFile()

    companion object {
        fun topLevelDecl(@Language("kotlin") str: String): DeclNode = ValentiaParser(str).topLevelObject()
        fun file(@Language("kotlin") str: String): FileNode = ValentiaParser(str).valentiaFile()
        fun expression(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Expr =
            ValentiaParser(str).expression()
        fun statement(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            ValentiaParser(str).statement()
        fun statements(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): List<Stm> =
            ValentiaParser(str).statements()
        fun type(@Language("kotlin", prefix = "typealias Example = ", suffix = "") str: String): TypeNode =
            ValentiaParser(str).type()

    }
}