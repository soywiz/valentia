package valentia.parser

import valentia.ast.StmBuilder
import kotlin.test.Test

class ValentiaParserFullExamplesTest : StmBuilder {
    val DOLLAR = "\$"

    @Test
    fun test1() {
        ValentiaParser.file("""
            package valentia.parser
            
            import java.io.File
            import kotlin.test.Test
            import kotlin.time.measureTime
            import kotlin.time.measureTimedValue
            
            class ParseValentiaSrc {
                @Test
                fun test() {
                    val files = File(".").walkBottomUp().filter { it.extension == "kt" }
                    for (file in files) {
                        println("FILE: ${DOLLAR}file : ")
                        val text = file.readText()
                        val (tokens, timeTokenize) = measureTimedValue { ValentiaTokenizer(text).tokenize() }
                        val (nodes, timeParse) = measureTimedValue { ValentiaParser(tokens).valentiaFile() }
                        println("   -> tokenize=${DOLLAR}timeTokenize, parse=${DOLLAR}timeParse")
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

                override fun toString(): String = indentToString()
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

    @Test
    fun test5a() {
        ValentiaParser.file("""
            package valentia.util

            interface Indenter {
                fun line(str: String, suffix: String = " {", newline: String = "}", block: () -> Unit): Line
            }
        """.trimIndent())
    }

    @Test
    fun test5b() {
        ValentiaParser.file("""
            package valentia.util

            interface Indenter {
                companion object {
                    @PublishedApi internal val INDENT_LEVELS = Array(128) { "  ".repeat(it) }
                    operator fun invoke(): Indenter = Impl()
                }
                
                data class Line(val indentLevel: Int, var str: String) {
                    override fun toString(): String = "${DOLLAR}{INDENT_LEVELS[indentLevel]}${DOLLAR}str"
                }

                fun unindent()
                fun line(str: String, suffix: String = " {", newline: String = "}", block: () -> Unit): Line {
                    line("${DOLLAR}str${DOLLAR}suffix")
                    indent {
                        block()
                    }
                    return line(newline)
                }
                fun unindent2()
            }
        """.trimIndent())
    }

    @Test
    fun test6() {
        ValentiaParser.file("""
            package valentia.sema
            
            import valentia.ast.Decl
            
            fun interface SymbolProvider {
                operator fun get(name: String): List<Decl>?
            }
            
            object EmptySymbolProvider : SymbolProvider {
                override fun get(name: String): List<Decl>? = null
            }
            
            data class ComposedSymbolProvider(val current: SymbolProvider, val parent: SymbolProvider) : SymbolProvider {
                override fun get(name: String): List<Decl>? = current[name] ?: parent[name]
            }
            
            operator fun SymbolProvider.plus(other: SymbolProvider): SymbolProvider = 
                ComposedSymbolProvider(other, this)
            
            class SymbolContainer(
                val symbolsByName: LinkedHashMap<String, ArrayList<Decl>>
            ) : SymbolProvider {
                override fun get(name: String): List<Decl> = symbolsByName.getOrPut(name) { arrayListOf() }
            }
            
            interface ResolutionContext {
            }
            
            object DummyResolutionContext : ResolutionContext {
            }
        """.trimIndent())
    }

    @Test
    fun test7() {
        ValentiaParser.file("""
            package valentia.sema

            import valentia.ast.Decl
            import valentia.ast.FileNode
            import valentia.ast.Identifier

            class Package(val identifier: Identifier?) {
                val symbols = LinkedHashMap<String, ArrayList<Decl>>()
                val files = arrayListOf<FileNode>()

                private fun getSymbolsByName(name: String): ArrayList<Decl> = symbols.getOrPut(name) { arrayListOf() }

                fun addFile(file: FileNode) {
                    files += file
                    file.symbolsByName.map { (name, decls) ->
                        getSymbolsByName(name).addAll(decls)
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun test8() {
        ValentiaParser.file("""
            package valentia.gen

            import valentia.ast.*
            import valentia.sema.*

            open class Codegen {
                open fun supportedNode(node: Node): Boolean {
                    return true
                }

                open fun generateProgram(program: Program) {
                    for (module in program.modulesById.values) {
                        generateModule(module)
                    }
                }
                open fun generateModule(module: Module) {
                    for (file in module.filesByPackage.values) {
                        generatePackage(file)
                    }
                }

                open fun generatePackage(pack: Package) {
                    for (file in pack.files) {
                        generateFile(file)
                    }
                }
                
                var symbolProvider: SymbolProvider = EmptySymbolProvider

                fun <T> pushSymbolProvider(symbolProvider: SymbolProvider, block: () -> T): T {
                    val parent = this.symbolProvider
                    try {
                        this.symbolProvider = symbolProvider + symbolProvider
                        return block()
                    } finally {
                        this.symbolProvider = parent
                    }
                }

                open fun generateFile(file: FileNode) {
                    pushSymbolProvider(file.symbolProvider) {
                        for (decl in file.topLevelDecls) {
                            when (decl) {
                                is ClassDecl -> generateClass(decl)
                                is FunDecl -> generateFunction(decl)
                            }
                        }
                    }
                }

                open fun generateClass(clazz: ClassDecl) {
                }
                open fun generateFunction(func: FunDecl) {
                }
                open fun generateStm(stm: Stm) {
                }

                data class IdWithContext(val id: String, val context: SymbolProvider) {
                    fun resolve(): List<Decl>? = context[id]
                    fun resolve(funcType: TypeNode): Decl? {
                        val items = resolve() ?: return null
                        for (item in items) {
                            // @TODO: Compat
                            if (funcType == item.getType(DummyResolutionContext)) {
                                return item
                            }
                        }
                        return null
                    }
                    override fun toString(): String = id
                }

                open fun generateExpr(expr: Expr): Any {
                    return when (expr) {
                        is IdentifierExpr -> IdWithContext(expr.id, symbolProvider)
                        is BoolLiteralExpr -> "${DOLLAR}{expr.value}"
                        is IntLiteralExpr -> "${DOLLAR}{expr.value}"
                        is CharLiteralExpr -> "${DOLLAR}{expr.value.code}"
                        is StringLiteralExpr -> "\"${DOLLAR}{expr.value}\"" // @TODO: Escaping
                        is UnaryPostOpExpr -> {
                            val exprStr = generateExpr(expr.expr)
                            when (expr.op) {
                                UnaryPostOp.INCR -> "${DOLLAR}exprStr++"
                                UnaryPostOp.DECR -> "${DOLLAR}exprStr--"
                                UnaryPostOp.NOT_NULL -> exprStr
                            }
                        }
                        is UnaryPreOpExpr -> {
                            val exprStr = generateExpr(expr.expr)
                            when (expr.op) {
                                UnaryPreOp.MINUS -> "-(${DOLLAR}exprStr)"
                                else -> TODO("Unsupported ${DOLLAR}expr")
                            }
                        }
                        is OpSeparatedExprs -> {
                            val exprStrs = expr.exprs.map { generateExpr(it) }
                            exprStrs[0].toString() + " " + (0 until expr.ops.size).joinToString(" ") {
                                expr.ops[it] + " " + exprStrs[it + 1]
                            }
                        }
                        is CallExpr -> {
                            //val symbols = symbolProvider[expr.id]
                            //println("expr.id=${DOLLAR}{expr.id} : ${DOLLAR}symbols")

                            val res = generateExpr(expr.expr)
                            val paramsStr = "(" + expr.params.joinToString(", ") { generateExpr(it).toString() } + ")"
                            if (res is IdWithContext) {
                                val funcType = FuncTypeNode(UnknownType, expr.params.map { NamedTypeNode(it.getType(DummyResolutionContext)) })
                                val resolved = res.resolve(funcType) ?: error("Can't resolve ${DOLLAR}funcType")
                                println("RESOLVE: ${DOLLAR}funcType : ${DOLLAR}resolved")
                                resolved!!.jsName + paramsStr
                            } else {
                                res.toString() + paramsStr
                            }
                        }
                        is NavigationExpr -> {
                            if (expr.op != ".") error("Unsupported ${DOLLAR}{expr.op}")
                            val keyStr = when (expr.key) {
                                is Expr -> generateExpr(expr.key)
                                else -> expr.key.toString()
                            }
                            "${DOLLAR}{generateExpr(expr.expr)}.${DOLLAR}keyStr"
                        }
                        else -> TODO("generateExpr: ${DOLLAR}expr")
                    }
                }
            }
        """.trimIndent())
    }
    
    @Test
    fun test9() {
        ValentiaParser.file("""
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
                        ' ', '\t', '\r', '\n', '\u000c' -> {
                            val res = readWhile { it == ' ' || it == '\t' || it == '\r' || it == '\n' || it == '\u000c' }
                            return if (res.contains('\n') || res.contains('\r')) NLToken(res) else SpacesToken(res)
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
                            //println(" || ${DOLLAR}{peekChar()}")
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
                            //println("N=${"${DOLLAR}baseNum${DOLLAR}extra"}")
                            //println(" -> ${DOLLAR}{peekChar()}")
                            return NumberToken("${DOLLAR}baseNum${DOLLAR}{extra ?: ""}")
                        }
                        in 'a'..'z', in 'A'..'Z', '_' -> {
                            if (expectOpt("as?")) {
                                return IDToken("as?")
                            }
                            return IDToken(readWhile { it.isLetterOrDigitOrUndescore() })
                        }
                        '`' -> {
                            val c = readChar()
                            return IDToken("${DOLLAR}c" + readUntil { it == '`' })
                        }
                        '\'' -> {
                            val spos = pos
                            val tokens = expectAndRecoverSure("'", "'") {
                                whileMap({ hasMore && peekChar() != '\'' }) { readStringToken('\'') }
                            }
                            val str = readAbsoluteRange(spos, pos)

                            return when (val token = tokens.firstOrNull()) {
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
                            return StringInterpolationToken(readAbsoluteRange(spos, pos), tokens, if (triple) "\"\"\"" else "\"")
                        }
                        '?' -> return SymbolToken(expectAny("?:", "?.", "?"))
                        '&' -> return SymbolToken(expectAny("&&", "&"))
                        '|' -> return SymbolToken(expectAny("||", "|"))
                        '^' -> return SymbolToken(expectAny("^"))
                        '=' -> return SymbolToken(expectAny("===", "==", "=>", "="))
                        '!' -> {
                            if (!peekChar(3).isLetterOrDigitOrUndescore()) {
                                if (expectOpt("!in")) return SymbolToken("!in")
                                if (expectOpt("!is")) return SymbolToken("!is")
                            }
                            return SymbolToken(expectAny("!==", "!=", "!!", "!"))
                        }
                        '~' -> return SymbolToken(expectAny("~"))
                        //'.' -> return SymbolToken(expectAny(".*", "..>", "..", "."))
                        '.' -> return SymbolToken(expectAny("..>", "..", "."))
                        '@' -> return SymbolToken(expectAny("@"))
                        '#' -> {
                            if (matches("#!")) {
                                return ShebangToken(readUntil { it == '\r' || it == '\n' })
                            }
                            return SymbolToken(expectAny("#"))
                        }
                        //'<' -> return SymbolToken(expectAny("<<<", "<<=", "<=", "<<", "<"))
                        //'>' -> return SymbolToken(expectAny(">>>", ">>=", ">=", ">>", ">"))
                        '<' -> return SymbolToken(expectAny("<<=", "<=", "<"))
                        '>' -> return SymbolToken(expectAny(">>=", ">=", ">"))
                        '-' -> return SymbolToken(expectAny("-=", "->", "--", "-"))
                        '+' -> return SymbolToken(expectAny("+=", "++", "+"))
                        '*' -> return SymbolToken(expectAny("*=", "*"))
                        '%' -> return SymbolToken(expectAny("%=", "%"))
                        ',' -> return SymbolToken(expectAny(","))
                        ';' -> return SymbolToken(expectAny(";"))
                        ':' -> return SymbolToken(expectAny("::", ":"))
                        '[' -> return SymbolToken(expectAny("["))
                        ']' -> return SymbolToken(expectAny("]"))
                        '(' -> return SymbolToken(expectAny("("))
                        ')' -> return SymbolToken(expectAny(")"))
                        '{' -> return SymbolToken(expectAny("{"))
                        '}' -> return SymbolToken(expectAny("}"))
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
                        else -> TODO("unknown '${DOLLAR}c'")
                    }
                }

                fun readStringTripleToken(): StringPartToken {
                    return when (peekChar()) {
                        '\${'$'}' -> {
                            val spos = pos
                            expect("\${'$'}")
                            val tokens: List<Token> = when {
                                matches("{") -> expectAndRecoverSure("{", "}") { whileMap({ !matches("}") }) { readToken() } }
                                peekChar().isLetterOrUndescore() -> listOf(readToken())
                                else -> TODO("Unsupported after dollar string $this")
                            }
                            ExpressionStringPartToken(readAbsoluteRange(spos, pos), tokens)
                        }
                        else -> {
                            LiteralStringPartToken(readUntil { it == '\${'$'}' || matches("\"\"\"") })
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
                        '\${'$'}' -> {
                            val spos = pos
                            expect("\${'$'}")
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
                            LiteralStringPartToken(readUntil { it == '\${'$'}' || it == '\\' || it == end })
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
            abstract class HiddenToken(str: String) : Token(str)
            data class CommentToken(val comment: String) : HiddenToken(comment)
            data class SpacesToken(val spaces: String) : HiddenToken(spaces)
            data class NLToken(val spaces: String) : Token(spaces)
            data class SymbolToken(val symbol: String) : Token(symbol)
            data class ShebangToken(val shebang: String) : Token(shebang)
            data class NumberToken(val number: String) : Token(number) {
                val numberCleanedUp by lazy 10
                val numberCleanedUp: String by lazy {
                    number.replace("_", "").replace(Regex("\\D+\${'$'}"), "")
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
                    } //?: error("Number '${DOLLAR}number' : '${DOLLAR}numberCleanedUp'")
                }
            }

            data class IDToken(val id: String) : Token(id)

            data class CharLiteralToken(val string: String, val char: Char) : Token(string)
            data class StringInterpolationToken(val string: String, val tokens: List<StringPartToken>, val type: String) : Token(string)

            sealed class StringPartToken(str: String) : Token(str)
            data class ExpressionStringPartToken(val string: String, val expr: List<Token>) : StringPartToken(string)
            data class LiteralStringPartToken(val string: String) : StringPartToken(string)
            data class EscapeStringPartToken(val string: String, val c: Char) : StringPartToken(string)

        """.trimIndent())
    }
}