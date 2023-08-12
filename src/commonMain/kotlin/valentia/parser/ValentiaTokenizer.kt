package valentia.parser

import valentia.util.isLetterOrDigitOrUndescore
import valentia.util.isLetterOrUndescore
import valentia.util.whileMap

class ValentiaTokenizer(str: String) : StrReader(str), BaseParser {
    fun tokenize(): List<Token> {
        val out = arrayListOf<Token>()
        while (hasMore) {
            out += readToken()
        }
        return out
    }

    fun readToken(): Token {
        when (val c = peekChar()) {
            ' ', '\t', '\r', '\n' -> {
                return SpacesToken(readWhile { it == ' ' || it == '\t' || it == '\r' || it == '\n' })
            }
            in '0'..'9' -> {
                return NumberToken(readWhile { it.isDigit() || it == '_' })
            }
            in 'a'..'z', in 'A'..'Z', '_' -> {
                return IDToken(readWhile { it.isLetterOrDigitOrUndescore() })
            }
            '`' -> {
                val c = readChar()
                return IDToken("$c" + readUntil { it == '`' })
            }
            '\'' -> {
                val spos = pos
                val tokens = expectAndRecoverSure("'", "'") {
                    whileMap({ hasMore && peekChar() != '"' }) { readStringToken('"') }
                }
                return StringInterpolationToken(readAbsoluteRange(spos, pos), tokens, '\'')
            }
            '"' -> {
                val spos = pos
                val tokens: List<StringPartToken> = if (matches("\"\"\"")) {
                    expectAndRecoverSure("\"\"\"", "\"\"\"") {
                        whileMap({ hasMore && !matches("\"\"\"") }) { readStringTripleToken() }
                    }
                } else {
                    expectAndRecoverSure("\"", "\"") {
                        whileMap({ hasMore && peekChar() != '"' }) { readStringToken('"') }
                    }
                }
                return StringInterpolationToken(readAbsoluteRange(spos, pos), tokens, '"')
            }
            '?' -> return SymbolToken(expectAny("?"))
            '&' -> return SymbolToken(expectAny("&&", "&"))
            '|' -> return SymbolToken(expectAny("||", "|"))
            '^' -> return SymbolToken(expectAny("^"))
            '=' -> return SymbolToken(expectAny("===", "==", "=>"))
            '!' -> return SymbolToken(expectAny("!==", "!=", "!!", "!"))
            '~' -> return SymbolToken(expectAny("~"))
            '.' -> return SymbolToken(expectAny("..>", "..", "."))
            '@' -> return SymbolToken(expectAny("@"))
            '#' -> return SymbolToken(expectAny("#"))
            '<' -> return SymbolToken(expectAny("<<<", "<<=", "<=", "<<", "<"))
            '>' -> return SymbolToken(expectAny(">>>", ">>=", ">=", ">>", ">"))
            '-' -> return SymbolToken(expectAny("->", "--", "-"))
            '+' -> return SymbolToken(expectAny("++", "+"))
            '*' -> return SymbolToken(expectAny("*=", "*"))
            '%' -> return SymbolToken(expectAny("%=", "%"))
            ';' -> return SymbolToken(expectAny(";"))
            ':' -> return SymbolToken(expectAny("::", ":"))
            '[' -> return SymbolToken(expectAny("["))
            ']' -> return SymbolToken(expectAny("]"))
            '(' -> return SymbolToken(expectAny("("))
            ')' -> return SymbolToken(expectAny(")"))
            '/' -> {
                val spos = pos
                if (matches("//")) {
                    return CommentToken(readUntil { it == '\n' || it == '\r' })
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
                    return CommentToken(readAbsoluteRange(spos, pos))
                }
                return SymbolToken(expectAny("/=", "/"))
            }
            else -> TODO("unknown '$c'")
        }
    }

    fun readStringTripleToken(): StringPartToken {
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
                LiteralStringPartToken(readUntil { it == '\$' || matches("\"\"\"") })
            }
        }
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
        return EscapeStringPartToken(readAbsoluteRange(spos, pos), rc)
    }

    fun readStringToken(end: Char): StringPartToken {
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
                LiteralStringPartToken(readUntil { it == '\$' || it == '\\' || it == end })
            }
        }
    }

    override fun reportError(e: Throwable) {
        TODO("Not yet implemented")
    }
}

sealed class Token(val str: String) {
    //override fun toString(): String = str
}
object EOFToken : Token("")
data class CommentToken(val comment: String) : Token(comment)
data class SpacesToken(val spaces: String) : Token(spaces)
data class SymbolToken(val symbol: String) : Token(symbol)
data class NumberToken(val number: String) : Token(number)
data class IDToken(val id: String) : Token(id)
data class StringInterpolationToken(val string: String, val tokens: List<StringPartToken>, val type: Char) : Token(string)

sealed class StringPartToken(str: String) : Token(str)
data class ExpressionStringPartToken(val string: String, val expr: List<Token>) : StringPartToken(string)
data class LiteralStringPartToken(val string: String) : StringPartToken(string)
data class EscapeStringPartToken(val string: String, val c: Char) : StringPartToken(string)
