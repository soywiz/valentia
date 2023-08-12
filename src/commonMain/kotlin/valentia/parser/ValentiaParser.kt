package valentia.parser

import org.intellij.lang.annotations.Language
import valentia.ast.*

open class ValentiaParser(
    tokens: List<Token>
) : KotlinParser(tokens) {
    constructor(str: String) : this(ValentiaTokenizer(str).tokenize())

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
            ValentiaParser(str).let { parser -> parser.statement().also { parser.EOF() } }
        fun statements(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): List<Stm> =
            ValentiaParser(str).statements()
        fun type(@Language("kotlin", prefix = "typealias Example = ", suffix = "") str: String): TypeNode =
            ValentiaParser(str).type()

    }
}