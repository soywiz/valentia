package valentia.parser

import valentia.ast.*

interface KotlinLexer : UnicodeLexer {
    /**
     * Kotlin lexical grammar in ANTLR4 notation
     */
    //lexer grammar KotlinLexer;
    //
    //import UnicodeClasses;
    //

// SECTION: lexicalGeneral
    //ShebangLine
    //    : '#!' ~[\r\n]*
    //    ;
    fun ShebangLine(): String {
        expect("#!")
        return "#!" + readUntil { it == '\r' || it == '\n' }
    }

    //DelimitedComment
    //    : '/*' ( DelimitedComment | . )*? '*/'
    //      -> channel(HIDDEN)
    //    ;
    fun DelimitedComment(): Unit {
        expect("/*")
        while (hasMore) {
            if (matches("/*")) DelimitedComment()
            if (expectOpt("*/")) break
            skip(1)
        }
    }

    //LineComment
    //    : '//' ~[\r\n]*
    //      -> channel(HIDDEN)
    //    ;
    fun LineComment(): Unit {
        expect("//")
        readUntil { it == '\r' || it == '\n' }
    }

    //WS
    //    : [\u0020\u0009\u000C]
    //      -> channel(HIDDEN)
    //    ;
    /** WhiteSpace */
    fun WS(): Unit = TODO("WS")

    //NL: '\n' | '\r' '\n'?;
    fun NL(): Unit {
        when (peekChar()) {
            '\n' -> skip()
            '\r' -> {
                skip()
                expectChar('\n')
            }
            else -> TODO("Expected New Line")
        }
    }

    // NL*
    fun NLs(): Unit {
        loop@while (!eof) {
            Hidden()
            when (peekChar()) {
                '\r', '\n' -> NL()
                else -> break@loop
            }
        }
        Hidden()
    }

    //fragment Hidden: DelimitedComment | LineComment | WS;
    //override
    fun Hidden(): Unit {
        loop@while (hasMore) {
            val c = peekChar()
            when {
                c == ' ' || c == '\t' || c == '\u000c' -> skip()
                matches("/*", consume = false) -> DelimitedComment()
                matches("//", consume = false) -> LineComment()
                else -> break@loop
            }
        }
    }

    // SECTION: separatorsAndOperations
    //RESERVED: '...';
    fun RESERVED(): Unit = expect("...")

    //DOT: '.';
    //val _DOT = "."
    fun DOT(): String = expectAny(".")

    //COMMA: ',';
    //val _COMMA = ","
    fun COMMA(): Unit = expect(",")

    //LPAREN: '(' -> pushMode(Inside);
    //val _LPAREN = "("
    fun LPAREN(): Unit = expect("(")

    //RPAREN: ')';
    //val _RPAREN = ")"
    fun RPAREN(): Unit = expect(")")

    //LSQUARE: '[' -> pushMode(Inside);
    //val _LSQUARE = "["
    fun LSQUARE(): Unit = expect("[")

    //RSQUARE: ']';
    //val _RSQUARE = "]"
    fun RSQUARE(): Unit = expect("]")

    //LCURL: '{' -> pushMode(DEFAULT_MODE);
    fun LCURL(): Unit = expect("{")

    ///*
    // * When using another programming language (not Java) to generate a parser,
    // * please replace this code with the corresponding code of a programming language you are using.
    // */
    //RCURL: '}' { if (!_modeStack.isEmpty()) { popMode(); } };
    fun RCURL(): Unit = expect("}")

    //MULT: '*';
    fun MULT(): Unit = expect("*")

    //MOD: '%';
    fun MOD(): Unit = expect("%")

    //DIV: '/';
    fun DIV(): Unit = expect("/")

    //ADD: '+';
    fun ADD(): Unit = expect("+")

    //SUB: '-';
    fun SUB(): Unit = expect("-")

    //INCR: '++';
    fun INCR(): Unit = expect("++")

    //DECR: '--';
    fun DECR(): Unit = expect("--")

    //CONJ: '&&';
    fun CONJ(): String = expectAny("&&")

    //DISJ: '||';
    fun DISJ(): String = expectAny("||")

    //EXCL_WS: '!' Hidden;
    fun EXCL_WS(): Unit = TODO("! Hidden")

    //EXCL_NO_WS: '!';
    fun EXCL_NO_WS(): Unit = TODO("!")

    //COLON: ':';
    fun COLON(): Unit = expect(":")

    //SEMICOLON: ';';
    fun SEMICOLON(): Unit = expect(";")

    //ASSIGNMENT: '=';
    fun ASSIGNMENT(): Unit = expect("=")

    //ADD_ASSIGNMENT: '+=';
    fun ADD_ASSIGNMENT(): Unit = expect("+=")

    //SUB_ASSIGNMENT: '-=';
    fun SUB_ASSIGNMENT(): Unit = expect("-=")

    //MULT_ASSIGNMENT: '*=';
    fun MULT_ASSIGNMENT(): Unit = expect("*=")

    //DIV_ASSIGNMENT: '/=';
    fun DIV_ASSIGNMENT(): Unit = expect("/=")

    //MOD_ASSIGNMENT: '%=';
    fun MOD_ASSIGNMENT(): Unit = expect("%=")

    //ARROW: '->';
    fun ARROW(): Unit = expect("->")

    //DOUBLE_ARROW: '=>';
    fun DOUBLE_ARROW(): Unit = expect("=>")

    //RANGE: '..';
    fun RANGE(): Unit = expect("..")

    //RANGE_UNTIL: '..<';
    fun RANGE_UNTIL(): Unit = expect("..<")

    //COLONCOLON: '::';
    fun COLONCOLON(): Unit = expect("::")

    //DOUBLE_SEMICOLON: ';;';
    fun DOUBLE_SEMICOLON(): Unit = expect(";;")

    //HASH: '#';
    fun HASH(): Unit = expect("#")

    //AT_NO_WS: '@';
    fun AT_NO_WS(): Unit = expect("@")

    //AT_POST_WS: '@' (Hidden | NL);
    fun AT_POST_WS(): Unit = TODO("'@' (Hidden | NL)")

    //AT_PRE_WS: (Hidden | NL) '@' ;
    fun AT_PRE_WS(): Unit = TODO("(Hidden | NL) '@'")

    //AT_BOTH_WS: (Hidden | NL) '@' (Hidden | NL);
    fun AT_BOTH_WS(): Unit = TODO("(Hidden | NL) '@' (Hidden | NL)")

    //QUEST_WS: '?' Hidden;
    fun QUEST_WS(): Unit = TODO("'?' Hidden")

    //QUEST_NO_WS: '?';
    fun QUEST_NO_WS(): Unit = expect("?")

    //LANGLE: '<';
    fun LANGLE(): Unit = expect("<")

    //RANGLE: '>';
    fun RANGLE(): Unit = expect(">")

    //LE: '<=';
    fun LE(): Unit = expect("<=")

    //GE: '>=';
    fun GE(): Unit = expect(">=")

    //EXCL_EQ: '!=';
    fun EXCL_EQ(): Unit = expect("!=")

    //EXCL_EQEQ: '!==';
    fun EXCL_EQEQ(): Unit = expect("!==")

    //AS_SAFE: 'as?';
    fun AS_SAFE(): Unit = expect("as?")

    //EQEQ: '==';
    fun EQEQ(): Unit = expect("==")

    //EQEQEQ: '===';
    fun EQEQEQ(): Unit = expect("===")

    //SINGLE_QUOTE: '\'';
    fun SINGLE_QUOTE(): Unit = expect("'")

    //AMP: '&';
    fun AMP(): Unit = expect("&")

// SECTION: keywords

    //RETURN_AT: 'return@' Identifier;
    fun RETURN_AT(): Unit = TODO("return@ Identifier")

    //CONTINUE_AT: 'continue@' Identifier;
    fun CONTINUE_AT(): Unit = TODO("continue@ Identifier")

    //BREAK_AT: 'break@' Identifier;
    fun BREAK_AT(): Unit = TODO("break@ Identifier")

    //THIS_AT: 'this@' Identifier;
    fun THIS_AT(): Unit = TODO("this@ Identifier")

    //SUPER_AT: 'super@' Identifier;
    fun SUPER_AT(): Unit = TODO("super@' Identifier")

    //FILE: 'file';
    fun FILE(): Unit = expect("file")

    //FIELD: 'field';
    fun FIELD(): Unit = expect("field")

    //PROPERTY: 'property';
    fun PROPERTY(): Unit = TODO()

    //GET: 'get';
    fun GET(): Unit = expect("get")

    //SET: 'set';
    fun SET(): Unit = expect("set")

    //RECEIVER: 'receiver';
    fun RECEIVER(): Unit = expect("receiver")

    //PARAM: 'param';
    fun PARAM(): Unit = expect("param")

    //SETPARAM: 'setparam';
    fun SETPARAM(): Unit = expect("setparam")

    //DELEGATE: 'delegate';
    fun DELEGATE(): Unit = expect("delegate")

    //PACKAGE: 'package';
    fun PACKAGE(): Unit = expect("package")

    //IMPORT: 'import';
    fun IMPORT(): Unit = expect("import")

    //CLASS: 'class';
    fun CLASS(): Unit = expect("class")

    //INTERFACE: 'interface';
    fun INTERFACE(): Unit = expect("interface")

    //FUN: 'fun';
    fun FUN(): Unit = expect("fun")

    //OBJECT: 'object';
    fun OBJECT(): Unit = expect("object")

    //VAL: 'val';
    fun VAL(): Unit = expect("val")

    //VAR: 'var';
    fun VAR(): Unit = expect("var")

    //TYPE_ALIAS: 'typealias';
    fun TYPE_ALIAS(): Unit = expect("typealias")

    //CONSTRUCTOR: 'constructor';
    fun CONSTRUCTOR(): Unit = expect("constructor")

    //BY: 'by';
    fun BY(): Unit = expect("by")

    //COMPANION: 'companion';
    fun COMPANION(): Unit = expect("companion")

    //INIT: 'init';
    fun INIT(): Unit = expect("init")

    //THIS: 'this';
    fun THIS(): Unit = expect("this")

    //SUPER: 'super';
    fun SUPER(): Unit = expect("super")

    //TYPEOF: 'typeof';
    fun TYPEOF(): Unit = expect("typeof")

    //WHERE: 'where';
    fun WHERE(): Unit = expect("where")

    //IF: 'if';
    fun IF(): Unit = expect("if")

    //ELSE: 'else';
    fun ELSE(): Unit = expect("else")

    //WHEN: 'when';
    fun WHEN(): Unit = expect("when")

    //TRY: 'try';
    fun TRY(): Unit = expect("try")

    //CATCH: 'catch';
    fun CATCH(): Unit = expect("catch")

    //FINALLY: 'finally';
    fun FINALLY(): Unit = expect("finally")

    //FOR: 'for';
    fun FOR(): Unit = expect("for")

    //DO: 'do';
    fun DO(): Unit = expect("do")

    //WHILE: 'while';
    fun WHILE(): Unit = expect("while")

    //THROW: 'throw';
    fun THROW(): Unit = expect("throw")

    //RETURN: 'return';
    fun RETURN(): Unit = expect("return")

    //CONTINUE: 'continue';
    fun CONTINUE(): Unit = expect("continue")

    //BREAK: 'break';
    fun BREAK(): Unit = expect("break")

    //AS: 'as';
    fun AS(): Unit = expect("as")

    //IS: 'is';
    fun IS(): Unit = expect("is")

    //IN: 'in';
    fun IN(): Unit = expect("in")

    //NOT_IS: '!is' (Hidden | NL);
    fun NOT_IS(): Unit = TODO()

    //NOT_IN: '!in' (Hidden | NL);
    fun NOT_IN(): Unit = TODO()

    //OUT: 'out';
    fun OUT(): Unit = expect("out")

    //DYNAMIC: 'dynamic';
    fun DYNAMIC(): Unit = expect("dynamic")

    // SECTION: lexicalModifiers
    //PUBLIC: 'public';
    fun PUBLIC(): Unit = expect("public")

    //PRIVATE: 'private';
    fun PRIVATE(): Unit = expect("private")

    //PROTECTED: 'protected';
    fun PROTECTED(): Unit = expect("protected")

    //INTERNAL: 'internal';
    fun INTERNAL(): Unit = expect("internal")

    //ENUM: 'enum';
    fun ENUM(): Unit = expect("enum")

    //SEALED: 'sealed';
    fun SEALED(): Unit = expect("sealed")

    //ANNOTATION: 'annotation';
    fun ANNOTATION(): Unit = expect("annotation")

    //DATA: 'data';
    fun DATA(): Unit = expect("data")

    //INNER: 'inner';
    fun INNER(): Unit = expect("inner")

    //VALUE: 'value';
    fun VALUE(): Unit = expect("value")

    //TAILREC: 'tailrec';
    fun TAILREC(): Unit = expect("tailrec")

    //OPERATOR: 'operator';
    fun OPERATOR(): Unit = expect("operator")

    //INLINE: 'inline';
    fun INLINE(): Unit = expect("inline")

    //INFIX: 'infix';
    fun INFIX(): Unit = expect("infix")

    //EXTERNAL: 'external';
    fun EXTERNAL(): Unit = expect("external")

    //SUSPEND: 'suspend';
    fun SUSPEND(): Unit = expect("suspend")

    //OVERRIDE: 'override';
    fun OVERRIDE(): Unit = expect("override")

    //ABSTRACT: 'abstract';
    fun ABSTRACT(): Unit = expect("abstract")

    //FINAL: 'final';
    fun FINAL(): Unit = expect("final")

    //OPEN: 'open';
    fun OPEN(): Unit = expect("open")

    //CONST: 'const';
    fun CONST(): Unit = expect("const")

    //LATEINIT: 'lateinit';
    fun LATEINIT(): Unit = expect("lateinit")

    //VARARG: 'vararg';
    fun VARARG(): Unit = expect("vararg")

    //NOINLINE: 'noinline';
    fun NOINLINE(): Unit = expect("noinline")

    //CROSSINLINE: 'crossinline';
    fun CROSSINLINE(): Unit = expect("crossinline")

    //REIFIED: 'reified';
    fun REIFIED(): Unit = expect("reified")

    //EXPECT: 'expect';
    fun EXPECT(): Unit = expect("expect")

    //ACTUAL: 'actual';
    fun ACTUAL(): Unit = expect("actual")

// SECTION: literals

    //fragment DecDigit: '0'..'9';
    fun DecDigit(c: Char): Boolean = c in '0'..'9'

    //fragment DecDigitNoZero: '1'..'9';
    fun DecDigitNoZero(c: Char): Boolean = c in '1'..'9'

    //fragment DecDigitOrSeparator: DecDigit | '_';
    fun DecDigitOrSeparator(c: Char): Boolean = c == '_' || DecDigit(c)

    //fragment DecDigits
    //    : DecDigit DecDigitOrSeparator* DecDigit
    //    | DecDigit
    //    ;
    fun DecDigits(): Unit = TODO("DecDigits")

    //fragment DoubleExponent: [eE] [+-]? DecDigits;
    fun DoubleExponent(): Unit = TODO("DoubleExponent")

    //RealLiteral
    //    : FloatLiteral
    //    | DoubleLiteral
    //    ;
    fun RealLiteral(): Unit = TODO("RealLiteral")

    //FloatLiteral
    //    : DoubleLiteral [fF]
    //    | DecDigits [fF]
    //    ;
    fun FloatLiteral(): Unit = TODO("FloatLiteral")

    //DoubleLiteral
    //    : DecDigits? '.' DecDigits DoubleExponent?
    //    | DecDigits DoubleExponent
    //    ;
    fun DoubleLiteral(): Unit = TODO("DoubleLiteral")

    //IntegerLiteral
    //    : DecDigitNoZero DecDigitOrSeparator* DecDigit
    //    | DecDigit
    //    ;
    //fun IntegerLiteralOpt(): IntLiteralExpr? = enrichOpt {
    //    numericLiteral(radix = 10) { DecDigit(it) }
    //}

    //fragment HexDigit: [0-9a-fA-F];
    fun HexDigit(c: Char): Boolean = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
    fun HexDigitValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> (c - 'a') + 10
        in 'A'..'F' -> (c - 'A') + 10
        else -> -1
    }

    //fragment HexDigitOrSeparator: HexDigit | '_';
    fun HexDigitOrSeparator(c: Char): Boolean = c == '_' || HexDigit(c)

    fun numericLiteralSure(radix: Int, isDigit: (Char) -> Boolean): IntLiteralExpr {
        return numericLiteral(radix, isDigit) ?: error("Not a base-$radix literal in $this")
    }

    fun numericLiteral(radix: Int, isDigit: (Char) -> Boolean): IntLiteralExpr? {
        var n = 0
        val c = peekChar(n)
        var lastC = c
        if (!isDigit(c)) return null
        while (hasMore) {
            n++
            val c = peekChar(n)
            lastC = c
            if (!(isDigit(c) || c == '_')) break
        }
        if (lastC == '_') return null
        return IntLiteralExpr(read(n).replace("_", "").toLong(radix = radix))
    }

    //HexLiteral
    //    : '0' [xX] HexDigit HexDigitOrSeparator* HexDigit
    //    | '0' [xX] HexDigit
    //    ;
    fun HexLiteral(c: Char): Unit = TODO("HexLiteral")
    //fun HexLiteralOpt(): IntLiteralExpr? = enrichOpt {
    //    if (expectAnyOpt("0x", "0X") == null) return@enrichOpt null
    //    numericLiteral(radix = 16) { HexDigit(it) }
    //}

    //fragment BinDigit: [01];
    fun BinDigit(c: Char): Boolean = c in '0'..'1'

    //fragment BinDigitOrSeparator: BinDigit | '_';
    fun BinDigitOrSeparator(c: Char): Boolean = c == '_' || BinDigit(c)

    //BinLiteral
    //    : '0' [bB] BinDigit BinDigitOrSeparator* BinDigit
    //    | '0' [bB] BinDigit
    //    ;
    //fun BinLiteralOpt(): IntLiteralExpr? = enrichOpt {
    //    if (expectAnyOpt("0b", "0B") == null) return@enrichOpt null
    //    numericLiteral(radix = 2) { BinDigit(it) }
    //}

    //UnsignedLiteral
    //    : (IntegerLiteral | HexLiteral | BinLiteral) [uU] [lL]?
    //    ;
    //fun UnsignedLiteral() {
    //    TODO("UnsignedLiteral")
    //}

    //LongLiteral
    //    : (IntegerLiteral | HexLiteral | BinLiteral) [lL]
    //    ;
    //fun LongLiteral(): IntLiteralExpr? {
    //    val res = IntegerLiteralOpt() ?: HexLiteralOpt() ?: BinLiteralOpt() ?: return null
    //    if (expectAnyOpt("l", "L") == null) return null
    //    return LongLiteralExpr(res.value).enrich(res)
    //}

    //BooleanLiteral: 'true'| 'false';
    //fun BooleanLiteralOpt(): BoolLiteralExpr? {
    //    val res = expectAnyOpt("true", "false") ?: return null
    //    return BoolLiteralExpr(res == "true")
    //}

    //NullLiteral: 'null';
    //fun NullLiteral() {
    //    TODO("NullLiteral")
    //}

    //CharacterLiteral
    //    : '\'' (EscapeSeq | ~[\n\r'\\]) '\''
    //    ;
    fun CharacterLiteral(): LiteralExpr {
        TODO("CharacterLiteral")
    }

    // SECTION: lexicalIdentifiers
    //
    //fragment UnicodeDigit: UNICODE_CLASS_ND;
    fun UnicodeDigit(c: Char): Boolean {
        //'\u0030'..'\u0039' |
        //'\u0660'..'\u0669' |
        //'\u06F0'..'\u06F9' |
        //'\u07C0'..'\u07C9' |
        //'\u0966'..'\u096F' |
        //'\u09E6'..'\u09EF' |
        //'\u0A66'..'\u0A6F' |
        //'\u0AE6'..'\u0AEF' |
        //'\u0B66'..'\u0B6F' |
        //'\u0BE6'..'\u0BEF' |
        //'\u0C66'..'\u0C6F' |
        //'\u0CE6'..'\u0CEF' |
        //'\u0D66'..'\u0D6F' |
        //'\u0E50'..'\u0E59' |
        //'\u0ED0'..'\u0ED9' |
        //'\u0F20'..'\u0F29' |
        //'\u1040'..'\u1049' |
        //'\u1090'..'\u1099' |
        //'\u17E0'..'\u17E9' |
        //'\u1810'..'\u1819' |
        //'\u1946'..'\u194F' |
        //'\u19D0'..'\u19D9' |
        //'\u1A80'..'\u1A89' |
        //'\u1A90'..'\u1A99' |
        //'\u1B50'..'\u1B59' |
        //'\u1BB0'..'\u1BB9' |
        //'\u1C40'..'\u1C49' |
        //'\u1C50'..'\u1C59' |
        //'\uA620'..'\uA629' |
        //'\uA8D0'..'\uA8D9' |
        //'\uA900'..'\uA909' |
        //'\uA9D0'..'\uA9D9' |
        //'\uAA50'..'\uAA59' |
        //'\uABF0'..'\uABF9' |
        //'\uFF10'..'\uFF19';
        return c in '0'..'9'
    }

    //Identifier
    //    : (Letter | '_') (Letter | '_' | UnicodeDigit)*
    //    | '`' ~([\r\n] | '`')+ '`'
    //    ;
    fun Identifier(): String {
        var n = 0
        val c = peekChar()
        if (c == '`') {
            error("backticked identifier")
        }
        if (!(Letter(c) || c == '_')) {
            error("Not a valid identifier starting with '$c'")
        }
        while (hasMore) {
            n++
            val c = peekChar(n)
            if (!(Letter(c) || c == '_' || UnicodeDigit(c))) {
                break
            }
        }
        val str = peek(n)
        when (str) {
            "return", "for", "while", "do", "else", "when", "if", "super"
                -> error("not an identifier")
        }
        skip(n)
        return str
    }

    //IdentifierOrSoftKey
    //    : Identifier
    //    /* Soft keywords */
    //    | ABSTRACT
    //    | ANNOTATION
    //    | BY
    //    | CATCH
    //    | COMPANION
    //    | CONSTRUCTOR
    //    | CROSSINLINE
    //    | DATA
    //    | DYNAMIC
    //    | ENUM
    //    | EXTERNAL
    //    | FINAL
    //    | FINALLY
    //    | IMPORT
    //    | INFIX
    //    | INIT
    //    | INLINE
    //    | INNER
    //    | INTERNAL
    //    | LATEINIT
    //    | NOINLINE
    //    | OPEN
    //    | OPERATOR
    //    | OUT
    //    | OVERRIDE
    //    | PRIVATE
    //    | PROTECTED
    //    | PUBLIC
    //    | REIFIED
    //    | SEALED
    //    | TAILREC
    //    | VARARG
    //    | WHERE
    //    | GET
    //    | SET
    //    | FIELD
    //    | PROPERTY
    //    | RECEIVER
    //    | PARAM
    //    | SETPARAM
    //    | DELEGATE
    //    | FILE
    //    | EXPECT
    //    | ACTUAL
    //    | VALUE
    //    /* Strong keywords */
    //    | CONST
    //    | SUSPEND
    //    ;
    fun IdentifierOrSoftKey() {
        TODO("IdentifierOrSoftKey")
    }

    //FieldIdentifier
    //    : '$' IdentifierOrSoftKey
    //    ;
    fun FieldIdentifier() {
        TODO("FieldIdentifier")
    }

    //fragment UniCharacterLiteral
    //    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    //    ;
    fun UniCharacterLiteral() {
        TODO()
    }

    //fragment EscapedIdentifier
    //    : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | '$')
    //    ;
    fun EscapedIdentifier() {
        TODO()
    }

    //fragment EscapeSeq
    //    : UniCharacterLiteral
    //    | EscapedIdentifier
    //    ;
    fun EscapeSeq() {
        TODO()
    }

    // SECTION: characters
    //fragment Letter
    //    : UNICODE_CLASS_LU
    //    | UNICODE_CLASS_LL
    //    | UNICODE_CLASS_LT
    //    | UNICODE_CLASS_LM
    //    | UNICODE_CLASS_LO
    //    ;
    fun Letter(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z'
    }

    // SECTION: strings
    //QUOTE_OPEN: '"' -> pushMode(LineString);
    val QUOTE: String get() = "\""
    fun QUOTE_OPEN() {
        TODO()
    }

    //TRIPLE_QUOTE_OPEN: '"""' -> pushMode(MultiLineString);
    val TRIPLE_QUOTE: String get() = "\"\"\""
    fun TRIPLE_QUOTE_OPEN() {
        TODO()
    }

    //mode LineString;
    //
    //QUOTE_CLOSE
    //    : '"' -> popMode
    //    ;
    fun QUOTE_CLOSE() {
        TODO()
    }

    //LineStrRef
    //    : FieldIdentifier
    //    ;
    fun LineStrRef() {
        TODO()
    }

    //LineStrText
    //    : ~('\\' | '"' | '$')+ | '$'
    //    ;
    fun LineStrText() {
        TODO()
    }

    //LineStrEscapedChar
    //    : EscapedIdentifier
    //    | UniCharacterLiteral
    //    ;
    fun LineStrEscapedChar() {
        TODO()
    }

    //LineStrExprStart
    //    : '${' -> pushMode(DEFAULT_MODE)
    //    ;
    fun LineStrExprStart() {
        TODO()
    }

    //mode MultiLineString;
    //
    //TRIPLE_QUOTE_CLOSE
    //    : MultiLineStringQuote? '"""' -> popMode
    //    ;
    fun TRIPLE_QUOTE_CLOSE() {
        TODO()
    }

    //MultiLineStringQuote
    //    : '"'+
    //    ;
    fun MultiLineStringQuote() {
        TODO()
    }

    //MultiLineStrRef
    //    : FieldIdentifier
    //    ;
    fun MultiLineStrRef() {
        TODO()
    }

    //MultiLineStrText
    //    :  ~('"' | '$')+ | '$'
    //    ;
    fun MultiLineStrText() {
        TODO()
    }

    //MultiLineStrExprStart
    //    : '${' -> pushMode(DEFAULT_MODE)
    //    ;
    fun MultiLineStrExprStart() {
        TODO()
    }

// SECTION: inside
    //
    //mode Inside;
    //
    //Inside_RPAREN: RPAREN -> popMode, type(RPAREN);
    //Inside_RSQUARE: RSQUARE -> popMode, type(RSQUARE);
    //Inside_LPAREN: LPAREN -> pushMode(Inside), type(LPAREN);
    //Inside_LSQUARE: LSQUARE -> pushMode(Inside), type(LSQUARE);
    //Inside_LCURL: LCURL -> pushMode(DEFAULT_MODE), type(LCURL);
    //Inside_RCURL: RCURL -> popMode, type(RCURL);
    //
    //Inside_DOT: DOT -> type(DOT);
    //Inside_COMMA: COMMA  -> type(COMMA);
    //Inside_MULT: MULT -> type(MULT);
    //Inside_MOD: MOD  -> type(MOD);
    //Inside_DIV: DIV -> type(DIV);
    //Inside_ADD: ADD  -> type(ADD);
    //Inside_SUB: SUB  -> type(SUB);
    //Inside_INCR: INCR  -> type(INCR);
    //Inside_DECR: DECR  -> type(DECR);
    //Inside_CONJ: CONJ  -> type(CONJ);
    //Inside_DISJ: DISJ  -> type(DISJ);
    //Inside_EXCL_WS: '!' (Hidden|NL) -> type(EXCL_WS);
    //Inside_EXCL_NO_WS: EXCL_NO_WS  -> type(EXCL_NO_WS);
    //Inside_COLON: COLON  -> type(COLON);
    //Inside_SEMICOLON: SEMICOLON  -> type(SEMICOLON);
    //Inside_ASSIGNMENT: ASSIGNMENT  -> type(ASSIGNMENT);
    //Inside_ADD_ASSIGNMENT: ADD_ASSIGNMENT  -> type(ADD_ASSIGNMENT);
    //Inside_SUB_ASSIGNMENT: SUB_ASSIGNMENT  -> type(SUB_ASSIGNMENT);
    //Inside_MULT_ASSIGNMENT: MULT_ASSIGNMENT  -> type(MULT_ASSIGNMENT);
    //Inside_DIV_ASSIGNMENT: DIV_ASSIGNMENT  -> type(DIV_ASSIGNMENT);
    //Inside_MOD_ASSIGNMENT: MOD_ASSIGNMENT  -> type(MOD_ASSIGNMENT);
    //Inside_ARROW: ARROW  -> type(ARROW);
    //Inside_DOUBLE_ARROW: DOUBLE_ARROW  -> type(DOUBLE_ARROW);
    //Inside_RANGE: RANGE  -> type(RANGE);
    //Inside_RANGE_UNTIL: RANGE_UNTIL  -> type(RANGE_UNTIL);
    //Inside_RESERVED: RESERVED -> type(RESERVED);
    //Inside_COLONCOLON: COLONCOLON  -> type(COLONCOLON);
    //Inside_DOUBLE_SEMICOLON: DOUBLE_SEMICOLON  -> type(DOUBLE_SEMICOLON);
    //Inside_HASH: HASH  -> type(HASH);
    //Inside_AT_NO_WS: AT_NO_WS  -> type(AT_NO_WS);
    //Inside_AT_POST_WS: AT_POST_WS  -> type(AT_POST_WS);
    //Inside_AT_PRE_WS: AT_PRE_WS  -> type(AT_PRE_WS);
    //Inside_AT_BOTH_WS: AT_BOTH_WS  -> type(AT_BOTH_WS);
    //Inside_QUEST_WS: '?' (Hidden | NL) -> type(QUEST_WS);
    //Inside_QUEST_NO_WS: QUEST_NO_WS -> type(QUEST_NO_WS);
    //Inside_LANGLE: LANGLE  -> type(LANGLE);
    //Inside_RANGLE: RANGLE  -> type(RANGLE);
    //Inside_LE: LE  -> type(LE);
    //Inside_GE: GE  -> type(GE);
    //Inside_EXCL_EQ: EXCL_EQ  -> type(EXCL_EQ);
    //Inside_EXCL_EQEQ: EXCL_EQEQ  -> type(EXCL_EQEQ);
    //Inside_IS: IS -> type(IS);
    //Inside_NOT_IS: NOT_IS -> type(NOT_IS);
    //Inside_NOT_IN: NOT_IN -> type(NOT_IN);
    //Inside_AS: AS  -> type(AS);
    //Inside_AS_SAFE: AS_SAFE  -> type(AS_SAFE);
    //Inside_EQEQ: EQEQ  -> type(EQEQ);
    //Inside_EQEQEQ: EQEQEQ  -> type(EQEQEQ);
    //Inside_SINGLE_QUOTE: SINGLE_QUOTE  -> type(SINGLE_QUOTE);
    //Inside_AMP: AMP  -> type(AMP);
    //Inside_QUOTE_OPEN: QUOTE_OPEN -> pushMode(LineString), type(QUOTE_OPEN);
    //Inside_TRIPLE_QUOTE_OPEN: TRIPLE_QUOTE_OPEN -> pushMode(MultiLineString), type(TRIPLE_QUOTE_OPEN);
    //
    //Inside_VAL: VAL -> type(VAL);
    //Inside_VAR: VAR -> type(VAR);
    //Inside_FUN: FUN -> type(FUN);
    //Inside_OBJECT: OBJECT -> type(OBJECT);
    //Inside_SUPER: SUPER -> type(SUPER);
    //Inside_IN: IN -> type(IN);
    //Inside_OUT: OUT -> type(OUT);
    //Inside_FIELD: FIELD -> type(FIELD);
    //Inside_FILE: FILE -> type(FILE);
    //Inside_PROPERTY: PROPERTY -> type(PROPERTY);
    //Inside_GET: GET -> type(GET);
    //Inside_SET: SET -> type(SET);
    //Inside_RECEIVER: RECEIVER -> type(RECEIVER);
    //Inside_PARAM: PARAM -> type(PARAM);
    //Inside_SETPARAM: SETPARAM -> type(SETPARAM);
    //Inside_DELEGATE: DELEGATE -> type(DELEGATE);
    //Inside_THROW: THROW -> type(THROW);
    //Inside_RETURN: RETURN -> type(RETURN);
    //Inside_CONTINUE: CONTINUE -> type(CONTINUE);
    //Inside_BREAK: BREAK -> type(BREAK);
    //Inside_RETURN_AT: RETURN_AT -> type(RETURN_AT);
    //Inside_CONTINUE_AT: CONTINUE_AT -> type(CONTINUE_AT);
    //Inside_BREAK_AT: BREAK_AT -> type(BREAK_AT);
    //Inside_IF: IF -> type(IF);
    //Inside_ELSE: ELSE -> type(ELSE);
    //Inside_WHEN: WHEN -> type(WHEN);
    //Inside_TRY: TRY -> type(TRY);
    //Inside_CATCH: CATCH -> type(CATCH);
    //Inside_FINALLY: FINALLY -> type(FINALLY);
    //Inside_FOR: FOR -> type(FOR);
    //Inside_DO: DO -> type(DO);
    //Inside_WHILE: WHILE -> type(WHILE);
    //
    //Inside_PUBLIC: PUBLIC -> type(PUBLIC);
    //Inside_PRIVATE: PRIVATE -> type(PRIVATE);
    //Inside_PROTECTED: PROTECTED -> type(PROTECTED);
    //Inside_INTERNAL: INTERNAL -> type(INTERNAL);
    //Inside_ENUM: ENUM -> type(ENUM);
    //Inside_SEALED: SEALED -> type(SEALED);
    //Inside_ANNOTATION: ANNOTATION -> type(ANNOTATION);
    //Inside_DATA: DATA -> type(DATA);
    //Inside_INNER: INNER -> type(INNER);
    //Inside_VALUE: VALUE -> type(VALUE);
    //Inside_TAILREC: TAILREC -> type(TAILREC);
    //Inside_OPERATOR: OPERATOR -> type(OPERATOR);
    //Inside_INLINE: INLINE -> type(INLINE);
    //Inside_INFIX: INFIX -> type(INFIX);
    //Inside_EXTERNAL: EXTERNAL -> type(EXTERNAL);
    //Inside_SUSPEND: SUSPEND -> type(SUSPEND);
    //Inside_OVERRIDE: OVERRIDE -> type(OVERRIDE);
    //Inside_ABSTRACT: ABSTRACT -> type(ABSTRACT);
    //Inside_FINAL: FINAL -> type(FINAL);
    //Inside_OPEN: OPEN -> type(OPEN);
    //Inside_CONST: CONST -> type(CONST);
    //Inside_LATEINIT: LATEINIT -> type(LATEINIT);
    //Inside_VARARG: VARARG -> type(VARARG);
    //Inside_NOINLINE: NOINLINE -> type(NOINLINE);
    //Inside_CROSSINLINE: CROSSINLINE -> type(CROSSINLINE);
    //Inside_REIFIED: REIFIED -> type(REIFIED);
    //Inside_EXPECT: EXPECT -> type(EXPECT);
    //Inside_ACTUAL: ACTUAL -> type(ACTUAL);
    //
    //Inside_BooleanLiteral: BooleanLiteral -> type(BooleanLiteral);
    //Inside_IntegerLiteral: IntegerLiteral -> type(IntegerLiteral);
    //Inside_HexLiteral: HexLiteral -> type(HexLiteral);
    //Inside_BinLiteral: BinLiteral -> type(BinLiteral);
    //Inside_CharacterLiteral: CharacterLiteral -> type(CharacterLiteral);
    //Inside_RealLiteral: RealLiteral -> type(RealLiteral);
    //Inside_NullLiteral: NullLiteral -> type(NullLiteral);
    //Inside_LongLiteral: LongLiteral -> type(LongLiteral);
    //Inside_UnsignedLiteral: UnsignedLiteral -> type(UnsignedLiteral);
    //
    //Inside_Identifier: Identifier -> type(Identifier);
    //Inside_Comment: (LineComment | DelimitedComment) -> channel(HIDDEN);
    //Inside_WS: WS -> channel(HIDDEN);
    //Inside_NL: NL -> channel(HIDDEN);
    //
    //mode DEFAULT_MODE;
    //
    //ErrorCharacter: .;
}