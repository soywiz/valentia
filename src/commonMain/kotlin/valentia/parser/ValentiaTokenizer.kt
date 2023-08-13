package valentia.parser

import valentia.util.isLetterOrDigitOrUndescore
import valentia.util.isLetterOrUndescore
import valentia.util.whileMap

class ValentiaTokenizer(str: String) : StrReader(str), BaseParser {
    var currentLine = 1

    fun tokenize(): List<Token> {
        val out = arrayListOf<Token>()
        while (hasMore) {
            out += readToken()
        }
        return out
    }

    fun String.countLineBreaks(): Int {
        // @TODO: \r or \r\n sequences should match too
        return this.count { it == '\n' }
    }

    fun readToken(): Token {
        val line = currentLine
        return when (val c = peekChar()) {
            ' ', '\t', '\r', '\n', '\u000c' -> {
                val res = readWhile { it == ' ' || it == '\t' || it == '\r' || it == '\n' || it == '\u000c' }
                val nlines = res.countLineBreaks()
                currentLine += nlines
                (if (nlines >= 1) NLToken(res, nlines) else SpacesToken(res))
            }
            in '0'..'9' -> {
                //while (hasMore) {
                //    val c = peekChar()
                //    when {
                //        c == 'e' || c == 'E' -> {
                //        }
                //        c.isLetterOrDigitOrUndescore() -> {
                //        }
                //    }
                //}
                val baseNum = readWhile { it.isLetterOrDigitOrUndescore() }
                //println(" || ${peekChar()}")
                val spos = pos
                val extra = if (matches(".")) {
                    opt {
                        expect(".")
                        if (peekChar().isDigit()) {
                            "." + readWhile { it.isLetterOrDigitOrUndescore() }
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
                if (extra == null) {
                    pos = spos
                }
                //println("N=${"$baseNum$extra"}")
                //println(" -> ${peekChar()}")
                NumberToken("$baseNum${extra ?: ""}")
            }
            in 'a'..'z', in 'A'..'Z', '_' -> {
                if (expectOpt("as?")) {
                    return IDToken("as?")
                }
                IDToken(readWhile { it.isLetterOrDigitOrUndescore() })
            }
            '`' -> {
                val c = readChar()
                IDToken("$c" + readUntil {
                    if (it == '\r' || it == '\n') error("Invalid token")
                    it == '`'
                })
            }
            '\'' -> {
                val spos = pos
                val tokens = expectAndRecoverSure("'", "'") {
                    whileMap({ hasMore && peekChar() != '\'' }) { readStringToken('\'') }
                }
                val str = readAbsoluteRange(spos, pos)

                when (val token = tokens.firstOrNull()) {
                    null -> CharLiteralToken(str, '\u0000')
                    is LiteralStringPartToken -> CharLiteralToken(str, token.string.first())
                    is EscapeStringPartToken -> CharLiteralToken(str, token.c)
                    is ExpressionStringPartToken -> TODO()
                }
            }
            '"' -> {
                val spos = pos
                val (triple, tokens: List<StringPartToken>) = if (matches("\"\"\"")) {
                    true to expectAndRecoverSure("\"\"\"", "\"\"\"") {
                        whileMap({ hasMore && !matches("\"\"\"") }) { readStringTripleToken() }
                    }
                } else {
                    false to expectAndRecoverSure("\"", "\"") {
                        whileMap({ hasMore && peekChar() != '"' }) { readStringToken('"') }
                    }
                }
                StringInterpolationToken(readAbsoluteRange(spos, pos), tokens, if (triple) "\"\"\"" else "\"")
            }
            '?' -> SymbolToken(expectAny("?:", "?.", "?"))
            '&' -> SymbolToken(expectAny("&&", "&"))
            '|' -> SymbolToken(expectAny("||", "|"))
            '^' -> SymbolToken(expectAny("^"))
            '=' -> SymbolToken(expectAny("===", "==", "=>", "="))
            '!' -> {
                if (!peekChar(3).isLetterOrDigitOrUndescore()) {
                    if (expectOpt("!in")) return SymbolToken("!in")
                    if (expectOpt("!is")) return SymbolToken("!is")
                }
                SymbolToken(expectAny("!==", "!=", "!!", "!"))
            }
            '~' -> SymbolToken(expectAny("~"))
            //'.' -> SymbolToken(expectAny(".*", "..>", "..", "."))
            '.' -> SymbolToken(expectAny("..>", "..", "."))
            '@' -> SymbolToken(expectAny("@"))
            '#' -> {
                if (matches("#!")) {
                    ShebangToken(readUntil { it == '\r' || it == '\n' })
                } else {
                    SymbolToken(expectAny("#"))
                }
            }
            //'<' -> SymbolToken(expectAny("<<<", "<<=", "<=", "<<", "<"))
            //'>' -> SymbolToken(expectAny(">>>", ">>=", ">=", ">>", ">"))
            '<' -> SymbolToken(expectAny("<<=", "<=", "<"))
            '>' -> SymbolToken(expectAny(">>=", ">=", ">"))
            '-' -> SymbolToken(expectAny("-=", "->", "--", "-"))
            '+' -> SymbolToken(expectAny("+=", "++", "+"))
            '*' -> SymbolToken(expectAny("*=", "*"))
            '%' -> SymbolToken(expectAny("%=", "%"))
            ',' -> SymbolToken(expectAny(","))
            ';' -> SymbolToken(expectAny(";"))
            ':' -> SymbolToken(expectAny("::", ":"))
            '[' -> SymbolToken(expectAny("["))
            ']' -> SymbolToken(expectAny("]"))
            '(' -> SymbolToken(expectAny("("))
            ')' -> SymbolToken(expectAny(")"))
            '{' -> SymbolToken(expectAny("{"))
            '}' -> SymbolToken(expectAny("}"))
            '/' -> {
                val spos = pos
                if (matches("//")) {
                    CommentToken(readUntil { it == '\n' || it == '\r' })
                } else if (matches("/*")) {
                    var open = 0
                    while (hasMore) {
                        if (expectOpt("/*")) {
                            open++
                        }
                        if (expectOpt("*/")) {
                            open--
                            if (open == 0) break
                        }
                        skip()
                    }
                    CommentToken(readAbsoluteRange(spos, pos).also {
                        currentLine += it.countLineBreaks()
                    })
                } else {
                    SymbolToken(expectAny("/=", "/"))
                }
            }
            else -> TODO("unknown '$c'")
        }.also { it.line = line }
    }

    fun readStringTripleToken(): StringPartToken {
        val line = currentLine
        return when (peekChar()) {
            '\$' -> {
                val spos = pos
                expect("\$")
                val tokens: List<Token> = when {
                    matches("{") -> expectAndRecoverSure("{", "}") { whileMap({ !matches("}") }) { readToken() } }
                    peekChar().isLetterOrUndescore() -> listOf(readToken())
                    else -> TODO("Unsupported after dollar string $this")
                }
                ExpressionStringPartToken(readAbsoluteRange(spos, pos), tokens)
            }
            else -> {
                LiteralStringPartToken(readUntil { it == '\$' || matches("\"\"\"") }.also {
                    currentLine += it.countLineBreaks()
                })
            }
        }.also { it.line = line }
    }

    fun readEscapeSequence(): EscapeStringPartToken {
        val spos = pos
        expect("\\")
        val c = readChar()
        val rc = when (c) {
            'u', 'U' -> {
                val digits = read(4)
                digits.toInt(16).toChar()
            }
            '\\', '"', '\'' -> c
            't' -> '\t'
            'r' -> '\r'
            'n' -> '\n'
            'b' -> '\b'
            else -> c
        }
        return EscapeStringPartToken(readAbsoluteRange(spos, pos), rc).also { it.line = currentLine }
    }

    fun readStringToken(end: Char): StringPartToken {
        val line = currentLine
        return when (peekChar()) {
            end -> unexpected()
            '\$' -> {
                val spos = pos
                expect("\$")
                val tokens: List<Token> = when {
                    matches("{") -> expectAndRecoverSure("{", "}") { whileMap({ !matches("}") }) { readToken() } }
                    peekChar().isLetterOrUndescore() -> listOf(readToken())
                    else -> TODO("Unsupported after dollar string $this")
                }
                ExpressionStringPartToken(readAbsoluteRange(spos, pos), tokens)
            }
            '\\' -> {
                readEscapeSequence()
            }
            else -> {
                LiteralStringPartToken(readUntil { it == '\$' || it == '\\' || it == end }.also {
                    currentLine += it.countLineBreaks()
                })
            }
        }.also { it.line = line }
    }

    override fun reportError(e: Throwable) {
        TODO("Not yet implemented")
    }
}

sealed class Token(val str: String) {
    var line = -1
    //override fun toString(): String = str
}
object EOFToken : Token("")
abstract class HiddenToken(str: String) : Token(str)
data class CommentToken(val comment: String) : HiddenToken(comment)
data class SpacesToken(val spaces: String) : HiddenToken(spaces)
data class NLToken(val spaces: String, val nlines: Int) : Token(spaces)
data class SymbolToken(val symbol: String) : Token(symbol)
data class ShebangToken(val shebang: String) : Token(shebang)
data class NumberToken(val number: String) : Token(number) {
    val numberCleanedUp: String by lazy {
        number.replace("_", "").replace(Regex("\\D+\$"), "")
    }
    val isFloat: Boolean get() = number.endsWith('f') || number.endsWith('F')
    val isLong: Boolean get() = number.endsWith('l') || number.endsWith('L')
    val isDecimal: Boolean get() = number.contains('.') || number.contains('e') || number.contains('E')
    val isUnsigned: Boolean get() = number.contains('u') || number.contains('U')

    val value: Number by lazy {
        when {
            numberCleanedUp.startsWith("0x") -> numberCleanedUp.substring(2).toLong(16)
            numberCleanedUp.startsWith("0o") -> numberCleanedUp.substring(2).toLong(8)
            numberCleanedUp.startsWith("0b") -> numberCleanedUp.substring(2).toLong(2)
            else -> {
                if (isDecimal) {
                    numberCleanedUp.toDoubleOrNull()
                } else {
                    numberCleanedUp.toLongOrNull()
                }
            }
        } ?: error("Number '$number' : '$numberCleanedUp'")
    }
}
data class IDToken(val id: String) : Token(id)

data class CharLiteralToken(val string: String, val char: Char) : Token(string)
data class StringInterpolationToken(val string: String, val tokens: List<StringPartToken>, val type: String) : Token(string)

sealed class StringPartToken(str: String) : Token(str)
data class ExpressionStringPartToken(val string: String, val expr: List<Token>) : StringPartToken(string)
data class LiteralStringPartToken(val string: String) : StringPartToken(string)
data class EscapeStringPartToken(val string: String, val c: Char) : StringPartToken(string)
