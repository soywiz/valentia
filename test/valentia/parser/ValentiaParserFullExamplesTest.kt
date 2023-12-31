package valentia.parser

import valentia.ast.StmBuilder
import kotlin.test.Test

class ValentiaParserFullExamplesTest : StmBuilder {
    val DOLLAR = "\$"

    @Test
    fun test1() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test2() {
        ValentiaParser.file(
            """
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
                    //println(ctx.packageHeader())
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun test3a() {
        ValentiaParser.file(
            """
            fun Char.isLetterOrUndescore(): Boolean = true
        """.trimIndent()
        )
    }

    @Test
    fun test3b() {
        ValentiaParser.file(
            """
            fun Char?.isLetterOrUndescore(): Boolean = true
        """.trimIndent()
        )
    }

    @Test
    fun test3() {
        ValentiaParser.file(
            """
            package valentia.util

            fun Char.isLetterOrUndescore(): Boolean = isLetter() || this == '_'
            fun Char.isLetterOrDigitOrUndescore(): Boolean = isLetter() || isDigit() || this == '_'
            fun Char.isHexDigit(): Boolean = isDigit() || this in 'a'..'f' || this in 'A'..'F'
        """.trimIndent()
        )
    }

    @Test
    fun test4() {
        ValentiaParser.file(
            """
            package valentia.util

            fun <T> whileMap(cond: (Int) -> Boolean, gen: () -> T): List<T> {
            //fun <T> whileMap(cond: Int, gen: Int): List<T> {
                val out = arrayListOf<T>()
                while (cond(out.size)) out += gen()
                return out
            }
        """.trimIndent()
        )
    }

    @Test
    fun test4b() {
        ValentiaParser.file("fun <T> whileMap(cond: (Int) -> Boolean, gen: () -> T): List<T>")
    }

    @Test
    fun test4c() {
        ValentiaParser.type("(Int) -> Boolean")
    }

    @Test
    fun test5() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test5a() {
        ValentiaParser.file(
            """
            package valentia.util

            interface Indenter {
                fun line(str: String, suffix: String = " {", newline: String = "}", block: () -> Unit): Line
            }
        """.trimIndent()
        )
    }

    @Test
    fun test5b() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test5c() {
        ValentiaParser.file(
            """
            interface Indenter {
                companion object;
                data class Line(val indentLevel: Int, var str: String)           
            }
        """.trimIndent()
        )
    }

    @Test
    fun test6() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test7() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test8() {
        ValentiaParser.file(
            """
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
        """.trimIndent()
        )
    }

    @Test
    fun test9a() {
        ValentiaParser.statements("""
            if (expectOpt("*"+"/")) ;
            skip()
        """.trimIndent())
    }

    @Test
    fun test9b() {
        ValentiaParser.file("""
            object EOFToken : Token("")
            abstract class HiddenToken(str: String) : Token(str) 
        """.trimIndent())
    }

    @Test
    fun test9() {
        //while (true)
        ValentiaParser.file(
            """
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
                            } else if (matches("/"+"*")) {
                                var open = 0
                                while (hasMore) {
                                    if (expectOpt("/"+"*")) {
                                        open++
                                    }
                                    if (expectOpt("*"+"/")) {
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
                //val numberCleanedUp: String by lazy 10
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
                    } ?: error("Number '${DOLLAR}number' : '${DOLLAR}numberCleanedUp'")
                }
            }

            data class IDToken(val id: String) : Token(id)

            data class CharLiteralToken(val string: String, val char: Char) : Token(string)
            data class StringInterpolationToken(val string: String, val tokens: List<StringPartToken>, val type: String) : Token(string)

            sealed class StringPartToken(str: String) : Token(str)
            data class ExpressionStringPartToken(val string: String, val expr: List<Token>) : StringPartToken(string)
            data class LiteralStringPartToken(val string: String) : StringPartToken(string)
            data class EscapeStringPartToken(val string: String, val c: Char) : StringPartToken(string)
        """.trimIndent()
        )
    }

    @Test
    fun test10a() {
        val str = """
            package valentia.parser

            import valentia.ast.*
            import valentia.util.Disjunction3

            open class KotlinParser(tokens: List<Token>) : TokenReader(tokens), BaseTokenParser {
                fun <T> parseListNew(
                    separator: () -> Unit = { COMMA() },
                    trailingSeparator: Boolean = false,
                    oneOrMore: Boolean = true,
                    block: () -> T
                ): List<T> {
                }
            }
        """
        ValentiaParser.file(str)
    }

    @Test
    fun test10b() {
        ValentiaParser.file("""
            fun functionDeclaration(): Decl {
                debug("TODO: functionDeclaration")
                val modifiers = modifiers(atLeastOne = false)
                var funcNameOpt: String? = null
                val spos = pos
                expect("fun")
                try {
                } catch (e: Throwable) {
                }
            }
        """.trimIndent())
    }

    @Test
    fun test11a() {
        ValentiaParser.file("""
            fun functionDeclaration(): Decl {
                return try {
                    1
                } catch (e: Throwable) {
                    2
                }
            }
        """.trimIndent())
    }

    @Test
    fun test12a() {
        ValentiaParser.file("""
            fun HexDigitValue(c: Char): Int = when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> (c - 'a') + 10
                in 'A'..'F' -> (c - 'A') + 10
                else -> -1
            }
        """.trimIndent())
    }

    @Test
    fun test13() {
        ValentiaParser.file("""
            package korlibs.datastructure

            import kotlin.test.*

            // This test is only defined on the JVM, so it doesn't execute on mobile, specially on Android,
            // where memory limitations might make it run into an OutOfMemory exception
            class IntMapJvmTest {
                private val items by lazy { listOf(0, 13, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 884, 900, 901, 902, 903, 904, 905, 906, 908, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 977, 978, 982, 1024, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096, 1097, 1098, 1099, 1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156, 1157, 1158, 1160, 1161, 1162, 1163, 1164, 1165, 1166, 1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176, 1177, 1178, 1179, 1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194, 1195, 1196, 1197, 1198, 1199, 1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216, 1217, 1218, 1219, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228, 1229, 1230, 1231, 1232, 1233, 1234, 1235, 1236, 1237, 1238, 1239, 1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1276, 1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299, 8192, 8193, 8194, 8195, 8196, 8197, 8198, 8199, 8200, 8201, 8202, 8203, 8211, 8212, 8213, 8215, 8216, 8217, 8218, 8219, 8220, 8221, 8222, 8224, 8225, 8226, 8230, 8240, 8242, 8243, 8249, 8250, 8252, 8260, 8355, 8356, 8358, 8359, 8360, 8361, 8363, 8364, 8369, 8377, 8378) }

                @Test
                //@Ignore
                fun testLimits() {
                    val set = LinkedHashSet<Int>()
                    val map = IntMap<Unit>()
                    val mapInt = IntIntMap()
                    for (i in items) {
                        for (j in items) {
                            if (j % 4 == 0) {
                                val key = i or (j shl 16)
                                set += key
                                map[key] = Unit
                                mapInt[key] = 1
                            }
                        }
                    }
                    assertEquals(131211, map.size)
                    assertEquals(131211, mapInt.size)
                    assertTrue { map.backSize <= 4194789 }
                    assertTrue { mapInt.backSize <= 4194789 }
                    assertTrue { map.stashSize <= 512 }
                    assertTrue { mapInt.stashSize <= 512 }
                    assertTrue { set.all { it in map } }
                    assertTrue { set.all { it in mapInt } }
                }
            }

        """.trimIndent())
    }

    @Test
    fun test14() {
        ValentiaParser.file("""
            package korlibs.datastructure

            open class LRUCache<K, V>(
                val maxSize: Int = Int.MAX_VALUE,
                val maxMemory: Long = Long.MAX_VALUE,
                val atLeastOne: Boolean = true,
                val getElementMemory: (V) -> Int = { 1 }
            ) : BaseCacheMap<K, V>() {
                var computedMemory: Long = 0L
                    private set

                override fun mustFree(key: K, value: V): Boolean {
                    if (atLeastOne && size <= 1) return false
                    if (size > maxSize) return true
                    if (computedMemory > maxMemory) return true
                    return false
                }

                override fun putNew(key: K, value: V) {
                    computedMemory += getElementMemory(value)
                }

                override fun freed(key: K, value: V) {
                    computedMemory -= getElementMemory(value)
                }

                // LRU
                override fun get(key: K): V? {
                    if (map.isNotEmpty() && map.keys.last() == key) return map[key]
                    val value = map.remove(key)
                    if (value != null) map[key] = value
                    return value
                }
            }
        """.trimIndent())
    }

    @Test
    fun test15a() {
        ValentiaParser.file("""
            package korlibs.datastructure

            import korlibs.datastructure.internal.*
            import kotlin.math.*

            inline fun count(cond: (index: Int) -> Boolean): Int {
                var counter = 0
                while (cond(counter)) counter++
                return counter
            }
            inline fun <reified T> mapWhile(cond: (index: Int) -> Boolean, gen: (Int) -> T): List<T> = arrayListOf<T>().apply { while (cond(this.size)) this += gen(this.size) }
            inline fun <reified T> mapWhileArray(cond: (index: Int) -> Boolean, gen: (Int) -> T): Array<T> = mapWhile(cond, gen).toTypedArray()
            inline fun mapWhileInt(cond: (index: Int) -> Boolean, gen: (Int) -> Int): IntArray = IntArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toIntArray()
            inline fun mapWhileFloat(cond: (index: Int) -> Boolean, gen: (Int) -> Float): FloatArray = FloatArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toFloatArray()
            inline fun mapWhileDouble(cond: (index: Int) -> Boolean, gen: (Int) -> Double): DoubleArray = DoubleArrayList().apply { while (cond(this.size)) this += gen(this.size) }.toDoubleArray()
        """.trimIndent())
    }

    @Test
    fun test16a() {
        ValentiaParser.expression("""
            buckets.values
                .flatMap
        """.trimIndent())
    }

    @Test
    fun test16() {
        ValentiaParser.file("""
            package korlibs.datastructure

            open class CustomHashMap<K, V>(
                val hasher: (key: K) -> Int,
                val equalKey: (a: K, b: K) -> Boolean,
                val equalValue: (a: V, b: V) -> Boolean,
                val initialCapacity: Int = 16,
            ) : MutableMap<K, V> {
                inner class Bucket {
                    val keys = fastArrayListOf<K>()
                    val values = fastArrayListOf<V>()

                    fun getKeyIndex(key: K): Int {
                        for (n in keys.indices) if (equalKey(keys[n], key)) return n
                        return -1
                    }

                    fun getValueIndex(value: V): Int {
                        for (n in values.indices) if (equalValue(values[n], value)) return n
                        return -1
                    }
                }

                override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
                    get() = buckets.values
                        .flatMap { bucket -> bucket?.keys?.indices?.map { it to bucket } ?: listOf() }
                        .map { (index, bucket) ->
                            object : MutableMap.MutableEntry<K, V> {
                                override val key: K = bucket.keys[index]
                                override val value: V = bucket.values[index]
                                override fun setValue(newValue: V): V = bucket.values[index].also { bucket.values[index] = newValue }
                            }
                        }
                        .toMutableSet()
                override val keys: MutableSet<K> get() = buckets.values.flatMap { it?.keys ?: listOf() }.toMutableSet()
                override val values: MutableCollection<V> get() = buckets.values.flatMap { it?.values ?: listOf() }.toMutableList()

                @PublishedApi
                internal val buckets = IntMap<Bucket>(initialCapacity)
                private fun getOrCreateBucket(key: K): Bucket = buckets.getOrPut(hasher(key)) { Bucket() }
                private fun getBucketOrNull(key: K): Bucket? = buckets.get(hasher(key))

                override var size: Int = 0
                    protected set

                override fun clear() {
                    size = 0
                    buckets.clear()
                }

                override fun isEmpty(): Boolean = size == 0

                override fun get(key: K): V? {
                    val bucket = getBucketOrNull(key) ?: return null
                    val keys = bucket.keys
                    for (n in keys.indices) {
                        if (equalKey(keys[n], key)) return bucket.values[n]
                    }
                    return null
                }

                override fun containsValue(value: V): Boolean {
                    buckets.fastForEach { _, bucket ->
                        if (bucket.getValueIndex(value) >= 0) return true
                    }
                    return false
                }

                override fun containsKey(key: K): Boolean {
                    val bucket = getBucketOrNull(key) ?: return false
                    return bucket.getKeyIndex(key) >= 0
                }

                override fun remove(key: K): V? {
                    val bucketKey = hasher(key)
                    val bucket = buckets[bucketKey] ?: return null
                    val index = bucket.getKeyIndex(key).takeIf { it >= 0 } ?: return null
                    size--
                    bucket.keys.removeAt(index)
                    try {
                        return bucket.values.removeAt(index)
                    } finally {
                        if (bucket.keys.isEmpty()) buckets.remove(bucketKey)
                    }
                }

                override fun put(key: K, value: V): V? {
                    val bucket = getOrCreateBucket(key)
                    val index = bucket.getKeyIndex(key)
                    return if (index >= 0) {
                        val oldValue = bucket.values[index]
                        bucket.values[index] = value
                        oldValue
                    } else {
                        size++
                        bucket.keys.add(key)
                        bucket.values.add(value)
                        null
                    }
                }

                override fun putAll(from: Map<out K, V>) {
                    for ((k, v) in from) {
                        put(k, v)
                    }
                }
            }

        """.trimIndent())
    }

    @Test
    fun test17() {
        ValentiaParser.file("""
            expect inline fun <T> Any?.fastCastTo(): T
        """.trimIndent())
    }

    @Test
    fun test18a() {
        ValentiaParser.file("""
            private var data = 1 as Array
            private var _start: Int = 0
        """.trimIndent())
    }

    @Test
    fun test18b() {
        ValentiaParser.expression("""
            arrayOfNulls<Any>(initialCapacity) as Array<TGen>
        """.trimIndent())
    }

    //val a by lazy { 10 }

    @Test
    fun test19() {
        ValentiaParser.file("""
            package korlibs.datastructure

            /**
             * [Map] with [String] keys that are treated in a insensitive manner.
             */
            class CaseInsensitiveStringMap<T> private constructor(
                private val mapOrig: MutableMap<String, T>,
                private val lcToOrig: MutableMap<String, String>,
                private val mapLC: MutableMap<String, T>
            ) : MutableMap<String, T> by mapOrig {
                constructor() : this(LinkedHashMap(), LinkedHashMap(), LinkedHashMap())
                constructor(data: Map<String, T>) : this() { putAll(data) }
                constructor(vararg items: Pair<String, T>) : this() { putAll(items.toList()) }

                override fun containsKey(key: String): Boolean = mapLC.containsKey(key.toLowerCase())

                override fun clear() {
                    mapOrig.clear()
                    mapLC.clear()
                    lcToOrig.clear()
                }

                override fun get(key: String): T? = mapLC[key.toLowerCase()]

                override fun put(key: String, value: T): T? {
                    remove(key)
                    mapOrig[key] = value
                    lcToOrig[key.toLowerCase()] = key
                    return mapLC.put(key.toLowerCase(), value)
                }

                override fun putAll(from: Map<out String, T>) {
                    for (v in from) put(v.key, v.value)
                }

                override fun remove(key: String): T? {
                    val lkey = key.toLowerCase()
                    val okey = lcToOrig[lkey]
                    mapOrig.remove(okey)
                    val res = mapLC.remove(lkey)
                    lcToOrig.remove(lkey)
                    return res
                }

                override fun equals(other: Any?): Boolean = (other is CaseInsensitiveStringMap<*>) && this.mapLC == other.mapLC
                override fun hashCode(): Int = mapLC.hashCode()
            }

            fun <T> Map<String, T>.toCaseInsensitiveMap(): Map<String, T> =
                CaseInsensitiveStringMap<T>().also { it.putAll(this) }
        """.trimIndent())
    }

    @Test
    fun test20a() {
        ValentiaParser.statement("""
            this[key]!! += value
        """.trimIndent())
    }

    @Test
    fun test20() {
        ValentiaParser.file("""
            package korlibs.datastructure

            typealias MapList<K, V> = Map<K, List<V>>
            typealias MutableMapList<K, V> = MutableMap<K, ArrayList<V>>
            typealias LinkedHashMapList<K, V> = LinkedHashMap<K, ArrayList<V>>

            fun <K, V> MapList<K, V>.getFirst(key: K): V? = this[key]?.firstOrNull()
            fun <K, V> MapList<K, V>.getLast(key: K): V? = this[key]?.lastOrNull()

            fun <K, V> MapList<K, V>.flatten(): List<Pair<K, V>> = flatMapIterable().toList()

            fun <K, V> MapList<K, V>.flatMapIterable(): Iterable<Pair<K, V>> = object : Iterable<Pair<K, V>> {
                override fun iterator(): Iterator<Pair<K, V>> = flatMapIterator()
            }

            fun <K, V> MapList<K, V>.flatMapIterator(): Iterator<Pair<K, V>> =
                entries.flatMap { item -> item.value.map { item.key to it } }.iterator()

            fun <K, V> MutableMapList<K, V>.append(key: K, value: V): MutableMapList<K, V> {
                getOrPut(key) { arrayListOf() }
                this[key]!! += value
                return this
            }

            fun <K, V> MutableMapList<K, V>.replace(key: K, value: V): MutableMapList<K, V> {
                remove(key)
                append(key, value)
                return this
            }

            fun <K, V> MutableMapList<K, V>.appendAll(vararg items: Pair<K, V>): MutableMapList<K, V> =
                this.apply { for ((k, v) in items) append(k, v) }

            fun <K, V> MutableMapList<K, V>.replaceAll(vararg items: Pair<K, V>): MutableMapList<K, V> =
                this.apply { for ((k, v) in items) replace(k, v) }

            fun <K, V> linkedHashMapListOf(vararg items: Pair<K, V>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
                for ((k, v) in items) append(k, v)
            }

            fun <K, V> LinkedHashMapList(items: List<Pair<K, V>>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
                for ((k, v) in items) append(k, v)
            }

            fun <K, V> LinkedHashMapList(items: MapList<K, V>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
                for ((k, values) in items) for (v in values) append(k, v)
            }
        """.trimIndent())
    }

    @Test
    fun test21() {
        ValentiaParser.file("""
            /**
             * Double growable ArrayList without boxing.
             */
            @Suppress("UNCHECKED_CAST")
            class DoubleArrayList(capacity: Int = 7) : IDoubleArrayList {
                companion object
                val a: Int
                //var data: DoubleArray = DoubleArray(capacity) as DoubleArray; private set
            }
        """.trimIndent())
    }

    @Test
    fun test22a() {
        ValentiaParser.topLevelDecl("private var C.prop by WeakProperty { 0 }")
    }

    @Test
    fun test22() {
        ValentiaParser.file("""
            package korlibs.datastructure

            import kotlin.test.Test
            import kotlin.test.assertEquals


            private class C {
                val value = 1
            }

            private var C.prop by WeakProperty { 0 }
            private var C.prop2 by WeakPropertyThis<C, String> { "${'$'}{value * 2}" }

            class WeakPropertyTest {
            	@Test
            	fun name() {
            		val c1 = C()
            		val c2 = C()
            		assertEquals(0, c1.prop)
            		assertEquals(0, c2.prop)
            		c1.prop = 1
            		c2.prop = 2
            		assertEquals(1, c1.prop)
            		assertEquals(2, c2.prop)

            		assertEquals("2", c2.prop2)
            		c2.prop2 = "3"
            		assertEquals("3", c2.prop2)
            	}
            }
        """.trimIndent())
    }

    @Test
    fun test23a() {
        ValentiaParser.file("""
            class Bitmap8(
            	data: ByteArray = ByteArray(width * height),
            ) {
            }
        """.trimIndent())
    }

    @Test
    fun test23() {
        ValentiaParser.file("""
            package korlibs.image.bitmap

            import korlibs.datastructure.*
            import korlibs.image.color.*

            class Bitmap8(
            	width: Int,
            	height: Int,
            	data: ByteArray = ByteArray(width * height),
            	palette: RgbaArray = RgbaArray(0x100)
            ) : BitmapIndexed(8, width, height, data, palette) {
            	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap8(width, height, palette = palette)

            	override fun setInt(x: Int, y: Int, color: Int) = setIntIndex(index(x, y), color)
            	override fun getInt(x: Int, y: Int): Int = datau[index(x, y)]
            	override fun getRgbaRaw(x: Int, y: Int): RGBA = palette[get(x, y)]
                override fun getIntIndex(n: Int): Int = datau[n]
                override fun setIntIndex(n: Int, color: Int) { datau[n] = color }

                override fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
                    if (dst !is Bitmap8) return super.copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
                    for (y in 0 until height) {
                        korlibs.memory.arraycopy(this.data, this.index(srcX, srcY + y), (dst as Bitmap8).data, dst.index(dstX, dstY + y), width)
                    }
                }

                override fun clone() = Bitmap8(width, height, data.copyOf(), RgbaArray(palette.ints.copyOf()))

            	override fun toString(): String = "Bitmap8(${'$'}width, ${'$'}height, palette=${'$'}{palette.size})"

                companion object {
                    inline operator fun invoke(width: Int, height: Int, palette: RgbaArray = RgbaArray(0x100), pixelProvider: (x: Int, y: Int) -> Byte): Bitmap8 {
                        return Bitmap8(width, height, ByteArray(width * height) { pixelProvider(it % width, it / width) }, palette)
                    }

                    fun copyRect(
                        src: Bitmap8,
                        srcX: Int,
                        srcY: Int,
                        dst: Bitmap8,
                        dstX: Int,
                        dstY: Int,
                        width: Int,
                        height: Int
                    ) = src.copy(srcX, srcY, dst, dstX, dstY, width, height)
                }
            }

            fun Bitmap.tryToExactBitmap8(): Bitmap8? {
                if (this is BitmapIndexed) {
                    return Bitmap8(width, height, ByteArray(width * height) { this.getIntIndex(it).toByte() }, RgbaArray(this.palette.ints.copyOf(256)))
                }

                val bmp = this.toBMP32IfRequired().depremultipliedIfRequired()
                val bmpInts = bmp.ints
                val palette = Palette(RgbaArray(256))
                val colors = IntIntMap()
                var ncolor = 0
                for (color in bmpInts) {
                     if (!colors.contains(color)) {
                        palette.colors[ncolor] = if (bmp.premultiplied) RGBAPremultiplied(color).depremultiplied else RGBA(color)
                        colors[color] = ncolor++
                    }
                    if (colors.size >= 256) return null
                }
                return Bitmap8(bmp.width, bmp.height, ByteArray(bmp.width * bmp.height) { colors[bmpInts[it]].toByte() }, palette.colors)
            }
        """.trimIndent())
    }

    @Test
    fun test24a() {
        ValentiaParser.statement("""
            return buildVectorPath(VectorPath(), fun VectorPath.() {
                close()
            })
        """.trimIndent())
    }
    @Test
    fun test24b() {
        ValentiaParser.statement("""
            return buildVectorPath(VectorPath(), fun VectorPath.(Int) {
                close()
            })
        """.trimIndent())
    }

    @Test
    fun test25a() {
        ValentiaParser.file("""
            fun FOR(id: String, expr: Expr, block: (StmBuilder.() -> Unit) = null): ForLoopStm? = null
        """.trimIndent())
    }

    @Test
    fun test25b() {
        ValentiaParser.file("""
            fun FOR(block: (StmBuilder.() -> Unit)?)
        """.trimIndent())
    }

    @Test
    fun test25c() {
        ValentiaParser.type("(StmBuilder.() -> Unit)?")
    }

    @Test
    fun test25() {
        ValentiaParser.file("""
            fun FOR(id: String, expr: Expr, block: (StmBuilder.() -> Unit)? = null): ForLoopStm? = null
        """.trimIndent())
    }

    @Test
    fun test26() {
        ValentiaParser.file("""
            package korlibs.datastructure

            /**
             * This acts as a [lazy] delegate but for functions.
             */
            fun <T : Any> (() -> T).memoize(): (() -> T) {
                val func = this
                var set = false
                lateinit var cached: T
                return {
                    if (!set) {
                        cached = func()
                        set = true
                    }
                    cached
                }
            }
        """.trimIndent())
    }

    @Test
    fun test26a() {
        ValentiaParser.file("fun <T : Any> (() -> T).memoize(): (() -> T)")
    }

    @Test
    fun test27() {
        ValentiaParser.file("""
            override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
                null
            } catch (t: Throwable) {
                null
            }
        
            override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) { }
        """.trimIndent())
    }

    @Test
    fun test28a() {
        ValentiaParser.statements("""
            val udata = if (celType == 2) 1 else data
            val a = 1
            AseBitmapCell(bmp, x, y, opacity)
        """.trimIndent())
    }

    @Test
    fun test29() {
        ValentiaParser.file("""
            fun lzfDecompress(in_data: UByteArrayInt, in_len: Int, out_data: UByteArrayInt, out_len: Int): Int {
                var iidx = 0
                var oidx = 0
        
                do {
                    var ctrl = in_data[iidx++]
        
                    if (ctrl < (1 shl 5)) { // literal run
                        ctrl++
        
                        if (oidx + ctrl > out_len) return -1 //SET_ERRNO (E2BIG);
        
                        //println("LITERAL COUNT: ${'$'}ctrl")
        
                        do {
                            out_data[oidx++] = in_data[iidx++]
                        } while ((--ctrl) != 0)
                    } else { // back reference
                        var len = ctrl ushr 5
        
                        var reference = (oidx - ((ctrl and 0x1f) shl 8) - 1).toInt()
        
                        if (len == 7) len += in_data[iidx++]
        
                        reference -= in_data[iidx++]
        
                        //println("LZ: ${'$'}reference, len=${'$'}len")
        
                        if (oidx + len + 2 > out_len) return -1 //SET_ERRNO (E2BIG);
                        if (reference < 0) return -2 //SET_ERRNO (EINVAL);
        
                        out_data[oidx++] = out_data[reference++]
                        out_data[oidx++] = out_data[reference++]
        
                        do {
                            out_data[oidx++] = out_data[reference++]
                        } while ((--len) != 0)
                    }
                } while (iidx < in_len)
        
                return oidx.toInt()
            }
        """.trimIndent())
    }

    @Test
    fun test30() {
        //while (true)
        ValentiaParser.file("""
            package korlibs.image.format.cg

            import cnames.structs.CGImage
            import korlibs.memory.*
            import korlibs.image.bitmap.*
            import korlibs.image.format.*
            import korlibs.io.async.*
            import kotlinx.cinterop.*
            import kotlinx.coroutines.*
            import platform.CoreFoundation.*
            import platform.CoreGraphics.*
            import platform.CoreServices.*
            import platform.ImageIO.*
            import platform.posix.*
            import kotlin.Boolean
            import kotlin.ByteArray
            import kotlin.Int
            import kotlin.native.concurrent.*

            open class CGBaseNativeImageFormatProvider : StbImageNativeImageFormatProvider() {
                companion object : CGBaseNativeImageFormatProvider()
                override fun createBitmapNativeImage(bmp: Bitmap): CoreGraphicsNativeImage = CoreGraphicsNativeImage(bmp.toBMP32().premultipliedIfRequired())
            }

            open class CGNativeImageFormatProvider : CGBaseNativeImageFormatProvider() {
                companion object : CGNativeImageFormatProvider()
                override suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {
                    memScoped {
                        autoreleasepool {
                            val cfdata = data.usePinned { dataPin ->
                                CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                            }
                            val imgSource = CGImageSourceCreateWithData(data = cfdata, options = null)
                            val props = CGImageSourceCopyPropertiesAtIndex(imgSource, 0.convert(), null)
                                ?: error("Failed trying to read image in decodeHeaderInternal")

                            try {
                                return ImageInfo().apply {
                                    this.width = getIntFromDict(props, kCGImagePropertyPixelWidth)
                                    this.height = getIntFromDict(props, kCGImagePropertyPixelHeight)
                                }
                            } finally {
                                CFRelease(props)
                            }
                        }
                    }
                }

                //override fun createBitmapNativeImage(bmp: Bitmap) = BitmapNativeImage(bmp.toBMP32().premultipliedIfRequired())
                override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
                    // Since we are decoding as premultiplied, we need a decoder that decodes un-multiplied
                    if (props.asumePremultiplied) {
                        return super.decodeInternal(data, props)
                    }

                    return withContext(Dispatchers.ResourceDecoder) {
                        val premultiplied = props.premultipliedSure
                        val maxSize = props.requestedMaxSize
                        memScoped {
                            val maxSizePtr = alloc<IntVar>()
                            autoreleasepool {
                                val cfdata = data.usePinned { dataPin ->
                                    CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                                }
                                val imgSource = CGImageSourceCreateWithData(data = cfdata, options = null)

                                //println("imgSource=${'$'}imgSource")

                                val dict = CFDictionaryCreateMutable(null, 0, null, null)

                                CFDictionaryAddValue(dict, kCGImageSourceShouldCache, kCFBooleanFalse)
                                CFDictionaryAddValue(dict, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanFalse)
                                CFDictionaryAddValue(dict, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)

                                val cgImage: CPointer<CGImage>? = if (maxSize != null) {
                                    maxSizePtr.value = maxSize

                                    // kCGImageSourceSubsampleFactor
                                    CFDictionaryAddValue(
                                        dict,
                                        kCGImageSourceThumbnailMaxPixelSize,
                                        CFNumberCreate(null, kCFNumberSInt32Type, maxSizePtr.ptr)
                                    )
                                    CGImageSourceCreateThumbnailAtIndex(imgSource, 0.convert(), dict)
                                } else {
                                    CGImageSourceCreateImageAtIndex(imgSource, 0.convert(), dict)
                                }
                                val iwidth = CGImageGetWidth(cgImage).toInt()
                                val iheight = CGImageGetHeight(cgImage).toInt()

                                if (iwidth == 0 && iheight == 0) error("Couldn't decode image with CG")

                                // This might have channels changed? RGBA -> ARGB?, might be in float, etc.
                                // https://developer.apple.com/documentation/coregraphics/1455401-cgimagegetalphainfo
                                // https://developer.apple.com/documentation/coregraphics/cgbitmapinfo
                                if (false) {
                                    val data = CGDataProviderCopyData(CGImageGetDataProvider(cgImage))
                                    try {
                                        val pixels = CFDataGetBytePtr(data);
                                        Bitmap32(iwidth, iheight, premultiplied = false).also { bmp ->
                                            bmp.ints.usePinned { pin ->
                                                memcpy(pin.startAddressOf, pixels, (iwidth * iheight * 4).convert())
                                            }
                                        }
                                    } finally {
                                        CFRelease(data)
                                    }
                                } else {

                                    //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                                    //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
                                    val colorSpace = CGColorSpaceCreateDeviceRGB()
                                    try {
                                        val realPremultiplied = true
                                        //val realPremultiplied = premultiplied

                                        val alphaInfo = when (realPremultiplied) {
                                            true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                            false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                                        }

                                        Bitmap32(iwidth, iheight, realPremultiplied).also { bmp ->
                                            bmp.ints.usePinned { pin ->
                                                val context = CGBitmapContextCreate(
                                                    pin.startAddressOf, iwidth.convert(), iheight.convert(), 8.convert(),
                                                    (iwidth * 4).convert(), colorSpace, alphaInfo
                                                )
                                                    ?: error("Couldn't create context for ${'$'}iwidth, ${'$'}iheight, premultiplied=${'$'}premultiplied")

                                                try {
                                                    val rect = CGRectMakeExt(0, 0, iwidth, iheight)
                                                    CGContextDrawImage(context, rect, cgImage)
                                                    CGContextFlush(context)
                                                } finally {
                                                    CGImageRelease(cgImage)
                                                    CGContextRelease(context)
                                                    CFRelease(imgSource)
                                                    CFRelease(cfdata)
                                                }
                                            }
                                        }
                                    } finally {
                                        CGColorSpaceRelease(colorSpace)
                                    }
                                }
                            }
                        }
                    }.wrapNativeExt(props)
                }

                private fun getIntFromDict(props: CFDictionaryRef?, key: CFStringRef?): Int {
                    return memScoped {
                        val vvar = alloc<IntVar>()
                        CFNumberGetValue(CFDictionaryGetValue(props, key)?.reinterpret(), kCFNumberIntType, vvar.ptr)
                        vvar.value
                    }
                }

                override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray = memScoped {
                    val data = CFDataCreateMutable(null, 0)
                    val destination = CGImageDestinationCreateWithData(data, when (props.mimeType) {
                        "image/jpeg", "image/jpg" -> kUTTypeJPEG
                        //"image/heif", "image/heic" -> UTTypeHEIF
                        else -> kUTTypePNG
                    }, 1.convert(), null)
                        ?: error("Failed to create CGImageDestination")

                    val imageProperties = CFDictionaryCreateMutable(null, 0, null, null)
                    val ref = alloc<DoubleVar>()
                    ref.value = props.quality
                    val num = CFNumberCreate(null, kCFNumberDoubleType, ref.ptr)
                    CFDictionaryAddValue(imageProperties, kCGImageDestinationLossyCompressionQuality, num)

                    //println("CGNativeImageFormatProvider.encodeSuspend")
                    val cgImage = image.mainBitmap.toBMP32().toCGImage()

                    try {
                        CGImageDestinationAddImage(destination, cgImage, imageProperties)
                        if (!CGImageDestinationFinalize(destination)) error("Can't write image")
                    } finally {
                        CGImageRelease(cgImage)
                        CFRelease(imageProperties)
                        CFRelease(num)
                        CFRelease(destination)
                    }
                    val length: Int = CFDataGetLength(data).convert()
                    val bytes = CFDataGetMutableBytePtr(data)?.readBytes(length.convert())
                    CFRelease(data)
                    return bytes ?: error("Can't write image")
                }
            }

            /*
            fun Map<*, *>.toCFDictionary(): CFDictionaryRef = memScoped {
                val dict = CFDictionaryCreateMutable(null, 0, null, null)
                for ((key, value) in this) {
                    val ref = alloc<DoubleVar>()
                    ref.value = value.todo
                    CFNumberCreate(null, kCFNumberDoubleType, null, ref.ptr)
                    CFDictionaryAddValue()
                }
                return dict
            }
            */
        """.trimIndent())
    }

    @Test
    fun test31() {
        ValentiaParser.file("""
            internal fun RGBA.toCgColor(releases: Releases, space: CGColorSpaceRef?): CPointer<CGColor>? = memScoped {
                // Not available on iOS
                //CGColorCreateGenericRGB(color.rd.cg, color.gd.cg, color.bd.cg, color.ad.cg)
                val data = allocArray<CGFloatVar>(4)
                //val data = releases.arena.allocArray<CGFloatVar>(4)
                data[0] = this@toCgColor.rd.cg
                data[1] = this@toCgColor.gd.cg
                data[2] = this@toCgColor.bd.cg
                data[3] = this@toCgColor.ad.cg
                val color = CGColorCreate(space, data)
                releases.colors.add(color)
                color
            }
        """.trimIndent())
    }

    @Test
    fun test32() {
        ValentiaParser.file("""
            package korlibs.image.format

            import korlibs.image.bitmap.matchContentsDistinctCount
            import korlibs.io.async.suspendTestNoBrowser
            import korlibs.io.file.std.resourcesVfs
            import kotlin.test.Test
            import kotlin.test.assertEquals
            import kotlin.test.assertTrue

            class DDSTest {
                val props = ImageDecodingProps(format = ImageFormats(PNG, DDS), premultiplied = false)

                @Test
                fun dxt1() = suspendTestNoBrowser {
                    val output = resourcesVfs["dxt1.dds"].readBitmapNoNative(props)
                    val expected = resourcesVfs["dxt1.png"].readBitmapNoNative(props)
                    assertEquals(0, output.matchContentsDistinctCount(expected))
                }

                @Test
                fun dxt3() = suspendTestNoBrowser {
                    val output = resourcesVfs["dxt3.dds"].readBitmapNoNative(props)
                    val expected = resourcesVfs["dxt3.png"].readBitmapNoNative(props)
                    assertTrue { output.matchContentsDistinctCount(expected) < 7000 }
                    //output.writeTo(LocalVfs("c:/temp/dxt3.png"))
                }

                @Test
                fun dxt5() = suspendTestNoBrowser {
                    val output = resourcesVfs["dxt5.dds"].readBitmapNoNative(props)
                    val expected = resourcesVfs["dxt5.png"].readBitmapNoNative(props)
                    assertTrue { output.matchContentsDistinctCount(expected) < 7000 }
                    //output.writeTo(LocalVfs("c:/temp/dxt5.png"))
                }

                @Test
                fun dxt5_registered() = suspendTestNoBrowser {
                    RegisteredImageFormats.temporalRegister(DDS, PNG) {
                        val output = resourcesVfs["dxt5.dds"].readBitmapNoNative()
                        val expected = resourcesVfs["dxt5.dds"].readBitmapNoNative(DDS.toProps())
                        assertEquals(0, output.matchContentsDistinctCount(expected))
                        //output.writeTo(LocalVfs("c:/temp/dxt5.png"))
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun test33a() {
        ValentiaParser.file("""
            internal var baseConsoleHook: ((
                kind: BaseConsole.Kind, 
                msg: Array<out Any?>,
                logInternal: (kind: BaseConsole.Kind, msg: Array<out Any?>) -> Unit,
            ) -> Unit)? = null

            open class BaseConsole() : AnsiEscape {
                enum class Kind(val level: Int, val color: AnsiEscape.Color?) {
                    ERROR(0, AnsiEscape.Color.RED),
                    LOG(5, null),
                }

                data class LogEntry(val kind: Kind, val msg: List<Any?>) {
                    override fun toString(): String = msg.joinToString(", ")
                }
            }

            expect object Console : BaseConsole

            fun Console.assert(cond: Boolean, msg: String) {
                if (cond) throw AssertionError(msg)
            }
        """.trimIndent())
    }

    @Test
    fun test33b() {
        ValentiaParser.type("""
            ((
                kind: BaseConsole.Kind, 
                msg: Array<out Any?>,
                logInternal: (kind: BaseConsole.Kind, msg: Array<out Any?>) -> Unit,
            ) -> Unit)?
        """.trimIndent())
    }

    @Test
    fun test34() {
        ValentiaParser.statement("""
            while (
                PeekMessageW(
                    msg.ptr,
                    null,
                    0.convert(),
                    0.convert(),
                    PM_REMOVE.convert()
                ).toInt() != 0
            ) {
                TranslateMessage(msg.ptr)
                DispatchMessageW(msg.ptr)
            }
        """.trimIndent())
    }

    @Test
    fun test35a() {
        ValentiaParser.statement("""
            (c as? JLabel)?.text ?: (c as? JScrollableTextArea)?.text ?: (c as? JTextComponent)?.text
            ?: (c as? AbstractButton)?.text ?: (c as? Frame)?.title
        """.trimIndent())
    }

    @Test
    fun test35() {
        ValentiaParser.statement("""
            @Suppress("UNCHECKED_CAST")
            override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
                return when (key) {
                    LightProperty.CHECKED -> {
                        (c as? JComponent)?.name
                    }
                    LightProperty.CHECKED -> {
                        (c as? JToggleButton)?.isSelected ?: false
                    }
                    LightProperty.TEXT -> {
                        (c as? JLabel)?.text ?: (c as? JScrollableTextArea)?.text ?: (c as? JTextComponent)?.text
                        ?: (c as? AbstractButton)?.text ?: (c as? Frame)?.title
                    }
                    LightProperty.SELECTED_INDEX -> {
                        (c as? JComboBox<ComboBoxItem>)?.selectedIndex ?: super.getProperty(c, key)
                    }
                    LightProperty.PROGRESS_CURRENT -> {
                        (c as? JSlider)?.value ?: super.getProperty(c, key)
                    }
                    LightProperty.PROGRESS_MAX -> {
                        (c as? JSlider)?.maximum ?: super.getProperty(c, key)
                    }
                    else -> super.getProperty(c, key)
                } as T
            }
        """)
    }

    @Test
    fun test36a() {
        ValentiaParser.statement("(c).text = text")
        ValentiaParser.statement("(c).text += text")
    }

    @Test
    fun test36() {
        ValentiaParser.file("""
            override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
                when (key) {
                    LightProperty.VISIBLE -> {
                        val visible = key[value]
                        if (c is JFrame2) {
                            if (!c.isVisible && visible) {
                                c.setLocationRelativeTo(null)
                            }
                        }
                        (c as Component).isVisible = visible
                    }
                    LightProperty.TEXT -> {
                        val text = key[value]
                        (c as? JLabel)?.text = text
                        (c as? JScrollableTextArea)?.text = text
                        (c as? JTextComponent)?.text = text
                        (c as? AbstractButton)?.text = text
                        (c as? Frame)?.title = text
                    }
                    LightProperty.NAME -> {
                        val text = key[value]
                        (c as? JComponent)?.name = text
                    }
                    LightProperty.IMAGE -> {
                        val bmp = key[value]
                        val image = (c as? JImage)
                        if (image != null) {
                            if (bmp == null) {
                                image.image = null
                            } else {
                                if (bmp is AwtNativeImage) {
                                    image.image = bmp.awtImage.clone()
        
                                } else {
                                    if ((image.width != bmp.width) || (image.height != bmp.height)) {
                                        //println("*********************** RECREATED NATIVE IMAGE!")
                                        image.image = bmp.toAwt()
                                    }
                                    bmp.toBMP32().transferTo(image.image!!)
                                }
                            }
                            image.repaint()
                        }
                    }
                    LightProperty.ICON -> {
                        val bmp = key[value]
                        when (c) {
                            is JFrame2 -> {
                                c.iconImage = bmp?.toBMP32()?.toAwt()
                            }
                        }
                    }
                    LightProperty.IMAGE_SMOOTH -> {
                        val v = key[value]
                        when (c) {
                            is JImage -> {
                                c.smooth = v
                            }
                        }
                    }
                    LightProperty.BGCOLOR -> {
                        val v = key[value]
                        (c as? Component)?.background = Color(v.value, true)
                    }
                    LightProperty.PROGRESS_CURRENT -> {
                        (c as? JProgressBar)?.value = key[value]
                        (c as? JSlider)?.value = key[value]
                    }
                    LightProperty.PROGRESS_MAX -> {
                        (c as? JProgressBar)?.maximum = key[value]
                        (c as? JSlider)?.maximum = key[value]
                    }
                    LightProperty.CHECKED -> {
                        (c as? JToggleButton)?.isSelected = key[value]
                    }
                    //LightProperty.RADIO_GROUP -> {
                    //	val lg = value as LightRadioButtonGroup
                    //	val group = lg.extra?.getOrPut("group") { ButtonGroup() } as ButtonGroup
                    //	val but = c as? AbstractButton
                    //	if (but != null) group.add(but)
                    //}
                    LightProperty.COMBO_BOX_ITEMS -> {
                        val cb = (c as? JComboBox<ComboBoxItem>)
                        if (cb != null) {
                            cb.removeAllItems()
                            for (item in (value as List<ComboBoxItem>)) cb.addItem(item)
                        }
                    }
                    LightProperty.SELECTED_INDEX -> {
                        val cb = (c as? JComboBox<ComboBoxItem>)
                        if (cb != null) {
                            cb.selectedIndex = cb.selectedIndex
                        }
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun test37() {
        ValentiaParser.file("""
            open class UniformBlock(val fixedLocation: Int) {
                val name: String get() = this::class.portableSimpleName
                private val layout = KMemLayoutBuilder()
            }
        """.trimIndent())
    }

    @Test
    fun test37a() {
        ValentiaParser.expression("this::class.portableSimpleName")
    }

    @Test
    fun test38a() {
        ValentiaParser.expression("""
            (1
                shl 2)
        """.trimIndent())
    }

    @Test
    fun test38b() {
        println(ValentiaParser.expression("""
            (1 shl 2)
        """.trimIndent()))
    }

    @Test
    fun test38() {
        ValentiaParser.statement("""
            val eventMask = NativeLong(
                (ExposureMask
                    or StructureNotifyMask
                    or EnterWindowMask
                    )
                    .toLong()
            )
        """)
    }

    @Test
    fun test39a() {
        ValentiaParser.file("""
            fun onOutOnOver(out: @EventsDslMarker (MouseEvents) -> Unit): T
        """.trimIndent())
    }

    @Test
    fun test39() {
        ValentiaParser.file("""
            inline fun <T : View> T.onOutOnOver(
                noinline out: @EventsDslMarker (MouseEvents) -> Unit,
                noinline over: @EventsDslMarker (MouseEvents) -> Unit
            ): T {
                var component: Closeable? = null
                onOut { events ->
                    component?.close()
                    component = null
                    out(events)
                }
                onOver { events -> component = onNextFrame { over(events) } }
                return this
            }

            inline fun <T : View?> T.onClick(noinline handler: @EventsDslMarker suspend (MouseEvents) -> Unit) =
                doMouseEvent(MouseEvents::click, handler)
        """.trimIndent())
    }

    @Test
    fun test40() {
        ValentiaParser.file("""
            @OptIn(KorgeInternal::class)
            abstract class View internal constructor(
                /** Indicates if this class is a container or not. This is only overridden by Container. This check is performed like this, to avoid type checks. That might be an expensive operation in some targets. */
                val isContainer: Boolean
            ) : BaseView(), Renderable
                , BView
                , HitTestable
                , WithHitShape2D
            //, EventDispatcher by EventDispatcher.Mixin()
        """.trimIndent())
    }

    @Test
    fun test41() {
        ValentiaParser.file("""
            package korlibs.korge.gradle.util

            import java.io.*
            import java.nio.file.Files

            object LDLibraries {
                private val libFolders = LinkedHashSet<File>()
                private val loadConfFiles = LinkedHashSet<File>()

                val ldFolders: List<File> get() = libFolders.toList()

                // /etc/ld.so.conf
                // include /etc/ld.so.conf.d/*.conf

                fun addPath(path: String) {
                    val file = File(path)
                    if (file.isDirectory) {
                        libFolders.add(file)
                    }
                }

                init {
                    try {
                        // Fixed paths as described https://renenyffenegger.ch/notes/Linux/fhs/etc/ld_so_conf
                        addPath("/lib")
                        addPath("/usr/lib")
                        // Load config file
                        loadConfFile(File("/etc/ld.so.conf"))
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }

                fun hasLibrary(name: String) = libFolders.any { File(it, name).exists() }

                private fun loadConfFile(file: File) {
                    if (file in loadConfFiles) return
                    loadConfFiles.add(file)
                    for (line in file.readLines()) {
                        val tline = line.trim().substringBefore('#').takeIf { it.isNotEmpty() } ?: continue

                        if (tline.startsWith("include ")) {
                            val glob = tline.removePrefix("include ")
                            val globFolder = File(glob).parentFile
                            val globPattern = File(glob).name
                            if (globFolder.isDirectory) {
                                for (folder in
                                Files.newDirectoryStream(globFolder.toPath(), globPattern).toList().map { it.toFile() }
                                ) {
                                    loadConfFile(folder)
                                }
                            }
                        } else {
                            addPath(tline)
                        }
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun test42() {
        ValentiaParser.expression("""
            throw SecurityException(
                ("Failed to open "
                        + URANDOM_FILE + " for reading"), e
            )
        """.trimIndent())
    }

    @Test
    fun test43() {
        ValentiaParser.statement("""
            Float32Buffer(14).also { array ->
                array.setArray(1, floatArrayOf(1f, 2f, 3f, 4f))
                array.setArray(7, floatArrayOf(-1f, -2f, -3f, -4f, -5f, -6f, -7f), 2, 3)
                assertEquals(
                    expectedStr,
                    ""${'"'}
                    ${'"'}{array.getArray().map { it.toInt() }.joinToString(",")}
                    ${'"'}{array.getArray(0, size = array.size).map { it.toInt() }.joinToString(",")}
                    ${'"'}{array.getArray(1).map { it.toInt() }.joinToString(",")}
                    ${'"'}{array.getArray(2, size = 3).map { it.toInt() }.joinToString(",")}
                    ""${'"'}.trimIndent()
                )
            }
        """.trimIndent())
    }

    @Test
    fun test44() {
        ValentiaParser.file("""
            fun PathInfo.normalize(): String {
            	val path = this.fullPath
            	val schemeIndex = path.indexOf(":")
            	return if (schemeIndex >= 0) {
            		val take = if (path.substring(schemeIndex).startsWith("://")) 3 else 1
            		path.substring(0, schemeIndex + take) + path.substring(schemeIndex + take).pathInfo.normalize()
            	} else {
            		val path2 = path.replace('\\', '/')
            		val out = ArrayList<String>()

            		path2.split("/").fastForEach { part ->
            			when (part) {
            				"", "." -> if (out.isEmpty()) out += "" else Unit
            				".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
            				else -> out += part
            			}
            		}
            		out.joinToString("/")
            	}
            }
        """.trimIndent())
    }

    @Test
    fun test45() {
        ValentiaParser.statement("""
            do {
            } while (inputBase.readS32LE(matchIndex) != inputBase.readS32LE(input) || matchIndex + MAX_DISTANCE < input
            )
        """.trimIndent())
    }

    @Test
    fun test46() {
        ValentiaParser.statement("""
            override fun toString(): String =
                "CurveLUT[$('$')curve](${'$'}{
                    (0 until size).joinToString(", ") {
                        "${'$'}{ts[it]},len=${'$'}{estimatedLengths[it]}: ${'$'}{points[it]}"
                    }
                })"
        """)
    }

    @Test
    fun test47a() {
        ValentiaParser.file("""
            import js.externals.jquery.`${'$'}`
        """.trimIndent())
    }

    @Test
    fun test47b() {
        ValentiaParser.statement("""
            test(cb@ { e ->})
        """.trimIndent())
    }

    @Test
    fun test47c() {
        ValentiaParser.file("""
            private fun installSearchBar() {
                searchBar.addEventListener("keyup", callback = cb@{ e ->
                    // If key isn't "Enter" then return
                    if ((e as? KeyboardEvent)?.which != 13) return@cb
                    val searchString = (e.target as? HTMLInputElement)?.value ?: return@cb
                    if (searchString.isBlank()) return@cb
                })
            }
        """.trimIndent())
    }

    @Test
    fun test48() {
        ValentiaParser.file("""
            class TOCMaker : PandocVisitor() {
                val fakeTop = TOCElement(listOf(), "top")
            
                /* this is a shallow visitor, not going below level 1 */
                override fun visit(b: Block): Block =
                        @Suppress(Warnings.USELESS_CAST)
                        if (b is Block.Header) visit(b as Block.Header)
                        else b
            
                override fun visit(b: Block.Header): Block {
                    fakeTop.add(b.level, b.text, b.attr.id, numbered = "unnumbered" !in b.attr.classes)
                    return b
                }
            
                fun buildPandocList(): Block =
                        fakeTop
                                .toPandocList()
                                .filterIsInstance<Block.BulletList>()
                                .first()
                                .items
                                .first()
                                .let { Block.Div(Attr(id = "TOC"), it) }
            }
        """.trimIndent())
    }

    @Test
    fun test49() {
        ValentiaParser.file("""
            interface ShapeFactory<in T : ShapeParameters> {
                fun createShape(shapeParameters: T): Shape
            }
        """.trimIndent())
    }

    @Test
    fun test50() {
        ValentiaParser.statement("""
            eventListeners.getOrPut(type) { FastArrayList() } += listener
        """.trimIndent())
    }

    @Test
    fun test51a() {
        ValentiaParser.file("""
            internal class InnerFunction(
                val rw_upvalue1: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
                , val rw_upvalue2: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
                , val ro_upvalue3: LuaValue?  // from enclosing function, but read-only everywhere.
            ) : TwoArgFunction()
        """.trimIndent())
    }

    @Test
    fun test51() {
        ValentiaParser.file("""
            package org.luaj.luajc

            import org.luaj.vm2.LuaValue
            import org.luaj.vm2.Varargs
            import org.luaj.vm2.lib.TwoArgFunction
            import org.luaj.vm2.lib.VarArgFunction

            // Must have this in the main chunk so it can be loaded and instantiated on all platforms.
            class SampleMainChunk : VarArgFunction() {

                internal var rw_ENV: Array<LuaValue> = arrayOf()  // The environment when it is read-write
                //	LuaValue ro_ENV;  // The environment when it is read-only in all sub-functions

                internal var rw_openup1: Array<LuaValue>? =
                    null  // upvalue that we create and modify in "slot" 1, passed to sub-function in initer.
                internal var rw_openup2: Array<LuaValue>? =
                    null  // array is instantiated on first set or before supply to closure, after that value is get, set.
                internal var rw_openup3: Array<LuaValue>? =
                    null  // closing these nulls them out, sub-functions still retain references to array & can use
                internal var ro_openup4: LuaValue? =
                    null  // open upvalue that is read-only once it is supplied to an inner function.
                internal var ro_openup5: LuaValue? = null  // closing this also nulls it out.

                fun initupvalue1(v: Array<LuaValue>) {
                    this.rw_ENV = v
                }

                override fun invoke(args: Varargs): Varargs {
                    rw_ENV[0].get(`${'$'}print`).call(`${'$'}foo`)

                    rw_ENV[0].set(`${'$'}print`, InnerFunction(rw_openup3, rw_openup1, ro_openup5))

                    return LuaValue.NIL
                }

                internal class InnerFunction(
                    val rw_upvalue1: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
                    , val rw_upvalue2: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
                    , val ro_upvalue3: LuaValue?  // from enclosing function, but read-only everywhere.
                ) : TwoArgFunction() {

                    var rw_openup1: Array<LuaValue>? =
                        null  // closing these nulls them out, sub-functions still retain references to array & can use
                    var ro_openup2: LuaValue? = null  // open upvalue that is read-only once it is supplied to an inner function.

                    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                        return LuaValue.NIL
                    }

                    companion object {
                        val `${'$'}print`: LuaValue = LuaValue.valueOf("print") // A constant, named for what it is.
                        val `${'$'}foo`: LuaValue = LuaValue.valueOf("foo")
                    }

                }

                companion object {

                    internal val `${'$'}print`: LuaValue = LuaValue.valueOf("print")
                    internal val `${'$'}foo`: LuaValue = LuaValue.valueOf("foo")
                }

            }
        """.trimIndent())
    }

    @Test
    fun test52() {
        ValentiaParser.file("""
            private fun getIndentFunction(indent: String) = when {
                indent.isEmpty() -> { line: String -> line }
                else -> { line: String -> indent + line }
            }
        """.trimIndent())
    }

    @Test
    fun test53() {
        val str = """
            ""${'"'}${'$'}{array.getArray(2, size = 3).map { it.toInt() }.joinToString(",")}""${'"'}.trimIndent()
        """.trim()
        println(ValentiaTokenizer(str).tokenize())
        ValentiaParser.expression(str)
    }
}