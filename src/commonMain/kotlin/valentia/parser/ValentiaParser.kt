package valentia.parser

import org.intellij.lang.annotations.Language
import valentia.ast.*

open class ValentiaParser(
    tokens: List<Token>
) : KotlinParser(tokens) {
    constructor(str: String) : this(ValentiaTokenizer(str).tokenize())

    fun valentiaFile(): FileNode = kotlinFile()

    companion object {
        private inline fun <T> parserEOF(str: String, checkEOF: Boolean = true, block: ValentiaParser.() -> T): T {
            return ValentiaParser(str).let { parser -> block(parser).also {
                if (checkEOF) parser.EOF("generated=$it")
            } }
        }

        fun topLevelDecl(@Language("kotlin") str: String): Decl =
            parserEOF(str) { topLevelObject() ?: error("Not a topLevelObject") }
        fun file(@Language("kotlin") str: String): FileNode =
            parserEOF(str) { valentiaFile() }
        fun expression(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String, checkEOF: Boolean = true): Expr =
            parserEOF(str, checkEOF) { expressionSure() }
        fun assignment(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            parserEOF(str) { assignment() ?: error("Not an assignment") }
        fun statement(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): Stm =
            parserEOF(str) { statement() }
        fun statements(@Language("kotlin", prefix = "fun test() {", suffix = "}") str: String): List<Stm> =
            parserEOF(str) { statements() }
        fun type(@Language("kotlin", prefix = "typealias Example = ", suffix = "") str: String): TypeNode =
            parserEOF(str) { type() ?: error("Expected type") }
    }
}
