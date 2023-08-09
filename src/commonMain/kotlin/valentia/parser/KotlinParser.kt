package valentia.parser

import valentia.ast.*

interface KotlinParser : KotlinLexer {
    /**
     * Kotlin syntax grammar in ANTLR4 notation
     */

    // parser grammar KotlinParser;

    // options { tokenVocab = KotlinLexer; }

// SECTION: general

    // kotlinFile
    //    : shebangLine? NL* fileAnnotation* packageHeader importList topLevelObject* EOF
    //    ;
    fun kotlinFile(): FileNode {
        val shebang = opt { shebangLine() }
        NLs()
        val fileAnnotations = zeroOrMore { fileAnnotation() }
        val _package = packageHeader()
        val imports = importList()
        val topLevelDecls = zeroOrMore { topLevelObject() }
        EOF()
        return FileNode(
            shebang = shebang,
            _package = _package,
            fileAnnotations = fileAnnotations,
            imports = imports,
            topLevelDecls = topLevelDecls,
        )
    }

    // script
    //    : shebangLine? NL* fileAnnotation* packageHeader importList (statement semi)* EOF
    //    ;
    fun script() {
        opt { shebangLine() }
        NLs()
        zeroOrMore { fileAnnotation() }
        packageHeader()
        importList()
        zeroOrMore {
            statement()
            semi()
        }
        EOF()
    }

    // shebangLine
    //    : ShebangLine NL+
    //    ;
    fun shebangLine(): String? {
        return ShebangLine().also { NLs() }
    }

    // fileAnnotation
    //    : (AT_NO_WS | AT_PRE_WS) FILE NL* COLON NL* (LSQUARE unescapedAnnotation+ RSQUARE | unescapedAnnotation) NL*
    //    ;
    fun fileAnnotation(): AnnotationNodes {
        return annotation()
    }

    // packageHeader
    //    : (PACKAGE identifier semi?)?
    //    ;
    fun packageHeader(): Identifier? = when {
        expectOpt("package") -> identifier().also { semiOpt() }
        else -> null
    }

    // importList
    //    : importHeader*
    //    ;
    fun importList(): List<ImportNode> {
        return zeroOrMore { importHeader() }
    }

    // importHeader
    //    : IMPORT identifier (DOT MULT | importAlias)? semi?
    //    ;
    fun importHeader(): ImportNode {
        expect("import")
        val id = identifier()
        var all = false
        var alias: String? = null
        opt {
            OR({
                expect(".*")
                all = true
            }, {
                alias = importAlias()
            })
        }
        semiOpt()
        return ImportNode(id, alias, all)
    }

    // importAlias
    //    : AS simpleIdentifier
    //    ;
    fun importAlias(): String {
        Hidden()
        expect("as")
        Hidden()
        return simpleIdentifier()
    }

    // topLevelObject
    //    : declaration semis?
    //    ;
    fun topLevelObject(): DeclNode {
        return declaration().also {
            semis(atLeastOne = false)
        }
    }

    // typeAlias
    //    : modifiers? TYPE_ALIAS NL* simpleIdentifier (NL* typeParameters)? NL* ASSIGNMENT NL* type
    //    ;
    fun typeAlias(): DeclNode {
        opt { modifiers() }
        expect("typealias")
        NLs()
        simpleIdentifier()
        opt {
            NLs()
            typeParameters()
        }
        NLs()
        assignment()
        NLs()
        type()
        TODO()
    }

    // declaration
    //    : classDeclaration
    //    | objectDeclaration
    //    | functionDeclaration
    //    | propertyDeclaration
    //    | typeAlias
    //    ;
    fun declaration(): DeclNode {
        return OR(
            { classDeclaration() },
            { objectDeclaration() },
            { functionDeclaration() },
            { propertyDeclaration() },
            { typeAlias() },
        )
    }

// SECTION: classes

    // classDeclaration
    //    : modifiers? (CLASS | (FUN NL*)? INTERFACE) NL* simpleIdentifier
    //      (NL* typeParameters)? (NL* primaryConstructor)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* typeConstraints)?
    //      (NL* classBody | NL* enumClassBody)?
    //    ;
    fun classDeclaration(): DeclNode {
        opt { modifiers() }
        OR(
            { expect("class") },
            {
                val isFun = expectOpt("fun")
                NLs()
                expect("interface")
            },
        )
        NLs()
        val className = simpleIdentifier()
        TODO("classDeclaration")
    }

    // primaryConstructor
    //    : (modifiers? CONSTRUCTOR NL*)? classParameters
    //    ;
    fun primaryConstructor() {
        TODO("primaryConstructor")
    }

    //classBody
    //    : LCURL NL* classMemberDeclarations NL* RCURL
    //    ;
    fun classBody() {
        TODO("classBody")
    }

    //classParameters
    //    : LPAREN NL* (classParameter (NL* COMMA NL* classParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    //
    fun classParameters() {
        TODO("classParameters")
    }

    //classParameter
    //    : modifiers? (VAL | VAR)? NL* simpleIdentifier COLON NL* type (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun classParameter() {
        TODO("classParameter")
    }

    //delegationSpecifiers
    //    : annotatedDelegationSpecifier (NL* COMMA NL* annotatedDelegationSpecifier)*
    //    ;
    fun delegationSpecifiers() {
        TODO("delegationSpecifiers")
    }

    //delegationSpecifier
    //    : constructorInvocation
    //    | explicitDelegation
    //    | userType
    //    | functionType
    //    | SUSPEND NL* functionType
    //    ;
    fun delegationSpecifier() {
        TODO("delegationSpecifier")
    }

    //constructorInvocation
    //    : userType NL* valueArguments
    //    ;
    fun constructorInvocation() {
        TODO()
    }

    //annotatedDelegationSpecifier
    //    : annotation* NL* delegationSpecifier
    //    ;
    fun annotatedDelegationSpecifier() {
        TODO()
    }

    //explicitDelegation
    //    : (userType | functionType) NL* BY NL* expression
    //    ;
    fun explicitDelegation() {
        TODO()
    }

    //typeParameters
    //    : LANGLE NL* typeParameter (NL* COMMA NL* typeParameter)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeParameters() {
        TODO()
    }

    //typeParameter
    //    : typeParameterModifiers? NL* simpleIdentifier (NL* COLON NL* type)?
    //    ;
    fun typeParameter() {
        TODO()
    }

    //typeConstraints
    //    : WHERE NL* typeConstraint (NL* COMMA NL* typeConstraint)*
    //    ;
    fun typeConstraints() {
        TODO()
    }

    //typeConstraint
    //    : annotation* simpleIdentifier NL* COLON NL* type
    //    ;
    fun typeConstraint() {
        TODO()
    }

// SECTION: classMembers

    //classMemberDeclarations
    //    : (classMemberDeclaration semis?)*
    //    ;
    fun classMemberDeclarations() {
        TODO()
    }

    //classMemberDeclaration
    //    : declaration
    //    | companionObject
    //    | anonymousInitializer
    //    | secondaryConstructor
    //    ;
    fun classMemberDeclaration() {
        TODO()
    }

    //anonymousInitializer
    //    : INIT NL* block
    //    ;
    fun anonymousInitializer() {
        TODO()
    }

    //companionObject
    //    : modifiers? COMPANION NL* DATA? NL* OBJECT
    //      (NL* simpleIdentifier)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun companionObject() {
        TODO()
    }

    //functionValueParameters
    //    : LPAREN NL* (functionValueParameter (NL* COMMA NL* functionValueParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    fun functionValueParameters() {
        TODO()
    }

    //functionValueParameter
    //    : parameterModifiers? parameter (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameter() {
        TODO()
    }

    //functionDeclaration
    //    : modifiers?
    //      FUN (NL* typeParameters)? (NL* receiverType NL* DOT)? NL* simpleIdentifier
    //      NL* functionValueParameters
    //      (NL* COLON NL* type)?
    //      (NL* typeConstraints)?
    //      (NL* functionBody)?
    //    ;
    fun functionDeclaration(): DeclNode {
        TODO()
    }

    //functionBody
    //    : block
    //    | ASSIGNMENT NL* expression
    //    ;
    fun functionBody() {
        TODO()
    }

    //variableDeclaration
    //    : annotation* NL* simpleIdentifier (NL* COLON NL* type)?
    //    ;
    fun variableDeclaration(): VariableDecl {
        zeroOrMore { annotation() }
        val id = simpleIdentifier()
        var type: TypeNode? = null
        opt {
            NLs()
            COLON()
            NLs()
            type = type()
        }
        return VariableDecl(id, type)
    }

    //multiVariableDeclaration
    //    : LPAREN NL* variableDeclaration (NL* COMMA NL* variableDeclaration)* (NL* COMMA)? NL* RPAREN
    //    ;
    fun multiVariableDeclaration(): VariableDecls {
        val vars = arrayListOf<VariableDecl>()
        expectAndRecover("(", ")") {
            NLs()
            vars += variableDeclaration()
            while (hasMore) {
                NLs()
                if (expectOpt(",")) {
                    NLs()
                    if (expectOpt(")")) {
                        skip(-1)
                        break
                    }
                    vars += variableDeclaration()
                }
            }
        }
        return VariableDecls(vars)
    }

    fun variableDeclarationOrMultiVariableDeclaration(): VariableDecls {
        return OR({ VariableDecls(variableDeclaration()) }, { multiVariableDeclaration() })
    }

    //propertyDeclaration
    //    : modifiers? (VAL | VAR)
    //      (NL* typeParameters)?
    //      (NL* receiverType NL* DOT)?
    //      (NL* (multiVariableDeclaration | variableDeclaration))
    //      (NL* typeConstraints)?
    //      (NL* (ASSIGNMENT NL* expression | propertyDelegate))?
    //      (NL* SEMICOLON)? NL* (getter? (NL* semi? setter)? | setter? (NL* semi? getter)?)
    //    ;
    fun propertyDeclaration(): DeclNode {
        TODO()
    }

    //propertyDelegate
    //    : BY NL* expression
    //    ;
    fun propertyDelegate() {
        TODO()
    }

    //getter
    //    : modifiers? GET
    //      (NL* LPAREN NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun getter() {
        TODO()
    }

    //setter
    //    : modifiers? SET
    //      (NL* LPAREN NL* functionValueParameterWithOptionalType (NL* COMMA)? NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun setter() {
        TODO()
    }

    //parametersWithOptionalType
    //    : LPAREN NL* (functionValueParameterWithOptionalType (NL* COMMA NL* functionValueParameterWithOptionalType)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    fun parametersWithOptionalType() {
        TODO()
    }

    //functionValueParameterWithOptionalType
    //    : parameterModifiers? parameterWithOptionalType (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameterWithOptionalType() {
        TODO()
    }

    //parameterWithOptionalType
    //    : simpleIdentifier NL* (COLON NL* type)?
    //    ;
    fun parameterWithOptionalType() {
        TODO()
    }

    //parameter
    //    : simpleIdentifier NL* COLON NL* type
    //    ;
    fun parameter() {
        TODO()
    }

    //objectDeclaration
    //    : modifiers? OBJECT
    //      NL* simpleIdentifier
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun objectDeclaration(): DeclNode {
        opt { modifiers() }
        expect("object")
        NLs()
        simpleIdentifier()
        opt {
            NLs()
            COLON()
            NLs()
            delegationSpecifiers()
        }
        opt {
            NLs()
            classBody()
        }
        TODO()
    }

    //secondaryConstructor
    //    : modifiers? CONSTRUCTOR NL* functionValueParameters (NL* COLON NL* constructorDelegationCall)? NL* block?
    //    ;
    fun secondaryConstructor() {
        TODO()
    }

    //constructorDelegationCall
    //    : (THIS | SUPER) NL* valueArguments
    //    ;
    fun constructorDelegationCall() {
        TODO()
    }

    // SECTION: enumClasses
    //enumClassBody
    //    : LCURL NL* enumEntries? (NL* SEMICOLON NL* classMemberDeclarations)? NL* RCURL
    //    ;
    fun enumClassBody() {
        TODO()
    }

    //enumEntries
    //    : enumEntry (NL* COMMA NL* enumEntry)* NL* COMMA?
    //    ;
    fun enumEntries() {
        TODO()
    }

    //enumEntry
    //    : (modifiers NL*)? simpleIdentifier (NL* valueArguments)? (NL* classBody)?
    //    ;
    fun enumEntry() {
        TODO()
    }

    // SECTION: types
    //type
    //    : typeModifiers? (functionType | parenthesizedType | nullableType | typeReference | definitelyNonNullableType)
    //    ;
    fun type(): TypeNode {
        Hidden()
        println("TODO: type")
        zeroOrMore { typeModifier() }
        //return typeReference()
        return OR(
            { functionType() },
            { nullableType() },
            { parenthesizedType() },
            { typeReference() },
            { definitelyNonNullableType() },
        )
    }

    //typeReference
    //    : userType
    //    | DYNAMIC
    //    ;
    fun typeReference(): TypeNode {
        if (expectOpt("dynamic")) return DynamicType
        return userType()
    }

    //nullableType
    //    : (typeReference | parenthesizedType) NL* quest+
    //    ;
    fun nullableType(): TypeNode {
        val type = OR({ typeReference() }, { parenthesizedType() })
        NLs()
        expect("?")
        do {
            Hidden()
        } while (expectOpt("?"))
        return type.nullable()
    }

    //quest
    //    : QUEST_NO_WS
    //    | QUEST_WS
    //    ;
    fun quest() {
        QUEST_WS()
        TODO()
    }

    //userType
    //    : simpleUserType (NL* DOT NL* simpleUserType)*
    //    ;
    fun userType(): TypeNode {
        println("TODO: userType")
        val res = simpleUserType()
        zeroOrMore {
            NLs()
            DOT()
            NLs()
            simpleUserType()
        }
        return res
    }

    //simpleUserType
    //    : simpleIdentifier (NL* typeArguments)?
    //    ;
    fun simpleUserType(): SimpleType {
        println("TODO: simpleUserType")
        val id = simpleIdentifier()
        NLs()
        if (peekChar() == '<') {
            typeArguments()
        }
        return SimpleType(id)
    }

    //typeProjection
    //    : typeProjectionModifiers? type
    //    | MULT
    //    ;
    fun typeProjection() {
        TODO()
    }

    //typeProjectionModifiers
    //    : typeProjectionModifier+
    //    ;
    fun typeProjectionModifiers() {
        TODO()
    }

    //typeProjectionModifier
    //    : varianceModifier NL*
    //    | annotation
    //    ;
    fun typeProjectionModifier() {
        TODO("typeProjectionModifier")
    }

    //functionType
    //    : (receiverType NL* DOT NL*)? functionTypeParameters NL* ARROW NL* type
    //    ;
    fun functionType(): TypeNode {
        TODO("functionType")
    }

    //functionTypeParameters
    //    : LPAREN NL* (parameter | type)? (NL* COMMA NL* (parameter | type))* (NL* COMMA)? NL* RPAREN
    //    ;
    fun functionTypeParameters() {
        TODO("functionTypeParameters")
    }

    //parenthesizedType
    //    : LPAREN NL* type NL* RPAREN
    //    ;
    fun parenthesizedType(): TypeNode {
        return expectAndRecover("(", ")") {
            NLs()
            val res = type()
            NLs()
            res
        } ?: UnknownType
    }

    //receiverType
    //    : typeModifiers? (parenthesizedType | nullableType | typeReference)
    //    ;
    fun receiverType() {
        TODO()
    }

    //parenthesizedUserType
    //    : LPAREN NL* (userType | parenthesizedUserType) NL* RPAREN
    //    ;
    fun parenthesizedUserType() {
        TODO()
    }

    //definitelyNonNullableType
    //    : typeModifiers? (userType | parenthesizedUserType) NL* AMP NL* typeModifiers? (userType | parenthesizedUserType)
    //    ;
    fun definitelyNonNullableType(): TypeNode {
        TODO("definitelyNonNullableType")
    }

// SECTION: statements

    //statements
    //    : (statement (semis statement)*)? semis?
    //    ;
    fun statements(): List<Stm> {
        val out = arrayListOf<Stm>()
        out += statement()
        zeroOrMore {
            semis()
            out += statement()
        }
        semis(atLeastOne = false)
        return out
    }

    //statement
    //    : (label | annotation)* ( declaration | assignment | loopStatement | expression)
    //    ;
    fun statement(): Stm {
        println("TODO: statement")
        zeroOrMore {
            OR({ label() }, { annotation() })
        }
        OR({ declaration() }, { assignment() }, { loopStatement() }, { expression() })
        TODO()
    }

    //label
    //    : simpleIdentifier (AT_NO_WS | AT_POST_WS) NL*
    //    ;
    fun label(): String {
        val id = simpleIdentifier()
        expect("@")
        NLs()
        return id
    }

    //controlStructureBody
    //    : block
    //    | statement
    //    ;
    fun controlStructureBody(): Stm {
        Hidden()
        return when {
            peekChar() == '{' -> block()
            else -> statement()
        }
    }

    //block
    //    : LCURL NL* statements NL* RCURL
    //    ;
    fun block(): Stm {
        return expectAndRecoverSure("{", "}") {
            NLs()
            Stms(statements()).also { NLs() }
        }
    }

    // loopStatement
    //    : forStatement
    //    | whileStatement
    //    | doWhileStatement
    //    ;
    fun loopStatement(): LoopStm = when (peekIdentifier()) {
        "for" -> forStatement()
        "while" -> whileStatement()
        "do" -> doWhileStatement()
        else -> unexpected("loop")
    }

    //forStatement
    //    : FOR NL* LPAREN annotation* (variableDeclaration | multiVariableDeclaration)
    //    IN expression RPAREN NL* controlStructureBody?
    //    ;
    fun forStatement(): ForLoopStm = enrich {
        expect("for")
        NLs()
        var annotations: List<Node> = emptyList()
        var expr: Expr? = null
        var vardecl: VariableDecls? = null
        expectAndRecover("(", ")") {
            annotations = annotations()
            vardecl = variableDeclarationOrMultiVariableDeclaration()
            expect("in")
            expr = expression()
        }
        NLs()
        val body = opt { controlStructureBody() }
        //val body = controlStructureBody()
        ForLoopStm(expr, vardecl, body, annotations)
    }

    // whileStatement
    //    : WHILE NL* LPAREN expression RPAREN NL* (controlStructureBody | SEMICOLON)
    //    ;
    fun whileStatement(): WhileLoopStm = enrich {
        expect("while")
        Hidden()
        NLs()
        Hidden()
        val cond = expectAndRecover("(", ")") { expression() }
        Hidden()
        NLs()
        Hidden()
        val body: Stm = OR({ expect(";"); EmptyStm() }, { controlStructureBody() })
        Hidden()
        WhileLoopStm(cond, body)
    }

    // doWhileStatement
    //    : DO NL* controlStructureBody? NL* WHILE NL* LPAREN expression RPAREN
    //    ;
    fun doWhileStatement(): DoWhileLoopStm = enrich {
        expect("do")
        NLs()
        val body = opt { controlStructureBody() }
        NLs()
        expect("while")
        NLs()
        val cond = expectAndRecover("(", ")") {
            expression()
        }
        DoWhileLoopStm(body, cond)
    }

    //assignment
    //    : (directlyAssignableExpression ASSIGNMENT | assignableExpression assignmentAndOperator) NL* expression
    //    ;
    fun assignment() {
        TODO()
    }

    //semi
    //    : (SEMICOLON | NL) NL*
    //    ;
    fun semi() {
        val c = peekChar()
        if (c == ';') {
            skip(1)
        } else if (c == '\r' || c == '\n') {
            NLs()
        } else {
            TODO("semi")
        }
        while (peekChar() == '\r' || peekChar() == '\n') {
            NLs()
        }
    }

    fun semiOpt() {
        opt { semi() }
    }

    //semis
    //    : (SEMICOLON | NL)+
    //    ;
    fun semis(atLeastOne: Boolean = true) {
        var count = 0
        while (true) {
            Hidden()
            when (peekChar()) {
                ';' -> expectChar(';')
                '\n', '\r' -> NL()
                else -> break
            }
            count++
            Hidden()
        }
        if (count == 0 && atLeastOne) error("Expected at least one semicolon or new line")
    }

// SECTION: expressions

    private inline fun binop(op: () -> String, next: () -> Expr, initialNLs: Boolean = true): Expr {
        val spos = pos
        val ops = arrayListOf<String>()
        val exprs = arrayListOf<Expr>()
        Hidden()
        exprs += next()
        Hidden()
        zeroOrMore {
            Hidden()
            if (initialNLs) NLs()
            Hidden()
            val op = op()
            Hidden()
            NLs()
            Hidden()
            val expr = next()
            Hidden()
            ops += op
            exprs += expr
        }
        if (ops.isEmpty()) {
            check(exprs.size == 1)
            return exprs.first()
        }
        return OpSeparatedExprs(ops, exprs).enrich(this, spos)
    }

    //expression
    //    : disjunction
    //    ;
    fun expression(): Expr = disjunction()

    //disjunction
    //    : conjunction (NL* DISJ NL* conjunction)*
    //    ;
    fun disjunction(): Expr = binop({ DISJ() }, { conjunction() })

    //conjunction
    //    : equality (NL* CONJ NL* equality)*
    //    ;
    fun conjunction(): Expr = binop({ CONJ() }, { equality() })

    //equality
    //    : comparison (equalityOperator NL* comparison)*
    //    ;
    fun equality(): Expr = binop({ equalityOperator() }, { comparison() }, initialNLs = false)

    //comparison
    //    : genericCallLikeComparison (comparisonOperator NL* genericCallLikeComparison)*
    //    ;
    fun comparison(): Expr = binop({ comparisonOperator() }, { genericCallLikeComparison() }, initialNLs = false)

    //genericCallLikeComparison
    //    : infixOperation callSuffix*
    //    ;
    fun genericCallLikeComparison(): Expr {
        val res = infixOperation()
        zeroOrMore { callSuffix() }
        println("TODO: genericCallLikeComparison")
        return res
    }

    //infixOperation
    //    : elvisExpression (inOperator NL* elvisExpression | isOperator NL* type)*
    //    ;
    fun infixOperation(): Expr {
        var res: Expr = elvisExpression()
        loop@while (true) {
            val op = expectAnyOpt("!in", "in", "!is", "is") ?: break
            NLs()
            res = when (op) {
                "!in", "in" -> RangeTestExpr(res, op, elvisExpression())
                "!is", "is" -> TypeTestExpr(res, op, type())
                else -> unexpected()
            }
        }
        return res
    }

    //elvisExpression
    //    : infixFunctionCall (NL* elvis NL* infixFunctionCall)*
    //    ;
    fun elvisExpression(): Expr = binop({ expectAny("?:") }, { infixFunctionCall() })

    //elvis
    //    : QUEST_NO_WS COLON
    //    ;
    //fun elvis() = expect("?:")

    //infixFunctionCall
    //    : rangeExpression (simpleIdentifier NL* rangeExpression)*
    //    ;
    fun infixFunctionCall(): Expr {
        return binop({ simpleIdentifier() }, { rangeExpression() })
    }

    //rangeExpression
    //    : additiveExpression ((RANGE | RANGE_UNTIL) NL* additiveExpression)*
    //    ;
    fun rangeExpression(): Expr {
        return binop({ expectAny("..<", "..") }, { additiveExpression() })
    }

    //additiveExpression
    //    : multiplicativeExpression (additiveOperator NL* multiplicativeExpression)*
    //    ;
    fun additiveExpression(): Expr {
        return binop({ additiveOperator() }, { multiplicativeExpression() })
    }

    //multiplicativeExpression
    //    : asExpression (multiplicativeOperator NL* asExpression)*
    //    ;
    fun multiplicativeExpression(): Expr {
        return binop({ multiplicativeOperator() }, { asExpression() })
    }

    //asExpression
    //    : prefixUnaryExpression (NL* asOperator NL* type)*
    //    ;
    fun asExpression(): Expr {
        var res: Expr = prefixUnaryExpression()
        val asTypes = zeroOrMore {
            NLs()
            val kind = asOperator()
            NLs()
            kind to type()
        }
        if (asTypes.isNotEmpty()) {
            for (type in asTypes) {
                res = CastExpr(res, type.second, type.first)
            }
        }
        return res
    }

    //prefixUnaryExpression
    //    : unaryPrefix* postfixUnaryExpression
    //    ;
    fun prefixUnaryExpression(): Expr {
        val prefixes = zeroOrMore { unaryPrefix() }
        println("TODO: prefixUnaryExpression : $prefixes")
        var res: Expr = postfixUnaryExpression()
        for (prefix in prefixes.reversed()) {
            res = when (prefix) {
                is String -> UnaryPreOpExpr(prefix, res)
                else -> TODO()
            }
        }
        return res
    }

    //unaryPrefix
    //    : annotation
    //    | label
    //    | prefixUnaryOperator NL*
    //    ;
    fun unaryPrefix(): Any {
        return OR(
            { prefixUnaryOperator().also { NLs() } },
            { annotation() },
            { label() },
        )
    }

    //postfixUnaryExpression
    //    : primaryExpression postfixUnarySuffix*
    //    ;
    fun postfixUnaryExpression(): Expr {
        println("TODO: postfixUnaryExpression")
        var res: Expr = primaryExpression()
        zeroOrMore {
            res = postfixUnarySuffix(res)
        }
        return res
    }

    private fun <T> parseList(oneOrMore: Boolean = false, separator: () -> Boolean, doBreak: () -> Boolean, node: () -> T): List<T> {
        val nodes = arrayListOf<T>()
        loop@while (hasMore) {
            NLs()
            if (doBreak()) break@loop
            NLs()
            nodes += node()
            NLs()
            if (doBreak()) break@loop
            separator()
            NLs()
        }
        if (oneOrMore && nodes.isEmpty()) error("parseList nodes=${nodes.size}")
        return nodes
    }

    //postfixUnarySuffix
    //    : postfixUnaryOperator
    //    | typeArguments
    //    | callSuffix
    //    | indexingSuffix
    //    | navigationSuffix
    //    ;
    fun postfixUnarySuffix(expr: Expr): Expr {
        expectAnyOpt("++", "--", "!!")?.let {
            return UnaryPostOpExpr(expr, it)
        }
        if (peekChar() == '<') {
            //return typeArguments()
            TODO("postfixUnarySuffix.typeArguments")
        }
        // valueArguments: LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
        // annotatedLambda: annotation* label? NL* lambdaLiteral
        if (peekChar() == '(') {
            val params = expectAndRecoverSure("(", ")") {
                parseList(oneOrMore = false, separator = { expectOpt(",") }, doBreak = { matches(")") }) {
                    valueArgument()
                }
            }
            println("TODO: postfixUnarySuffix.callSuffix")
            //opt {
            //    annotations()
            //    opt { label() }
            //    NLs()
            //    lambdaLiteral()
            //}

            return CallExpr(expr, params)
        }
        if (peekChar() == '[') {
            val params = expectAndRecoverSure("[", "]") {
                parseList(separator = { expectOpt(",") }, doBreak = { matches("]") }) {
                    expression()
                }
            }
            return IndexedExpr(expr, params)
        }
        if (expectAnyOpt(".", "?.", "::") != null) {
            //return navigationSuffix()
            TODO("postfixUnarySuffix.navigationSuffix")
        }
        TODO("postfixUnarySuffix")
    }

    //directlyAssignableExpression
    //    : postfixUnaryExpression assignableSuffix
    //    | simpleIdentifier
    //    | parenthesizedDirectlyAssignableExpression
    //    ;
    fun directlyAssignableExpression() {
        TODO()
    }

    //parenthesizedDirectlyAssignableExpression
    //    : LPAREN NL* directlyAssignableExpression NL* RPAREN
    //    ;
    fun parenthesizedDirectlyAssignableExpression() {
        TODO()
    }

    //assignableExpression
    //    : prefixUnaryExpression
    //    | parenthesizedAssignableExpression
    //    ;
    fun assignableExpression() {
        TODO()
    }

    //parenthesizedAssignableExpression
    //    : LPAREN NL* assignableExpression NL* RPAREN
    //    ;
    fun parenthesizedAssignableExpression() {
        TODO()
    }

    //assignableSuffix
    //    : typeArguments
    //    | indexingSuffix
    //    | navigationSuffix
    //    ;
    fun assignableSuffix() {
        TODO()
    }

    //indexingSuffix
    //    : LSQUARE NL* expression (NL* COMMA NL* expression)* (NL* COMMA)? NL* RSQUARE
    //    ;
    fun indexingSuffix() {
        TODO()
    }

    //navigationSuffix
    //    : memberAccessOperator NL* (simpleIdentifier | parenthesizedExpression | CLASS)
    //    ;
    fun navigationSuffix() {
        TODO()
    }

    //callSuffix
    //    : typeArguments? (valueArguments? annotatedLambda | valueArguments)
    //    ;
    fun callSuffix() {
        TODO()
    }

    //annotatedLambda
    //    : annotation* label? NL* lambdaLiteral
    //    ;
    fun annotatedLambda() {
        TODO()
    }

    //typeArguments
    //    : LANGLE NL* typeProjection (NL* COMMA NL* typeProjection)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeArguments() {
        TODO("typeArguments")
    }

    //valueArguments
    //    : LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
    //    ;
    fun valueArguments() {
        TODO()
    }

    //valueArgument
    //    : annotation? NL* (simpleIdentifier NL* ASSIGNMENT NL*)? MULT? NL* expression
    //    ;
    fun valueArgument(): Expr {
        println("TODO: valueArgument")
        return expression()
    }

    //primaryExpression
    //    : parenthesizedExpression
    //    | simpleIdentifier
    //    | literalConstant
    //    | stringLiteral
    //    | callableReference
    //    | functionLiteral
    //    | objectLiteral
    //    | collectionLiteral
    //    | thisExpression
    //    | superExpression
    //    | ifExpression
    //    | whenExpression
    //    | tryExpression
    //    | jumpExpression
    //    ;
    fun primaryExpression(): Expr {
        Hidden()
        if (peekChar() == '(') return parenthesizedExpression()
        literalConstantOpt()?.let { return it }

        println("TODO: primaryExpression")

        return OR(
            { stringLiteral() },
            { callableReference() },
            { IdentifierExpr(simpleIdentifier()) },
            { functionLiteral() },
            { objectLiteral() },
            { collectionLiteral() },
            { thisExpression() },
            { superExpression() },
            { ifExpression() },
            { whenExpression() },
            { tryExpression() },
            { jumpExpression() },
        )
    }

    //parenthesizedExpression
    //    : LPAREN NL* expression NL* RPAREN
    //    ;
    fun parenthesizedExpression(): Expr {
        Hidden()
        return expectAndRecoverSure("(", ")") {
            Hidden()
            NLs()
            Hidden()
            val expr = expression()
            Hidden()
            NLs()
            Hidden()
            expr
        }
    }

    //collectionLiteral
    //    : LSQUARE NL* (expression (NL* COMMA NL* expression)* (NL* COMMA)? NL*)? RSQUARE
    //    ;
    fun collectionLiteral(): Expr {
        TODO()
    }

    //literalConstant
    //    : BooleanLiteral
    //    | IntegerLiteral
    //    | HexLiteral
    //    | BinLiteral
    //    | CharacterLiteral
    //    | RealLiteral
    //    | NullLiteral
    //    | LongLiteral
    //    | UnsignedLiteral
    //    ;
    fun literalConstantOpt(): LiteralExpr? {
        if (matches("'", consume = false)) {
            return CharacterLiteral()
        }
        expectAnyOpt("false", "true")?.let { return BoolLiteralExpr(it == "true") }
        if (expectOpt("null")) return NullLiteralExpr()
        val numLit = when {
            expectOpt("0x") || expectOpt("0X") -> numericLiteral(radix = 16) { HexDigit(it) }
            expectOpt("0o") || expectOpt("0O") -> numericLiteral(radix = 8) { it in '0'..'7' }
            expectOpt("0b") || expectOpt("0B") -> numericLiteral(radix = 2) { it in '0'..'1' }
            else -> numericLiteral(radix = 10) { DecDigit(it) }
        } ?: return null
        val isUnsigned = expectAnyOpt("u", "U")
        val isLong = expectAnyOpt("l", "L")
        return numLit.copy(isLong = isLong != null, isUnsigned = isUnsigned != null)
    }

    //stringLiteral
    //    : lineStringLiteral
    //    | multiLineStringLiteral
    //    ;
    fun stringLiteral(): Expr {
        TODO()
    }

    //lineStringLiteral
    //    : QUOTE_OPEN (lineStringContent | lineStringExpression)* QUOTE_CLOSE
    //    ;
    fun lineStringLiteral() {
        TODO()
    }

    //multiLineStringLiteral
    //    : TRIPLE_QUOTE_OPEN (multiLineStringContent | multiLineStringExpression | MultiLineStringQuote)* TRIPLE_QUOTE_CLOSE
    //    ;
    fun multiLineStringLiteral() {
        TODO()
    }

    //lineStringContent
    //    : LineStrText
    //    | LineStrEscapedChar
    //    | LineStrRef
    //    ;
    fun lineStringContent() {
        TODO()
    }

    //lineStringExpression
    //    : LineStrExprStart NL* expression NL* RCURL
    //    ;
    fun lineStringExpression() {
        TODO()
    }

    //multiLineStringContent
    //    : MultiLineStrText
    //    | MultiLineStringQuote
    //    | MultiLineStrRef
    //    ;
    fun multiLineStringContent() {
        TODO()
    }

    //multiLineStringExpression
    //    : MultiLineStrExprStart NL* expression NL* RCURL
    //    ;
    fun multiLineStringExpression() {
        TODO()
    }

    //lambdaLiteral
    //    : LCURL NL* (lambdaParameters? NL* ARROW NL*)? statements NL* RCURL
    //    ;
    fun lambdaLiteral() {
        TODO()
    }

    //lambdaParameters
    //    : lambdaParameter (NL* COMMA NL* lambdaParameter)* (NL* COMMA)?
    //    ;
    fun lambdaParameters() {
        TODO()
    }

    //lambdaParameter
    //    : variableDeclaration
    //    | multiVariableDeclaration (NL* COLON NL* type)?
    //    ;
    fun lambdaParameter() {
        TODO()
    }

    //anonymousFunction
    //    : SUSPEND?
    //      NL*
    //      FUN
    //      (NL* type NL* DOT)?
    //      NL* parametersWithOptionalType
    //      (NL* COLON NL* type)?
    //      (NL* typeConstraints)?
    //      (NL* functionBody)?
    //    ;
    fun anonymousFunction() {
        TODO()
    }

    //functionLiteral
    //    : lambdaLiteral
    //    | anonymousFunction
    //    ;
    fun functionLiteral(): Expr {
        TODO()
    }

    //objectLiteral
    //    : DATA? NL* OBJECT (NL* COLON NL* delegationSpecifiers NL*)? (NL* classBody)?
    //    ;
    fun objectLiteral(): Expr {
        expectOpt("data")
        NLs()
        expect("object")
        TODO()
    }

    //thisExpression
    //    : THIS
    //    | THIS_AT
    //    ;
    fun thisExpression(): ThisExpr = enrich {
        expect("this")
        val id = if (expectOpt("@")) identifier() else null
        ThisExpr(id)
    }

    //superExpression
    //    : SUPER (LANGLE NL* type NL* RANGLE)? (AT_NO_WS simpleIdentifier)?
    //    | SUPER_AT
    //    ;
    fun superExpression(): Expr {
        TODO()
    }

    //ifExpression
    //    : IF NL* LPAREN NL* expression NL* RPAREN NL*
    //      ( controlStructureBody
    //      | controlStructureBody? NL* SEMICOLON? NL* ELSE NL* (controlStructureBody | SEMICOLON)
    //      | SEMICOLON)
    //    ;
    fun ifExpression(): Expr {
        TODO()
    }

    //whenSubject
    //    : LPAREN (annotation* NL* VAL NL* variableDeclaration NL* ASSIGNMENT NL*)? expression RPAREN
    //    ;
    fun whenSubject() {
        TODO()
    }

    //whenExpression
    //    : WHEN NL* whenSubject? NL* LCURL NL* (whenEntry NL*)* NL* RCURL
    //    ;
    fun whenExpression(): Expr {
        TODO()
    }

    //whenEntry
    //    : whenCondition (NL* COMMA NL* whenCondition)* (NL* COMMA)? NL* ARROW NL* controlStructureBody semi?
    //    | ELSE NL* ARROW NL* controlStructureBody semi?
    //    ;
    fun whenEntry() {
        TODO()
    }

    //whenCondition
    //    : expression
    //    | rangeTest
    //    | typeTest
    //    ;
    fun whenCondition() {
        TODO()
    }

    //rangeTest
    //    : inOperator NL* expression
    //    ;
    //fun rangeTest(): RangeTestExpr = enrich {
    //    val kind = inOperator()
    //    NLs()
    //    val expr = expression()
    //    RangeTestExpr(kind, expr)
    //}

    //typeTest
    //    : isOperator NL* type
    //    ;
    //fun typeTest(): TypeTestExpr = enrich {
    //    val kind = isOperator()
    //    NLs()
    //    val expr = expression()
    //    TypeTestExpr(kind, expr)
    //}

    //tryExpression
    //    : TRY NL* block ((NL* catchBlock)+ (NL* finallyBlock)? | NL* finallyBlock)
    //    ;
    fun tryExpression(): Expr {
        TODO()
    }

    //catchBlock
    //    : CATCH NL* LPAREN annotation* simpleIdentifier COLON type (NL* COMMA)? RPAREN NL* block
    //    ;
    fun catchBlock() {
        TODO()
    }

    //finallyBlock
    //    : FINALLY NL* block
    //    ;
    fun finallyBlock() {
        TODO()
    }

    //jumpExpression
    //    : THROW NL* expression
    //    | (RETURN | RETURN_AT) expression?
    //    | CONTINUE
    //    | CONTINUE_AT
    //    | BREAK
    //    | BREAK_AT
    //    ;
    fun jumpExpression(): Expr {
        TODO()
    }

    //callableReference
    //    : receiverType? COLONCOLON NL* (simpleIdentifier | CLASS)
    //    ;
    fun callableReference(): Expr {
        TODO()
    }

    //assignmentAndOperator
    //    : ADD_ASSIGNMENT
    //    | SUB_ASSIGNMENT
    //    | MULT_ASSIGNMENT
    //    | DIV_ASSIGNMENT
    //    | MOD_ASSIGNMENT
    //    ;
    fun assignmentAndOperator(): String = expectAny("+=", "-=", "*=", "/=", "%=")

    //equalityOperator
    //    : EXCL_EQ
    //    | EXCL_EQEQ
    //    | EQEQ
    //    | EQEQEQ
    //    ;
    fun equalityOperator(): String = expectAny("!==", "===", "!=", "==")

    //comparisonOperator
    //    : LANGLE
    //    | RANGLE
    //    | LE
    //    | GE
    //    ;
    fun comparisonOperator(): String = expectAny(">=", "<=", "<", ">")

    //inOperator
    //    : IN
    //    | NOT_IN
    //    ;
    fun inOperator(): String = expectAny("!in", "in")

    //isOperator
    //    : IS
    //    | NOT_IS
    //    ;
    fun isOperator(): String = expectAny("!is", "is")

    //additiveOperator
    //    : ADD
    //    | SUB
    //    ;
    fun additiveOperator(): String = expectAny("+", "-")

    //multiplicativeOperator
    //    : MULT
    //    | DIV
    //    | MOD
    //    ;
    fun multiplicativeOperator(): String = expectAny("*", "/", "%")

    //asOperator
    //    : AS
    //    | AS_SAFE
    //    ;
    fun asOperator(): String = expectAny("as?", "as")

    //prefixUnaryOperator
    //    : INCR
    //    | DECR
    //    | SUB
    //    | ADD
    //    | excl
    //    ;
    fun prefixUnaryOperator(): String = expectAny("++", "--", "-", "+", "!")

    //postfixUnaryOperator
    //    : INCR
    //    | DECR
    //    | EXCL_NO_WS excl
    //    ;
    fun postfixUnaryOperator(): String = expectAny("++", "--", "!!")

    //excl
    //    : EXCL_NO_WS
    //    | EXCL_WS
    //    ;
    //fun excl() = expectAny("!")

    //memberAccessOperator
    //    : NL* DOT
    //    | NL* safeNav
    //    | COLONCOLON
    //    ;
    fun memberAccessOperator(): String {
        return when {
            expectOpt("::") -> "::"
            else -> {
                NLs()
                when {
                    expectOpt("?.") -> "?."
                    expectOpt(".") -> "."
                    else -> TODO()
                }
            }
        }
    }

    //safeNav
    //    : QUEST_NO_WS DOT
    //    ;
    /** ?. */
    fun safeNav(): Unit = TODO()

    // SECTION: modifiers
    //modifiers
    //    : (annotation | modifier)+
    //    ;
    fun modifiers() {
        TODO()
    }

    //parameterModifiers
    //    : (annotation | parameterModifier)+
    //    ;
    fun parameterModifiers() {
        TODO()
    }

    //modifier
    //    : (classModifier
    //    | memberModifier
    //    | visibilityModifier
    //    | functionModifier
    //    | propertyModifier
    //    | inheritanceModifier
    //    | parameterModifier
    //    | platformModifier) NL*
    //    ;
    fun modifier() {
        TODO()
    }

    //typeModifiers
    //    : typeModifier+
    //    ;
    fun typeModifiers() {
        oneOrMore { typeModifier() }
    }

    //typeModifier
    //    : annotation
    //    | SUSPEND NL*
    //    ;
    fun typeModifier() {
        TODO("typeModifier")
    }

    //classModifier
    //    : ENUM
    //    | SEALED
    //    | ANNOTATION
    //    | DATA
    //    | INNER
    //    | VALUE
    //    ;
    fun classModifier(): String = expectAny("enum", "sealed", "annotation", "data", "inner", "value")

    //memberModifier
    //    : OVERRIDE
    //    | LATEINIT
    //    ;
    fun memberModifier(): String = expectAny("override", "lateinit")

    //visibilityModifier
    //    : PUBLIC
    //    | PRIVATE
    //    | INTERNAL
    //    | PROTECTED
    //    ;
    fun visibilityModifier(): String = expectAny("public", "private", "internal", "protected")

    //varianceModifier
    //    : IN
    //    | OUT
    //    ;
    fun varianceModifier(): String = expectAny("in", "out")

    //typeParameterModifiers
    //    : typeParameterModifier+
    //    ;
    fun typeParameterModifiers() {
        TODO()
    }

    //typeParameterModifier
    //    : reificationModifier NL*
    //    | varianceModifier NL*
    //    | annotation
    //    ;
    fun typeParameterModifier() {
        TODO()
    }

    //functionModifier
    //    : TAILREC
    //    | OPERATOR
    //    | INFIX
    //    | INLINE
    //    | EXTERNAL
    //    | SUSPEND
    //    ;
    fun functionModifier(): String = expectAny("tailrec", "operator", "infix", "inline", "external", "suspend")

    //propertyModifier
    //    : CONST
    //    ;
    fun propertyModifier(): String = expectAny("const")

    //inheritanceModifier
    //    : ABSTRACT
    //    | FINAL
    //    | OPEN
    //    ;
    fun inheritanceModifier(): String = expectAny("abstract", "final", "open")

    //parameterModifier
    //    : VARARG
    //    | NOINLINE
    //    | CROSSINLINE
    //    ;
    fun parameterModifier(): String = expectAny("vararg", "noinline", "crossinline")

    //reificationModifier
    //    : REIFIED
    //    ;
    fun reificationModifier(): String = expectAny("reified")

    //platformModifier
    //    : EXPECT
    //    | ACTUAL
    //    ;
    fun platformModifier() = expectAny("expect", "actual")

    // SECTION: annotations
    //annotation
    //    : (singleAnnotation | multiAnnotation) NL*
    //    ;
    fun annotation(): AnnotationNodes {
        Hidden()
        NLs()
        expect("@")
        val useSite = expectAnyOpt("field", "property", "get", "set", "receiver", "param", "setparam", "delegate", "file")
        if (useSite != null) {
            NLs()
            expect(":")
        }
        val nodes = if (peekChar() == '[') {
            expectAndRecoverSure("[", "]") {
                AnnotationNodes(oneOrMore { unescapedAnnotation() })
            }
        } else {
            AnnotationNodes(listOf(unescapedAnnotation()))
        }
        return AnnotationNodes(nodes.annotations.map { it.copy(useSite = useSite) })
    }

    fun annotations(): List<AnnotationNodes> {
        return zeroOrMore { annotation() }
    }

    //singleAnnotation
    //    : (annotationUseSiteTarget NL* | AT_NO_WS | AT_PRE_WS) unescapedAnnotation
    //    ;
    //fun singleAnnotation(): Node {
    //    annotationUseSiteTarget()
    //    TODO("singleAnnotation")
    //}

    //multiAnnotation
    //    : (annotationUseSiteTarget NL* | AT_NO_WS | AT_PRE_WS) LSQUARE unescapedAnnotation+ RSQUARE
    //    ;
    //fun multiAnnotation(): Node {
    //    expectAndRecover("[", "]") {
    //        oneOrMore { unescapedAnnotation() }
    //    }
    //    TODO()
    //}

    //annotationUseSiteTarget
    //    : (AT_NO_WS | AT_PRE_WS) (FIELD | PROPERTY | GET | SET | RECEIVER | PARAM | SETPARAM | DELEGATE) NL* COLON
    //    ;
    fun annotationUseSiteTarget() {
        TODO()
    }

    //unescapedAnnotation
    //    : constructorInvocation
    //    | userType
    //    ;
    fun unescapedAnnotation(): AnnotationNode {
        TODO()
    }

    // SECTION: identifiers
    //simpleIdentifier
    //    : Identifier
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
    //    | GET
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
    //    | SET
    //    | VARARG
    //    | WHERE
    //    | FIELD
    //    | PROPERTY
    //    | RECEIVER
    //    | PARAM
    //    | SETPARAM
    //    | DELEGATE
    //    | FILE
    //    | EXPECT
    //    | ACTUAL
    //    | CONST
    //    | SUSPEND
    //    | VALUE
    //    ;
    fun simpleIdentifier(): String {
        Hidden()
        val res = expectAnyOpt(
            "abstract", "annotation", "by", "catch", "companion", "constructor", "crossinline", "data", "dynamic",
            "enum", "external", "final", "finally", "get", "import", "infix", "init", "inline", "inner", "internal",
            "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public",
            "reified", "sealed", "tailrec", "set", "vararg", "where", "field", "property", "receiver", "param",
            "setparam", "delegate", "file", "expect", "actual", "const", "suspend", "value"
        ) ?: Identifier()
        Hidden()
        return res
    }

    //identifier
    //    : simpleIdentifier (NL* DOT simpleIdentifier)*
    //    ;
    fun identifier(): Identifier {
        val ids = arrayListOf<String>()

        ids += simpleIdentifier()
        zeroOrMore {
            NLs()
            DOT()
            ids += simpleIdentifier()
        }
        return valentia.ast.Identifier(ids)
    }
}

