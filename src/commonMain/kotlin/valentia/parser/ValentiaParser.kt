package valentia.parser

import org.intellij.lang.annotations.Language
import valentia.ast.*

open class ValentiaParser(
    tokens: List<Token>
) : KotlinParser(tokens) {
    constructor(str: String) : this(ValentiaTokenizer(str).tokenize())

    fun valentiaFile(): FileNode = kotlinFile()

    companion object {
        private fun <T> parserEOF(str: String, block: ValentiaParser.() -> T): T {
            return ValentiaParser(str).let { parser -> block(parser).also { parser.EOF() } }
        }

        fun topLevelDecl(@Language("kotlin") str: String): Decl =
            parserEOF(str) { topLevelObject() }
        fun file(@Language("kotlin") str: String): FileNode =
            parserEOF(str) { valentiaFile() }
        fun expression(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Expr =
            parserEOF(str) { expression() }
        fun assignment(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            parserEOF(str) { assignment() }
        fun statement(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            parserEOF(str) { statement() }
        fun statements(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): List<Stm> =
            parserEOF(str) { statements() }
        fun type(@Language("kotlin", prefix = "typealias Example = ", suffix = "") str: String): TypeNode =
            parserEOF(str) { type() }

    }
}
