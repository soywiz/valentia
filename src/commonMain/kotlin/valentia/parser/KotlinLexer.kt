package valentia.parser

import valentia.ast.BoolLiteralExpr
import valentia.ast.IntLiteralExpr
import valentia.ast.enrich

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
    fun ShebangLine(): Unit = TODO("ShebangLine")

    //DelimitedComment
    //    : '/*' ( DelimitedComment | . )*? '*/'
    //      -> channel(HIDDEN)
    //    ;
    fun DelimitedComment(): Unit = TODO("DelimitedComment")

    //LineComment
    //    : '//' ~[\r\n]*
    //      -> channel(HIDDEN)
    //    ;
    fun LineComment(): Unit = TODO("LineComment")

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
            when (peekChar()) {
                '\r', '\n' -> NL()
                else -> break@loop
            }
        }
    }

    //fragment Hidden: DelimitedComment | LineComment | WS;
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
    fun RESERVED(): Unit = TODO("...")

    //DOT: '.';
    //val _DOT = "."
    fun DOT(): String = expectAny(".")

    //COMMA: ',';
    //val _COMMA = ","
    fun COMMA(): Unit = TODO(",")

    //LPAREN: '(' -> pushMode(Inside);
    //val _LPAREN = "("
    fun LPAREN(): Unit = TODO("(")

    //RPAREN: ')';
    //val _RPAREN = ")"
    fun RPAREN(): Unit = TODO(")")

    //LSQUARE: '[' -> pushMode(Inside);
    //val _LSQUARE = "["
    fun LSQUARE(): Unit = TODO("[")

    //RSQUARE: ']';
    //val _RSQUARE = "]"
    fun RSQUARE(): Unit = TODO("]")

    //LCURL: '{' -> pushMode(DEFAULT_MODE);
    fun LCURL(): Unit = TODO("{")

    ///*
    // * When using another programming language (not Java) to generate a parser,
    // * please replace this code with the corresponding code of a programming language you are using.
    // */
    //RCURL: '}' { if (!_modeStack.isEmpty()) { popMode(); } };
    fun RCURL(): Unit = TODO("}")

    //MULT: '*';
    fun MULT(): Unit = TODO("*")

    //MOD: '%';
    fun MOD(): Unit = TODO("%")

    //DIV: '/';
    fun DIV(): Unit = TODO("/")

    //ADD: '+';
    fun ADD(): Unit = TODO("+")

    //SUB: '-';
    fun SUB(): Unit = TODO("-")

    //INCR: '++';
    fun INCR(): Unit = TODO("++")

    //DECR: '--';
    fun DECR(): Unit = TODO("--")

    //CONJ: '&&';
    fun CONJ(): String = expectAny("&&")

    //DISJ: '||';
    fun DISJ(): String = expectAny("||")

    //EXCL_WS: '!' Hidden;
    fun EXCL_WS(): Unit = TODO("! Hidden")

    //EXCL_NO_WS: '!';
    fun EXCL_NO_WS(): Unit = TODO("!")

    //COLON: ':';
    fun COLON(): Unit = TODO(":")

    //SEMICOLON: ';';
    fun SEMICOLON(): Unit = TODO(";")

    //ASSIGNMENT: '=';
    fun ASSIGNMENT(): Unit = TODO("=")

    //ADD_ASSIGNMENT: '+=';
    fun ADD_ASSIGNMENT(): Unit = TODO("+=")

    //SUB_ASSIGNMENT: '-=';
    fun SUB_ASSIGNMENT(): Unit = TODO("-=")

    //MULT_ASSIGNMENT: '*=';
    fun MULT_ASSIGNMENT(): Unit = TODO("*=")

    //DIV_ASSIGNMENT: '/=';
    fun DIV_ASSIGNMENT(): Unit = TODO("/=")

    //MOD_ASSIGNMENT: '%=';
    fun MOD_ASSIGNMENT(): Unit = TODO("%=")

    //ARROW: '->';
    fun ARROW(): Unit = TODO("->")

    //DOUBLE_ARROW: '=>';
    fun DOUBLE_ARROW(): Unit = TODO("=>")

    //RANGE: '..';
    fun RANGE(): Unit = TODO("..")

    //RANGE_UNTIL: '..<';
    fun RANGE_UNTIL(): Unit = TODO("--<")

    //COLONCOLON: '::';
    fun COLONCOLON(): Unit = TODO("::")

    //DOUBLE_SEMICOLON: ';;';
    fun DOUBLE_SEMICOLON(): Unit = TODO(";;")

    //HASH: '#';
    fun HASH(): Unit = TODO("#")

    //AT_NO_WS: '@';
    fun AT_NO_WS(): Unit = TODO("@")

    //AT_POST_WS: '@' (Hidden | NL);
    fun AT_POST_WS(): Unit = TODO("@ (Hidden | NL)")

    //AT_PRE_WS: (Hidden | NL) '@' ;
    fun AT_PRE_WS(): Unit = TODO()

    //AT_BOTH_WS: (Hidden | NL) '@' (Hidden | NL);
    fun AT_BOTH_WS(): Unit = TODO()

    //QUEST_WS: '?' Hidden;
    fun QUEST_WS(): Unit = TODO()

    //QUEST_NO_WS: '?';
    fun QUEST_NO_WS(): Unit = TODO("?")

    //LANGLE: '<';
    fun LANGLE(): Unit = TODO("<")

    //RANGLE: '>';
    fun RANGLE(): Unit = TODO(">")

    //LE: '<=';
    fun LE(): Unit = TODO("<=")

    //GE: '>=';
    fun GE(): Unit = TODO(">=")

    //EXCL_EQ: '!=';
    fun EXCL_EQ(): Unit = TODO("!=")

    //EXCL_EQEQ: '!==';
    fun EXCL_EQEQ(): Unit = TODO("!==")

    //AS_SAFE: 'as?';
    fun AS_SAFE(): Unit = TODO("as?")

    //EQEQ: '==';
    fun EQEQ(): Unit = TODO("==")

    //EQEQEQ: '===';
    fun EQEQEQ(): Unit = TODO("===")

    //SINGLE_QUOTE: '\'';
    fun SINGLE_QUOTE(): Unit = TODO("\'")

    //AMP: '&';
    fun AMP(): Unit = TODO("&")

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
    fun FILE(): Unit = TODO("file")

    //FIELD: 'field';
    fun FIELD(): Unit = TODO("field")

    //PROPERTY: 'property';
    fun PROPERTY(): Unit = TODO()

    //GET: 'get';
    fun GET(): Unit = TODO()

    //SET: 'set';
    fun SET(): Unit = TODO()

    //RECEIVER: 'receiver';
    fun RECEIVER(): Unit = TODO()

    //PARAM: 'param';
    fun PARAM(): Unit = TODO()

    //SETPARAM: 'setparam';
    fun SETPARAM(): Unit = TODO()

    //DELEGATE: 'delegate';
    fun DELEGATE(): Unit = TODO()

    //PACKAGE: 'package';
    fun PACKAGE(): Unit = TODO()

    //IMPORT: 'import';
    fun IMPORT(): Unit = TODO()

    //CLASS: 'class';
    fun CLASS(): Unit = TODO()

    //INTERFACE: 'interface';
    fun INTERFACE(): Unit = TODO()

    //FUN: 'fun';
    fun FUN(): Unit = TODO()

    //OBJECT: 'object';
    fun OBJECT(): Unit = TODO()

    //VAL: 'val';
    fun VAL(): Unit = TODO()

    //VAR: 'var';
    fun VAR(): Unit = TODO()

    //TYPE_ALIAS: 'typealias';
    fun TYPE_ALIAS(): Unit = TODO()

    //CONSTRUCTOR: 'constructor';
    fun CONSTRUCTOR(): Unit = TODO()

    //BY: 'by';
    fun BY(): Unit = TODO()

    //COMPANION: 'companion';
    fun COMPANION(): Unit = TODO()

    //INIT: 'init';
    fun INIT(): Unit = TODO()

    //THIS: 'this';
    fun THIS(): Unit = TODO()

    //SUPER: 'super';
    fun SUPER(): Unit = TODO()

    //TYPEOF: 'typeof';
    fun TYPEOF(): Unit = TODO()

    //WHERE: 'where';
    fun WHERE(): Unit = TODO()

    //IF: 'if';
    fun IF(): Unit = TODO()

    //ELSE: 'else';
    fun ELSE(): Unit = TODO()

    //WHEN: 'when';
    fun WHEN(): Unit = TODO()

    //TRY: 'try';
    fun TRY(): Unit = TODO()

    //CATCH: 'catch';
    fun CATCH(): Unit = TODO()

    //FINALLY: 'finally';
    fun FINALLY(): Unit = TODO()

    //FOR: 'for';
    fun FOR(): Unit = TODO()

    //DO: 'do';
    fun DO(): Unit = TODO()

    //WHILE: 'while';
    fun WHILE(): Unit = TODO()

    //THROW: 'throw';
    fun THROW(): Unit = TODO()

    //RETURN: 'return';
    fun RETURN(): Unit = TODO()

    //CONTINUE: 'continue';
    fun CONTINUE(): Unit = TODO()

    //BREAK: 'break';
    fun BREAK(): Unit = TODO()

    //AS: 'as';
    fun AS(): Unit = TODO()

    //IS: 'is';
    fun IS(): Unit = TODO()

    //IN: 'in';
    fun IN(): Unit = TODO()

    //NOT_IS: '!is' (Hidden | NL);
    fun NOT_IS(): Unit = TODO()

    //NOT_IN: '!in' (Hidden | NL);
    fun NOT_IN(): Unit = TODO()

    //OUT: 'out';
    fun OUT(): Unit = TODO()

    //DYNAMIC: 'dynamic';
    fun DYNAMIC(): Unit = TODO()

    // SECTION: lexicalModifiers
    //PUBLIC: 'public';
    fun PUBLIC(): Unit = TODO()

    //PRIVATE: 'private';
    fun PRIVATE(): Unit = TODO()

    //PROTECTED: 'protected';
    fun PROTECTED(): Unit = TODO()

    //INTERNAL: 'internal';
    fun INTERNAL(): Unit = TODO()

    //ENUM: 'enum';
    fun ENUM(): Unit = TODO()

    //SEALED: 'sealed';
    fun SEALED(): Unit = TODO()

    //ANNOTATION: 'annotation';
    fun ANNOTATION(): Unit = TODO()

    //DATA: 'data';
    fun DATA(): Unit = TODO()

    //INNER: 'inner';
    fun INNER(): Unit = TODO()

    //VALUE: 'value';
    fun VALUE(): Unit = TODO()

    //TAILREC: 'tailrec';
    fun TAILREC(): Unit = TODO()

    //OPERATOR: 'operator';
    fun OPERATOR(): Unit = TODO()

    //INLINE: 'inline';
    fun INLINE(): Unit = TODO()

    //INFIX: 'infix';
    fun INFIX(): Unit = TODO()

    //EXTERNAL: 'external';
    fun EXTERNAL(): Unit = TODO()

    //SUSPEND: 'suspend';
    fun SUSPEND(): Unit = TODO()

    //OVERRIDE: 'override';
    fun OVERRIDE(): Unit = TODO()

    //ABSTRACT: 'abstract';
    fun ABSTRACT(): Unit = TODO()

    //FINAL: 'final';
    fun FINAL(): Unit = TODO()

    //OPEN: 'open';
    fun OPEN(): Unit = TODO()

    //CONST: 'const';
    fun CONST(): Unit = TODO()

    //LATEINIT: 'lateinit';
    fun LATEINIT(): Unit = TODO()

    //VARARG: 'vararg';
    fun VARARG(): Unit = TODO()

    //NOINLINE: 'noinline';
    fun NOINLINE(): Unit = TODO()

    //CROSSINLINE: 'crossinline';
    fun CROSSINLINE(): Unit = TODO()

    //REIFIED: 'reified';
    fun REIFIED(): Unit = TODO()

    //EXPECT: 'expect';
    fun EXPECT(): Unit = TODO("expect")

    //ACTUAL: 'actual';
    fun ACTUAL(): Unit = TODO("actual")

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
    fun IntegerLiteral(): IntLiteralExpr? {
        println("TODO=IntegerLiteral")
        val c = peekChar()
        if (c == '0') return IntLiteralExpr(0)
        if (DecDigit(c)) {
            var n = 0
            while (hasMore) {
                n++
                if (!DecDigitOrSeparator(peekChar(n))) {
                    break
                }
            }
            val spos = pos
            val str = read(n).replace("_", "").toInt()
            return IntLiteralExpr(str).enrich(this, spos)
        }
        TODO("IntegerLiteral")
    }

    //fragment HexDigit: [0-9a-fA-F];
    fun HexDigit(c: Char): Boolean = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

    //fragment HexDigitOrSeparator: HexDigit | '_';
    fun HexDigitOrSeparator(c: Char): Boolean = c == '_' || HexDigit(c)

    //
    //HexLiteral
    //    : '0' [xX] HexDigit HexDigitOrSeparator* HexDigit
    //    | '0' [xX] HexDigit
    //    ;
    fun HexLiteral(c: Char): Unit = TODO("HexLiteral")

    //fragment BinDigit: [01];
    fun BinDigit(c: Char): Boolean = c in '0'..'1'

    //fragment BinDigitOrSeparator: BinDigit | '_';
    fun BinDigitOrSeparator(c: Char): Boolean = c == '_' || BinDigit(c)

    //BinLiteral
    //    : '0' [bB] BinDigit BinDigitOrSeparator* BinDigit
    //    | '0' [bB] BinDigit
    //    ;
    fun BinLiteral() {
        TODO("BinLiteral")
    }

    //UnsignedLiteral
    //    : (IntegerLiteral | HexLiteral | BinLiteral) [uU] [lL]?
    //    ;
    fun UnsignedLiteral() {
        TODO("UnsignedLiteral")
    }

    //LongLiteral
    //    : (IntegerLiteral | HexLiteral | BinLiteral) [lL]
    //    ;
    fun LongLiteral() {
        TODO("LongLiteral")
    }

    //BooleanLiteral: 'true'| 'false';
    fun BooleanLiteral(): BoolLiteralExpr? {
        val res = expectAnyOpt("true", "false") ?: return null
        return BoolLiteralExpr(res == "true")
    }

    //NullLiteral: 'null';
    fun NullLiteral() {
        TODO("NullLiteral")
    }

    //CharacterLiteral
    //    : '\'' (EscapeSeq | ~[\n\r'\\]) '\''
    //    ;
    fun CharacterLiteral() {
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
            TODO("backticked identifier")
        }
        if (!(Letter(c) || c == '_')) {
            TODO("Not a valid identifier starting with '$c'")
        }
        while (hasMore) {
            n++
            val c = peekChar(n)
            if (!(Letter(c) || c == '_' || UnicodeDigit(c))) {
                break
            }
        }
        return read(n)
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
    fun QUOTE_OPEN() {
        TODO()
    }

    //TRIPLE_QUOTE_OPEN: '"""' -> pushMode(MultiLineString);
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