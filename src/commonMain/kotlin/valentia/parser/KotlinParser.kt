package valentia.parser

import valentia.ast.*

open class KotlinParser : KotlinLexer() {
    /**
     * Kotlin syntax grammar in ANTLR4 notation
     */

    // parser grammar KotlinParser;

    // options { tokenVocab = KotlinLexer; }

// SECTION: general

    // kotlinFile
    //    : shebangLine? NL* fileAnnotation* packageHeader importList topLevelObject* EOF
    //    ;
    fun kotlinFile() {
        opt { shebangLine() }
        NLs()
        zeroOrMore { fileAnnotation() }
        packageHeader()
        importList()
        zeroOrMore { topLevelObject() }
        EOF()
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
    fun shebangLine() {
        ShebangLine()
        NLs()
    }

    // fileAnnotation
    //    : (AT_NO_WS | AT_PRE_WS) FILE NL* COLON NL* (LSQUARE unescapedAnnotation+ RSQUARE | unescapedAnnotation) NL*
    //    ;
    fun fileAnnotation() {
        TODO()
    }

    // packageHeader
    //    : (PACKAGE identifier semi?)?
    //    ;
    fun packageHeader() {
        TODO()
    }

    // importList
    //    : importHeader*
    //    ;
    fun importList() {
        TODO()
    }

    // importHeader
    //    : IMPORT identifier (DOT MULT | importAlias)? semi?
    //    ;
    fun importHeader() {
        TODO()
    }

    // importAlias
    //    : AS simpleIdentifier
    //    ;
    fun importAlias() {
        TODO()
    }

    // topLevelObject
    //    : declaration semis?
    //    ;
    fun topLevelObject() {
        TODO()
    }

    // typeAlias
    //    : modifiers? TYPE_ALIAS NL* simpleIdentifier (NL* typeParameters)? NL* ASSIGNMENT NL* type
    //    ;
    fun typeAlias() {
        TODO()
    }

    // declaration
    //    : classDeclaration
    //    | objectDeclaration
    //    | functionDeclaration
    //    | propertyDeclaration
    //    | typeAlias
    //    ;
    fun declaration() {
        TODO()
    }

// SECTION: classes

    // classDeclaration
    //    : modifiers? (CLASS | (FUN NL*)? INTERFACE) NL* simpleIdentifier
    //      (NL* typeParameters)? (NL* primaryConstructor)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* typeConstraints)?
    //      (NL* classBody | NL* enumClassBody)?
    //    ;
    fun classDeclaration() {
        TODO()
    }

    // primaryConstructor
    //    : (modifiers? CONSTRUCTOR NL*)? classParameters
    //    ;
    fun primaryConstructor() {
        TODO()
    }

    //classBody
    //    : LCURL NL* classMemberDeclarations NL* RCURL
    //    ;
    fun classBody() {
        TODO()
    }

    //classParameters
    //    : LPAREN NL* (classParameter (NL* COMMA NL* classParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    //
    fun classParameters() {
        TODO()
    }

    //classParameter
    //    : modifiers? (VAL | VAR)? NL* simpleIdentifier COLON NL* type (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun classParameter() {
        TODO()
    }

    //delegationSpecifiers
    //    : annotatedDelegationSpecifier (NL* COMMA NL* annotatedDelegationSpecifier)*
    //    ;
    fun delegationSpecifiers() {
        TODO()
    }

    //delegationSpecifier
    //    : constructorInvocation
    //    | explicitDelegation
    //    | userType
    //    | functionType
    //    | SUSPEND NL* functionType
    //    ;
    fun delegationSpecifier() {
        TODO()
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
    fun functionDeclaration() {
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
    fun variableDeclaration() {
        TODO()
    }

    //multiVariableDeclaration
    //    : LPAREN NL* variableDeclaration (NL* COMMA NL* variableDeclaration)* (NL* COMMA)? NL* RPAREN
    //    ;
    fun multiVariableDeclaration() {
        TODO()
    }

    fun variableDeclarationOrMultiVariableDeclaration(): Node {
        //OR({ variableDeclaration() }, { multiVariableDeclaration() })
        TODO()
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
    fun propertyDeclaration() {
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
    fun objectDeclaration() {
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
    fun type() {
        TODO()
    }

    //typeReference
    //    : userType
    //    | DYNAMIC
    //    ;
    fun typeReference() {
        TODO()
    }

    //nullableType
    //    : (typeReference | parenthesizedType) NL* quest+
    //    ;
    fun nullableType() {
        TODO()
    }

    //quest
    //    : QUEST_NO_WS
    //    | QUEST_WS
    //    ;
    fun quest() {
        TODO()
    }

    //userType
    //    : simpleUserType (NL* DOT NL* simpleUserType)*
    //    ;
    fun userType() {
        TODO()
    }

    //simpleUserType
    //    : simpleIdentifier (NL* typeArguments)?
    //    ;
    fun simpleUserType() {
        TODO()
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
        TODO()
    }

    //functionType
    //    : (receiverType NL* DOT NL*)? functionTypeParameters NL* ARROW NL* type
    //    ;
    fun functionType() {
        TODO()
    }

    //functionTypeParameters
    //    : LPAREN NL* (parameter | type)? (NL* COMMA NL* (parameter | type))* (NL* COMMA)? NL* RPAREN
    //    ;
    fun functionTypeParameters() {
        TODO()
    }

    //parenthesizedType
    //    : LPAREN NL* type NL* RPAREN
    //    ;
    fun parenthesizedType() {
        TODO()
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
    fun definitelyNonNullableType() {
        TODO()
    }

// SECTION: statements

    //statements
    //    : (statement (semis statement)*)? semis?
    //    ;
    fun statements() {
        TODO()
    }

    //statement
    //    : (label | annotation)* ( declaration | assignment | loopStatement | expression)
    //    ;
    fun statement() {
        TODO()
    }

    //label
    //    : simpleIdentifier (AT_NO_WS | AT_POST_WS) NL*
    //    ;
    fun label() {
        TODO()
    }

    //controlStructureBody
    //    : block
    //    | statement
    //    ;
    fun controlStructureBody(): Stm {
        TODO()
    }

    //block
    //    : LCURL NL* statements NL* RCURL
    //    ;
    fun block() {
        TODO()
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
        var vardecl: Node? = null
        expectAndRecover("(", ")") {
            annotations = annotations()
            vardecl = variableDeclarationOrMultiVariableDeclaration()
            expect("in")
            expression()
        }
        NLs()
        val body = opt { controlStructureBody() }
        ForLoopStm(expr, vardecl, body, annotations)
    }

    // whileStatement
    //    : WHILE NL* LPAREN expression RPAREN NL* (controlStructureBody | SEMICOLON)
    //    ;
    fun whileStatement(): WhileLoopStm = enrich {
        expect("while")
        NLs()
        val cond = expectAndRecover("(", ")") { expression() }
        NLs()
        val body = OR({ expect(";") }, { controlStructureBody() })
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
        TODO()
    }

    //semis
    //    : (SEMICOLON | NL)+
    //    ;
    fun semis() {
        TODO()
    }

// SECTION: expressions

    //expression
    //    : disjunction
    //    ;
    fun expression(): Expr {
        TODO()
    }

    //disjunction
    //    : conjunction (NL* DISJ NL* conjunction)*
    //    ;
    fun disjunction() {
        TODO()
    }

    //conjunction
    //    : equality (NL* CONJ NL* equality)*
    //    ;
    fun conjunction() {
        TODO()
    }

    //equality
    //    : comparison (equalityOperator NL* comparison)*
    //    ;
    fun equality() {
        TODO()
    }

    //comparison
    //    : genericCallLikeComparison (comparisonOperator NL* genericCallLikeComparison)*
    //    ;
    fun comparison() {
        TODO()
    }

    //genericCallLikeComparison
    //    : infixOperation callSuffix*
    //    ;
    fun genericCallLikeComparison() {
        TODO()
    }

    //infixOperation
    //    : elvisExpression (inOperator NL* elvisExpression | isOperator NL* type)*
    //    ;
    fun infixOperation() {
        TODO()
    }

    //elvisExpression
    //    : infixFunctionCall (NL* elvis NL* infixFunctionCall)*
    //    ;
    fun elvisExpression() {
        TODO()
    }

    //elvis
    //    : QUEST_NO_WS COLON
    //    ;
    fun elvis() {
        TODO()
    }

    //infixFunctionCall
    //    : rangeExpression (simpleIdentifier NL* rangeExpression)*
    //    ;
    fun infixFunctionCall() {
        TODO()
    }

    //rangeExpression
    //    : additiveExpression ((RANGE | RANGE_UNTIL) NL* additiveExpression)*
    //    ;
    fun rangeExpression() {
        TODO()
    }

    //additiveExpression
    //    : multiplicativeExpression (additiveOperator NL* multiplicativeExpression)*
    //    ;
    fun additiveExpression() {
        TODO()
    }

    //multiplicativeExpression
    //    : asExpression (multiplicativeOperator NL* asExpression)*
    //    ;
    fun multiplicativeExpression() {
        TODO()
    }

    //asExpression
    //    : prefixUnaryExpression (NL* asOperator NL* type)*
    //    ;
    fun asExpression() {
        TODO()
    }

    //prefixUnaryExpression
    //    : unaryPrefix* postfixUnaryExpression
    //    ;
    fun prefixUnaryExpression() {
        TODO()
    }

    //unaryPrefix
    //    : annotation
    //    | label
    //    | prefixUnaryOperator NL*
    //    ;
    fun unaryPrefix() {
        TODO()
    }

    //postfixUnaryExpression
    //    : primaryExpression postfixUnarySuffix*
    //    ;
    fun postfixUnaryExpression() {
        TODO()
    }

    //postfixUnarySuffix
    //    : postfixUnaryOperator
    //    | typeArguments
    //    | callSuffix
    //    | indexingSuffix
    //    | navigationSuffix
    //    ;
    fun postfixUnarySuffix() {
        TODO()
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
        TODO()
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
    fun valueArgument() {
        TODO()
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
    fun primaryExpression() {
        TODO()
    }

    //parenthesizedExpression
    //    : LPAREN NL* expression NL* RPAREN
    //    ;
    fun parenthesizedExpression() {
        TODO()
    }

    //collectionLiteral
    //    : LSQUARE NL* (expression (NL* COMMA NL* expression)* (NL* COMMA)? NL*)? RSQUARE
    //    ;
    fun collectionLiteral() {
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
    fun literalConstant() {
        TODO()
    }

    //stringLiteral
    //    : lineStringLiteral
    //    | multiLineStringLiteral
    //    ;
    fun stringLiteral() {
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
    fun functionLiteral() {
        TODO()
    }

    //objectLiteral
    //    : DATA? NL* OBJECT (NL* COLON NL* delegationSpecifiers NL*)? (NL* classBody)?
    //    ;
    fun objectLiteral() {
        TODO()
    }

    //thisExpression
    //    : THIS
    //    | THIS_AT
    //    ;
    fun thisExpression() {
        TODO()
    }

    //superExpression
    //    : SUPER (LANGLE NL* type NL* RANGLE)? (AT_NO_WS simpleIdentifier)?
    //    | SUPER_AT
    //    ;
    fun superExpression() {
        TODO()
    }

    //ifExpression
    //    : IF NL* LPAREN NL* expression NL* RPAREN NL*
    //      ( controlStructureBody
    //      | controlStructureBody? NL* SEMICOLON? NL* ELSE NL* (controlStructureBody | SEMICOLON)
    //      | SEMICOLON)
    //    ;
    fun ifExpression() {
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
    fun whenExpression() {
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
    fun rangeTest() {
        TODO()
    }

    //typeTest
    //    : isOperator NL* type
    //    ;
    fun typeTest() {
        TODO()
    }

    //tryExpression
    //    : TRY NL* block ((NL* catchBlock)+ (NL* finallyBlock)? | NL* finallyBlock)
    //    ;
    fun tryExpression() {
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
    fun jumpExpression() {
        TODO()
    }

    //callableReference
    //    : receiverType? COLONCOLON NL* (simpleIdentifier | CLASS)
    //    ;
    fun callableReference() {
        TODO()
    }

    //assignmentAndOperator
    //    : ADD_ASSIGNMENT
    //    | SUB_ASSIGNMENT
    //    | MULT_ASSIGNMENT
    //    | DIV_ASSIGNMENT
    //    | MOD_ASSIGNMENT
    //    ;
    fun assignmentAndOperator() {
        TODO()
    }

    //equalityOperator
    //    : EXCL_EQ
    //    | EXCL_EQEQ
    //    | EQEQ
    //    | EQEQEQ
    //    ;
    fun equalityOperator() {
        TODO()
    }

    //comparisonOperator
    //    : LANGLE
    //    | RANGLE
    //    | LE
    //    | GE
    //    ;
    fun comparisonOperator() {
        TODO()
    }

    //inOperator
    //    : IN
    //    | NOT_IN
    //    ;
    fun inOperator() {
        TODO()
    }

    //isOperator
    //    : IS
    //    | NOT_IS
    //    ;
    fun isOperator() {
        TODO()
    }

    //additiveOperator
    //    : ADD
    //    | SUB
    //    ;
    fun additiveOperator() {
        TODO()
    }

    //multiplicativeOperator
    //    : MULT
    //    | DIV
    //    | MOD
    //    ;
    fun multiplicativeOperator() {
        TODO()
    }

    //asOperator
    //    : AS
    //    | AS_SAFE
    //    ;
    fun asOperator() {
        TODO()
    }

    //prefixUnaryOperator
    //    : INCR
    //    | DECR
    //    | SUB
    //    | ADD
    //    | excl
    //    ;
    fun prefixUnaryOperator() {
        TODO()
    }

    //postfixUnaryOperator
    //    : INCR
    //    | DECR
    //    | EXCL_NO_WS excl
    //    ;
    fun postfixUnaryOperator() {
        TODO()
    }

    //excl
    //    : EXCL_NO_WS
    //    | EXCL_WS
    //    ;
    fun excl() {
        TODO()
    }

    //memberAccessOperator
    //    : NL* DOT
    //    | NL* safeNav
    //    | COLONCOLON
    //    ;
    fun memberAccessOperator() {
        TODO()
    }

    //safeNav
    //    : QUEST_NO_WS DOT
    //    ;
    fun safeNav() {
        TODO()
    }

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
        TODO()
    }

    //typeModifier
    //    : annotation
    //    | SUSPEND NL*
    //    ;
    fun typeModifier() {
        TODO()
    }

    //classModifier
    //    : ENUM
    //    | SEALED
    //    | ANNOTATION
    //    | DATA
    //    | INNER
    //    | VALUE
    //    ;
    fun classModifier() {
        TODO()
    }

    //memberModifier
    //    : OVERRIDE
    //    | LATEINIT
    //    ;
    fun memberModifier() {
        TODO()
    }

    //visibilityModifier
    //    : PUBLIC
    //    | PRIVATE
    //    | INTERNAL
    //    | PROTECTED
    //    ;
    fun visibilityModifier() {
        TODO()
    }

    //varianceModifier
    //    : IN
    //    | OUT
    //    ;
    fun varianceModifier() {
        TODO()
    }

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
    fun functionModifier() {
        TODO()
    }

    //propertyModifier
    //    : CONST
    //    ;
    fun propertyModifier() {
        TODO()
    }

    //inheritanceModifier
    //    : ABSTRACT
    //    | FINAL
    //    | OPEN
    //    ;
    fun inheritanceModifier() {
        TODO()
    }

    //parameterModifier
    //    : VARARG
    //    | NOINLINE
    //    | CROSSINLINE
    //    ;
    fun parameterModifier() {
        TODO()
    }

    //reificationModifier
    //    : REIFIED
    //    ;
    fun reificationModifier() {
        TODO()
    }

    //platformModifier
    //    : EXPECT
    //    | ACTUAL
    //    ;
    fun platformModifier() {
        TODO()
    }

    // SECTION: annotations
    //annotation
    //    : (singleAnnotation | multiAnnotation) NL*
    //    ;
    fun annotation(): Node {
        TODO()
    }

    fun annotations(): List<Node> {
        return zeroOrMore { annotation() }
    }

    //singleAnnotation
    //    : (annotationUseSiteTarget NL* | AT_NO_WS | AT_PRE_WS) unescapedAnnotation
    //    ;
    fun singleAnnotation() {
        TODO()
    }

    //multiAnnotation
    //    : (annotationUseSiteTarget NL* | AT_NO_WS | AT_PRE_WS) LSQUARE unescapedAnnotation+ RSQUARE
    //    ;
    fun multiAnnotation() {
        TODO()
    }

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
    fun unescapedAnnotation() {
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
    fun simpleIdentifier() {
        TODO()
    }

    //identifier
    //    : simpleIdentifier (NL* DOT simpleIdentifier)*
    //    ;
    fun identifier() {
        TODO()
    }
}

