package valentia.parser

import valentia.ast.*
import valentia.util.Disjunction2
import valentia.util.Disjunction3

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
        val modifiers = modifiers(atLeastOne = false)
        expect("typealias")
        NLs()
        val id = simpleIdentifier()
        val types = opt {
            NLs()
            typeParameters()
        }
        NLs()
        expect("=")
        NLs()
        val type = type()
        return TypeAliasDecl(id, type, types, modifiers)
    }

    // declaration
    //    : classDeclaration
    //    | objectDeclaration
    //    | functionDeclaration
    //    | propertyDeclaration
    //    | typeAlias
    //    ;
    fun declaration(): DeclNode {
        println("TODO: declaration")
        if (matches("typealias")) {
            return typeAlias()
        }
        return OR(
            { functionDeclaration() },
            { classDeclaration() },
            { objectDeclaration() },
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
    fun classDeclaration(): ClassDecl {
        println("TODO: classDeclaration")
        val modifiers = opt { modifiers() }
        val kind = OR(
            { expect("class"); "class" },
            {
                val isFun = expectOpt("fun")
                NLs()
                expect("interface")
                if (isFun) "fun interface" else "interface"
            },
        )
        NLs()
        val className = simpleIdentifier()
        opt { NLs(); typeParameters() }
        opt { NLs(); primaryConstructor() }
        val subTypes = opt { NLs(); COLON(); NLs(); delegationSpecifiers() }
        opt { NLs(); typeConstraints() }
        opt { NLs(); OR({ classBody() }, { enumClassBody() }) }
        return ClassDecl(kind = kind, name = className, subTypes = subTypes)
    }

    // primaryConstructor
    //    : (modifiers? CONSTRUCTOR NL*)? classParameters
    //    ;
    fun primaryConstructor() {
        println("TODO: primaryConstructor")
        opt {
            opt { modifiers() }
            CONSTRUCTOR()
            NLs()
        }
        classParameters()
        return
    }

    //classBody
    //    : LCURL NL* classMemberDeclarations NL* RCURL
    //    ;
    fun classBody(): List<DeclNode> {
        return expectAndRecover("{", "}") {
            NLs()
            classMemberDeclarations().also { NLs() }
        } ?: emptyList()
    }

    //classParameters
    //    : LPAREN NL* (classParameter (NL* COMMA NL* classParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    //
    fun classParameters(): List<ClassParameter> {
        return parseListWithStartEnd("(", ")", oneOrMore = true, separator = { expectOpt(",") }) {
            classParameter()
        }
    }

    //classParameter
    //    : modifiers? (VAL | VAR)? NL* simpleIdentifier COLON NL* type (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun classParameter(): ClassParameter {
        println("TODO: classParameter")
        opt { modifiers() }
        opt { expectAnyOpt("val", "var") }
        NLs()
        val id = simpleIdentifier()
        COLON()
        NLs()
        val type = type()
        opt { NLs(); ASSIGNMENT(); NLs(); expression() }
        return ClassParameter(id)
    }

    //delegationSpecifiers
    //    : annotatedDelegationSpecifier (NL* COMMA NL* annotatedDelegationSpecifier)*
    //    ;
    fun delegationSpecifiers(): List<SubTypeInfo> {
        return parseList(oneOrMore = true, separator = { expectOpt(",") }) {
            annotatedDelegationSpecifier()
        }
    }

    //delegationSpecifier
    //    : constructorInvocation
    //    | explicitDelegation
    //    | userType
    //    | functionType
    //    | SUSPEND NL* functionType
    //    ;
    fun delegationSpecifier(): SubTypeInfo {
        return OR(
            { constructorInvocation() },
            { explicitDelegation() },
            { BasicSubTypeInfo(userType()) },
            { BasicSubTypeInfo(functionType()) },
            { SUSPEND(); NLs(); BasicSubTypeInfo(functionType().suspendable()) },
        )
    }

    //constructorInvocation
    //    : userType NL* valueArguments
    //    ;
    fun constructorInvocation(): ConstructorInvocation {
        val type = userType()
        NLs()
        val args = valueArguments()
        return ConstructorInvocation(type, args)
    }

    //annotatedDelegationSpecifier
    //    : annotation* NL* delegationSpecifier
    //    ;
    fun annotatedDelegationSpecifier(): SubTypeInfo {
        val annotations = annotations(atLeastOne = false).also { NLs() }
        return delegationSpecifier().annotated(annotations)
    }

    //explicitDelegation
    //    : (userType | functionType) NL* BY NL* expression
    //    ;
    fun explicitDelegation(): ExplicitDelegation {
        val type = OR(
            { userType() },
            { functionType() }
        )
        NLs()
        BY()
        NLs()
        val delegate = expression()
        return ExplicitDelegation(type, delegate)
    }

    //typeParameters
    //    : LANGLE NL* typeParameter (NL* COMMA NL* typeParameter)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeParameters(): List<TypeParameter> {
        return parseListWithStartEnd("<", ">", oneOrMore = true, separator = { expectOpt(",") }) {
            typeParameter()
        }
    }

    //typeParameter
    //    : typeParameterModifiers? NL* simpleIdentifier (NL* COLON NL* type)?
    //    ;
    fun typeParameter(): TypeParameter {
        println("TODO: typeParameter")
        //opt { typeParameterModifiers() }
        NLs()
        val id = simpleIdentifier()
        var type: TypeNode? = null
        opt {
            NLs()
            COLON()
            NLs()
            type = type()
        }
        return TypeParameter(id, type)
    }

    //typeConstraints
    //    : WHERE NL* typeConstraint (NL* COMMA NL* typeConstraint)*
    //    ;
    fun typeConstraints() {
        expect("where")
        NLs()
        typeConstraint()
        zeroOrMore {
            NLs()
            expect(",")
            NLs()
            typeConstraint()
        }
        TODO("typeConstraints")
    }

    //typeConstraint
    //    : annotation* simpleIdentifier NL* COLON NL* type
    //    ;
    fun typeConstraint() {
        TODO("typeConstraint")
    }

// SECTION: classMembers

    //classMemberDeclarations
    //    : (classMemberDeclaration semis?)*
    //    ;
    fun classMemberDeclarations(): List<DeclNode> {
        return zeroOrMore {
            classMemberDeclaration().also {
                semis(atLeastOne = false)
            }
        }
    }

    //classMemberDeclaration
    //    : declaration
    //    | companionObject
    //    | anonymousInitializer
    //    | secondaryConstructor
    //    ;
    fun classMemberDeclaration(): DeclNode {
        return OR(
            { declaration() },
            { companionObject() },
            { anonymousInitializer() },
            { secondaryConstructor() },
        )
    }

    //anonymousInitializer
    //    : INIT NL* block
    //    ;
    fun anonymousInitializer(): InitDecl {
        expect("init")
        NLs()
        val block = block()
        return InitDecl(block)
    }

    //companionObject
    //    : modifiers? COMPANION NL* DATA? NL* OBJECT
    //      (NL* simpleIdentifier)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun companionObject(): CompanionObjectDecl {
        println("TODO: companionObject")
        modifiers(atLeastOne = false)
        expect("companion")
        NLs()
        val isData = expectOpt("data")
        NLs()
        expect("object")
        val name = opt { NLs(); simpleIdentifier() }
        opt { NLs(); COLON(); NLs(); delegationSpecifiers() }
        opt { NLs(); classBody() }
        return CompanionObjectDecl(name)
    }

    //functionValueParameters
    //    : LPAREN NL* (functionValueParameter (NL* COMMA NL* functionValueParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    fun functionValueParameters(): List<FuncValueParam> {
        return parseListWithStartEnd("(", ")", oneOrMore = false) {
            functionValueParameter()
        }
    }

    //functionValueParameter
    //    : parameterModifiers? parameter (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameter(): FuncValueParam {
        parameterModifiers(atLeastOne = false)
        val param = parameter()
        opt {
            NLs()
            assignment()
            NLs()
            expression()
        }
        println("TODO: functionValueParameter")
        return FuncValueParam(param.id, param.type)
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
        println("TODO: functionDeclaration")
        expect("fun")
        opt {
            NLs()
            typeParameters()
        }
        opt {
            NLs()
            receiverType()
            NLs()
            DOT()
        }
        NLs()
        val funcName = simpleIdentifier()
        NLs()
        val params = functionValueParameters()
        opt {
            NLs()
            COLON()
            NLs()
            type()
        }
        opt {
            NLs()
            typeConstraints()
        }
        val body = opt {
            NLs()
            functionBody()
        }
        return FunDecl(funcName, params, body = body)
    }

    //functionBody
    //    : block
    //    | ASSIGNMENT NL* expression
    //    ;
    fun functionBody(): Stm {
        println("TODO: functionBody")
        return OR(
            { block() },
            { ASSIGNMENT(); NLs(); ExprStm(expression()) }
        )
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
    //      (NL* SEMICOLON)? NL* (
    //      getter? (NL* semi? setter)?
    //      | setter? (NL* semi? getter)?
    //      )
    //    ;
    fun propertyDeclaration(): VariableDecls {
        println("TODO: propertyDeclaration")
        modifiers(atLeastOne = false)
        expectAny("val", "var")
        opt { NLs(); typeParameters() }
        opt { NLs(); receiverType(); NLs(); DOT() }
        val decls = opt { NLs(); OR({ multiVariableDeclaration() }, { VariableDecls(variableDeclaration()) }) }
        opt { typeConstraints() }
        opt { NLs(); OR({ ASSIGNMENT(); NLs(); expression() }, { propertyDelegate() }) }
        opt { NLs(); SEMICOLON() }
        NLs()
        OR(
            {
                opt { getter() }
                opt { NLs(); semiOpt(); setter() }
            },
            {
                opt { setter() }
                opt { NLs(); semiOpt(); getter() }
            },
        )
        return decls ?: VariableDecls()
    }

    //propertyDelegate
    //    : BY NL* expression
    //    ;
    fun propertyDelegate(): Expr {
        expect("by")
        NLs()
        return expression()
    }

    //getter
    //    : modifiers? GET
    //      (NL* LPAREN NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun getter() {
        modifiers(atLeastOne = false)
        GET()
        opt {
            NLs()
            LPAREN()
            NLs()
            RPAREN()
            opt {
                NLs()
                COLON()
                NLs()
                type()
            }
            NLs()
            functionBody()
        }
    }

    //setter
    //    : modifiers? SET
    //      (NL* LPAREN NL* functionValueParameterWithOptionalType (NL* COMMA)? NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun setter() {
        modifiers(atLeastOne = false)
        SET()
        opt {
            NLs()
            LPAREN()
            NLs()
            functionValueParameterWithOptionalType()
            opt {
                NLs()
                COMMA()
            }
            RPAREN()
            opt {
                NLs()
                COLON()
                NLs()
                type()
            }
            NLs()
            functionBody()
        }
    }

    //parametersWithOptionalType
    //    : LPAREN NL* (functionValueParameterWithOptionalType (NL* COMMA NL* functionValueParameterWithOptionalType)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    fun parametersWithOptionalType() {
        TODO("parametersWithOptionalType")
    }

    //functionValueParameterWithOptionalType
    //    : parameterModifiers? parameterWithOptionalType (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameterWithOptionalType() {
        opt { parameterModifiers() }
        parameterWithOptionalType()
        opt { NLs(); ASSIGNMENT(); NLs(); expression() }
        TODO("functionValueParameterWithOptionalType")
    }

    //parameterWithOptionalType
    //    : simpleIdentifier NL* (COLON NL* type)?
    //    ;
    fun parameterWithOptionalType() {
        simpleIdentifier()
        NLs()
        opt { COLON(); NLs(); type() }
        TODO("parameterWithOptionalType")
    }

    //parameter
    //    : simpleIdentifier NL* COLON NL* type
    //    ;
    fun parameter(): Parameter {
        val id = simpleIdentifier()
        NLs()
        COLON()
        NLs()
        val type = type()
        return Parameter(id, type)
    }

    //objectDeclaration
    //    : modifiers? OBJECT
    //      NL* simpleIdentifier
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun objectDeclaration(): DeclNode {
        println("TODO: objectDeclaration")
        opt { modifiers() }
        expect("object")
        NLs()
        val name = simpleIdentifier()
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
        return ObjectDecl(name)
    }

    //secondaryConstructor
    //    : modifiers? CONSTRUCTOR NL* functionValueParameters (NL* COLON NL* constructorDelegationCall)? NL* block?
    //    ;
    fun secondaryConstructor(): ConstructorDecl {
        println("TODO: secondaryConstructor")
        modifiers(atLeastOne = false)
        CONSTRUCTOR()
        NLs()
        val params = functionValueParameters()
        opt {
            NLs()
            COLON()
            NLs()
            constructorDelegationCall()
        }
        NLs()
        val body = opt { block() }
        return ConstructorDecl(params = params, body = body)
    }

    //constructorDelegationCall
    //    : (THIS | SUPER) NL* valueArguments
    //    ;
    fun constructorDelegationCall() {
        val kind = expectAnyOpt("this", "super")
        NLs()
        valueArguments()
        TODO("constructorDelegationCall")
    }

    // SECTION: enumClasses
    //enumClassBody
    //    : LCURL NL* enumEntries? (NL* SEMICOLON NL* classMemberDeclarations)? NL* RCURL
    //    ;
    fun enumClassBody() {
        println("TODO: enumClassBody")
        expect("{")
        NLs()
        enumEntries(oneOrMore = false)
        opt {
            NLs()
            SEMICOLON()
            NLs()
            classMemberDeclarations()
        }
        NLs()
        expect("}")
    }

    //enumEntries
    //    : enumEntry (NL* COMMA NL* enumEntry)* NL* COMMA?
    //    ;
    fun enumEntries(oneOrMore: Boolean = true): List<EnumEntry> {
        return parseList(oneOrMore = oneOrMore, separator = { expectOpt(",") }, doBreak = { false }) {
            enumEntry()
        }
    }

    //enumEntry
    //    : (modifiers NL*)? simpleIdentifier (NL* valueArguments)? (NL* classBody)?
    //    ;
    fun enumEntry(): EnumEntry {
        println("TODO: enumEntry")
        val modifiers = opt { modifiers() }
        NLs()
        val id = simpleIdentifier()
        NLs()
        val arguments = opt { valueArguments() }
        NLs()
        val body = opt { classBody() }
        return EnumEntry(id)
    }

    // SECTION: types
    //type
    //    : typeModifiers? (functionType | parenthesizedType | nullableType | typeReference | definitelyNonNullableType)
    //    ;
    fun type(): TypeNode {
        Hidden()
        println("TODO: type")
        val type = when {
            matches("(") -> expectAndRecover("(", ")") {
                NLs()
                type().also { NLs() }
            } ?: UnknownType
            else -> OR({ definitelyNonNullableType() }, { typeReference() })
        }
        NLs()
        val isNullable = if (expectOpt("?")) {
            do {
                Hidden()
            } while (expectOpt("?"))
            true
        } else {
            false
        }

        return if (isNullable) type.nullable() else type
        //zeroOrMore { typeModifier() }
        //return typeReference()
        //return OR(
        //    { nullableType() },
        //    { parenthesizedType() },
        //    { typeReference() },
        //    { definitelyNonNullableType() },
        //    //{ functionType() },
        //)
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
    //fun nullableType(): TypeNode {
    //}

    //quest
    //    : QUEST_NO_WS
    //    | QUEST_WS
    //    ;
    //fun quest() {
    //    QUEST_WS()
    //    TODO("quest")
    //}

    //userType
    //    : simpleUserType (NL* DOT NL* simpleUserType)*
    //    ;
    fun userType(): TypeNode {
        //println("TODO: userType")
        val base = simpleUserType()
        val more = zeroOrMore {
            NLs()
            DOT()
            NLs()
            simpleUserType()
        }

        return if (more.isNotEmpty()) MultiType(listOf(base, *more.toTypedArray())) else base
        //val types = parseList(oneOrMore = true, separator = { expectOpt(".") }) {
        //    simpleUserType()
        //}
        //return UserType(types)
    }

    //simpleUserType
    //    : simpleIdentifier (NL* typeArguments)?
    //    ;
    fun simpleUserType(): TypeNode {
        println("TODO: simpleUserType")
        val id = simpleIdentifier()
        NLs()
        val generic = if (peekChar() == '<') typeArguments() else null
        val simple = SimpleType(id)
        return if (generic != null) GenericType(simple, generic) else simple
    }

    //typeProjection
    //    : typeProjectionModifiers? type
    //    | MULT
    //    ;
    fun typeProjection(): TypeNode {
        return OR(
            {
                opt { typeProjectionModifiers() }
                type()
            },
            {
                MULT()
                SimpleType("*")
            },
        )
    }

    //typeProjectionModifiers
    //    : typeProjectionModifier+
    //    ;
    fun typeProjectionModifiers(atLeastOne: Boolean = true): List<Any> {
        return multiple(atLeastOne = atLeastOne) { typeProjectionModifier() }
    }

    //typeProjectionModifier
    //    : varianceModifier NL*
    //    | annotation
    //    ;
    fun typeProjectionModifier(): Any {
        return OR(
            { varianceModifier().also { NLs() } },
            { annotation() }
        )
    }

    //functionType
    //    : (receiverType NL* DOT NL*)? functionTypeParameters NL* ARROW NL* type
    //    ;
    fun functionType(): FuncTypeNode {
        opt {
            receiverType()
            NLs()
            DOT().also { NLs() }
        }
        functionTypeParameters()
        NLs()
        ARROW()
        NLs()
        type()
        TODO("functionType")
    }

    //functionTypeParameters
    //    : LPAREN NL* (parameter | type)? (NL* COMMA NL* (parameter | type))* (NL* COMMA)? NL* RPAREN
    //    ;
    fun functionTypeParameters() {
        expectAndRecoverSure("(", ")") {
            opt { OR({ parameter() }, { type() }) }
            zeroOrMore {
                NLs()
                COMMA()
                NLs()
                OR({ parameter() }, { type() })
            }
            opt {
                NLs()
                COMMA()
            }
            NLs()
        }
        TODO("functionTypeParameters")
    }

    //parenthesizedType
    //    : LPAREN NL* type NL* RPAREN
    //    ;
    //fun parenthesizedType(): TypeNode {
    //    return expectAndRecover("(", ")") {
    //        NLs()
    //        type().also { NLs() }
    //    } ?: UnknownType
    //}

    //receiverType
    //    : typeModifiers? (parenthesizedType | nullableType | typeReference)
    //    ;
    fun receiverType(): TypeNode {
        println("TODO: receiverType")
        val modifiers = zeroOrMore { typeModifier() }
        //return nullableType().withModifiers(modifiers)
        return type().withModifiers(modifiers)
    }

    //parenthesizedUserType
    //    : LPAREN NL* (userType | parenthesizedUserType) NL* RPAREN
    //    ;
    fun parenthesizedUserType(): Any? {
        return expectAndRecover("(", ")") {
            NLs()
            OR({ userType() }, { parenthesizedUserType() }).also { NLs() }
        }
    }

    //definitelyNonNullableType
    //    : typeModifiers? (userType | parenthesizedUserType) NL* AMP NL* typeModifiers? (userType | parenthesizedUserType)
    //    ;
    fun definitelyNonNullableType(): TypeNode {
        opt { typeModifiers() }
        OR({ userType() }, { parenthesizedUserType() })
        NLs()
        expect("&")
        NLs()
        opt { typeModifiers() }
        OR({ userType() }, { parenthesizedUserType() })
        TODO("definitelyNonNullableType")
    }

// SECTION: statements

    //statements
    //    : (statement (semis statement)*)? semis?
    //    ;
    fun statements(oneOrMore: Boolean = true): List<Stm> {
        val out = parseList(oneOrMore = oneOrMore, separator = {
            try {
                semis()
                true
            } catch (e: IllegalStateException) {
                false
            }
        }, doBreak = { matches("}") }) {
            statement()
        }
        semis(atLeastOne = false)
        return out
    }

    //statement
    //    : (label | annotation)* ( declaration | assignment | loopStatement | expression)
    //    ;
    fun statement(): Stm {
        println("TODO: statement")
        NLs()
        zeroOrMore {
            OR({ label() }, { annotation() })
        }
        NLs()
        return OR(
            { loopStatement() },
            { DeclStm(declaration()) },
            { assignment() },
            { ExprStm(expression()) }
        )
    }

    //label
    //    : simpleIdentifier (AT_NO_WS | AT_POST_WS) NL*
    //    ;
    fun label(): LabelNode {
        val id = simpleIdentifier()
        expect("@")
        NLs()
        return LabelNode(id)
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
            if (!matches("}")) {
                Stms(statements()).also { NLs() }
            } else {
                Stms()
            }
        }
    }

    // loopStatement
    //    : forStatement
    //    | whileStatement
    //    | doWhileStatement
    //    ;
    fun loopStatement(): LoopStm = when (expectAnyOpt("for", "while", "do", consume = false)) {
        "for" -> forStatement()
        "while" -> whileStatement()
        "do" -> doWhileStatement()
        else -> error("loop")
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
        val cond = expectAndRecover("(", ")") { expression() } ?: EmptyExpr()
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
    fun assignment(): AssignStm {
        var op = "="
        val lvalue = OR(
            { directlyAssignableExpression().also { ASSIGNMENT(); op = "=" } },
            { assignableExpression().also { Hidden(); op = assignmentAndOperator() } }
        )
        NLs()
        val expr = expression()
        return AssignStm(lvalue, op, expr)
    }

    //semi
    //    : (SEMICOLON | NL) NL*
    //    ;
    fun semi() {
        val c = peekChar()
        when (c) {
            ';' -> skip(1)
            '\r', '\n' -> NLs()
            else -> error("semi")
        }
        while (peekChar() == '\r' || peekChar() == '\n') NLs()
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
        var res: Expr = infixOperation()
        zeroOrMore {
            callSuffix(res)?.also { res = it }
        }
        //println("TODO: genericCallLikeComparison")
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
            val prefixValue = prefix.value
            res = when (prefixValue) {
                is UnaryPreOp -> UnaryPreOpExpr(prefixValue, res)
                else -> TODO("prefixUnaryExpression")
            }
        }
        return res
    }

    //unaryPrefix
    //    : annotation
    //    | label
    //    | prefixUnaryOperator NL*
    //    ;
    fun unaryPrefix(): Disjunction3<UnaryPreOp, AnnotationNodes, LabelNode> {
        return ORDis(
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
            postfixUnarySuffix(res)?.let { res = it }
        }
        return res
    }

    private fun <T> parseListWithStartEnd(start: String, end: String, oneOrMore: Boolean = false, separator: () -> Boolean = { expectOpt(",") }, node: () -> T): List<T> {
        return expectAndRecoverSure(start, end) {
            parseList(oneOrMore = oneOrMore, separator = separator, doBreak = { matches(end) }, node = node)
        }
    }

    private fun <T> parseList(oneOrMore: Boolean = false, separator: () -> Boolean = { expectOpt(",") }, doBreak: () -> Boolean = { false }, node: () -> T): List<T> {
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
    fun postfixUnarySuffix(expr: Expr): Expr? {
        expectAnyOpt("++", "--", "!!")?.let {
            return UnaryPostOpExpr(expr, UnaryPostOp.BY_ID[it]!!)
        }
        // valueArguments: LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
        // annotatedLambda: annotation* label? NL* lambdaLiteral
        if (peekChar() == '<' || peekChar() == '(') {
            println("TODO: postfixUnarySuffix.callSuffix")
            return callSuffix(expr)
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
        return null
    }

    //directlyAssignableExpression
    //    : postfixUnaryExpression assignableSuffix
    //    | simpleIdentifier
    //    | parenthesizedDirectlyAssignableExpression
    //    ;
    fun directlyAssignableExpression(): AssignableExpr {
        //println("TODO: directlyAssignableExpression")
        return OR(
            { parenthesizedDirectlyAssignableExpression() },
            { assignableSuffix(postfixUnaryExpression()) },
            { IdentifierExpr(simpleIdentifier()) },
        )
    }

    //parenthesizedDirectlyAssignableExpression
    //    : LPAREN NL* directlyAssignableExpression NL* RPAREN
    //    ;
    fun parenthesizedDirectlyAssignableExpression(): AssignableExpr {
        return expectAndRecoverSure("(", ")") {
            NLs()
            directlyAssignableExpression().also { NLs() }
        }
    }

    //assignableExpression
    //    : prefixUnaryExpression
    //    | parenthesizedAssignableExpression
    //    ;
    fun assignableExpression(): AssignableExpr {
        return OR({
            val expr = prefixUnaryExpression()
            //(expr as? AssignableExpr?) ?: TODO("expr=$expr !is AssignableExpr")
            (expr as? AssignableExpr?) ?: error("expr=$expr !is AssignableExpr")
        }, { parenthesizedAssignableExpression() })
    }

    //parenthesizedAssignableExpression
    //    : LPAREN NL* assignableExpression NL* RPAREN
    //    ;
    fun parenthesizedAssignableExpression(): AssignableExpr {
        return expectAndRecover("(", ")") {
            NLs()
            assignableExpression().also {
                NLs()
            }
        } ?: TODO("parenthesizedAssignableExpression.null")
    }

    //assignableSuffix
    //    : typeArguments
    //    | indexingSuffix
    //    | navigationSuffix
    //    ;
    fun assignableSuffix(expr: Expr): AssignableExpr = OR(
        { TypeArgumentsAssignableSuffixExpr(expr, typeArguments()) },
        { indexingSuffix(expr) },
        { navigationSuffix(expr) }
    )

    //indexingSuffix
    //    : LSQUARE NL* expression (NL* COMMA NL* expression)* (NL* COMMA)? NL* RSQUARE
    //    ;
    fun indexingSuffix(expr: Expr): AssignableExpr =
        IndexedExpr(expr, parseListWithStartEnd("[", "]", oneOrMore = true, separator = { expectOpt(",") }) {
            expression()
        })

    //navigationSuffix
    //    : memberAccessOperator NL* (simpleIdentifier | parenthesizedExpression | CLASS)
    //    ;
    fun navigationSuffix(expr: Expr): AssignableExpr {
        memberAccessOperator()
        NLs()
        OR({ CLASS() }, { parenthesizedExpression() }, { simpleIdentifier() })
        TODO("navigationSuffix")
    }

    data class CallSuffix(
        val typeArgs: List<TypeNode>? = null,
        val args: List<Expr> = emptyList(),
        val lambdaArg: Expr? = null,
    )

    //callSuffix
    //    : typeArguments? (valueArguments? annotatedLambda | valueArguments)
    //    ;
    fun callSuffix(expr: Expr): Expr? {
        val typeArgs = opt { typeArguments() }
        //val args = opt { valueArguments() }
        val args = valueArguments()
        val lambdaArg = if (args == null) {
            annotatedLambda()
        } else {
            opt { annotatedLambda() }
        }

        return CallExpr(expr, args ?: emptyList(), lambdaArg, typeArgs)
    }

    //annotatedLambda
    //    : annotation* label? NL* lambdaLiteral
    //    ;
    fun annotatedLambda(): Expr {
        zeroOrMore { annotation() }
        opt { label() }
        NLs()
        return lambdaLiteral()
    }

    //typeArguments
    //    : LANGLE NL* typeProjection (NL* COMMA NL* typeProjection)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeArguments(): List<TypeNode> {
        return expectAndRecover("<", ">") {
            NLs()
            parseList(oneOrMore = true, separator = { expectOpt(",") }, doBreak = { matches(">") }) {
                typeProjection()
            }.also {
                NLs()
            }
        } ?: error("")
    }

    //valueArguments
    //    : LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
    //    ;
    fun valueArguments(): List<Expr> {
        println("valueArguments: $this")
        return expectAndRecoverSure("(", ")") {
            parseList(oneOrMore = false, separator = { expectOpt(",") }, doBreak = { matches(")") }) {
                valueArgument()
            }
        }.also {
            println("/valueArguments: $this")
        }
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

        if (matches("\"")) return stringLiteral()
        if (matches("[")) return collectionLiteral()
        if (matches("this")) return thisExpression()
        if (matches("if")) return ifExpression()
        if (matches("when")) return whenExpression()
        if (matches("try")) return tryExpression()
        if (matches("super")) return superExpression()
        if (matches("this")) return thisExpression()
        if (matches("{")) return functionLiteral()
        //if (matches("suspend")) return functionLiteral()
        if (matches("fun")) return functionLiteral()
        if (matches("object")) return objectLiteral()

        if (matches("throw") || matches("return") || matches("continue") || matches("break")) return jumpExpression()

        return OR(
            { stringLiteral() },
            { collectionLiteral() },
            { callableReference() },
            { functionLiteral() },
            { objectLiteral() },
            { thisExpression() },
            { superExpression() },
            { ifExpression() },
            { whenExpression() },
            { tryExpression() },
            { jumpExpression() },
            { IdentifierExpr(simpleIdentifier()) },
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
    fun collectionLiteral(): CollectionLiteralExpr {
        return CollectionLiteralExpr(parseListWithStartEnd("[", "]", separator = { expectOpt("," )}) {
            expression()
        })
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
    fun stringLiteral(): Expr = enrich {
        when (expectAnyOpt(TRIPLE_QUOTE, QUOTE)) {
            TRIPLE_QUOTE -> TODO("TRIPLE_QUOTE")
            QUOTE -> {
                val chunks = arrayListOf<InterpolatedStringExpr.Chunk>()
                val str = StringBuilder()
                fun flush() {
                    if (str.isNotEmpty()) {
                        chunks.add(InterpolatedStringExpr.StringChunk(str.toString()))
                        str.clear()
                    }
                }
                while (hasMore) {
                    val c = readChar()
                    when (c) {
                        '"' -> break
                        '\\' -> TODO("Missing quoted strings")
                        '\$' -> {
                            flush()
                            val expr = when {
                                peekChar() == '{' -> {
                                    expectAndRecoverSure("{", "}") {
                                        expression()
                                    }
                                }
                                else -> {
                                    IdentifierExpr(Identifier())
                                }
                            }
                            chunks += InterpolatedStringExpr.ExpressionChunk(expr)
                        }
                        else -> str.append(c)
                    }
                }
                flush()
                if (chunks.isEmpty()) {
                    StringLiteralExpr("")
                } else if (chunks.size == 1 && chunks.first() is InterpolatedStringExpr.StringChunk) {
                    StringLiteralExpr((chunks.first() as InterpolatedStringExpr.StringChunk).string)
                } else {
                    InterpolatedStringExpr(chunks)
                }
            }
            else -> error("Not a string literal")
        }
    }

    //lineStringLiteral
    //    : QUOTE_OPEN (lineStringContent | lineStringExpression)* QUOTE_CLOSE
    //    ;
    fun lineStringLiteral(): Expr {
        QUOTE_OPEN()
        TODO("lineStringLiteral")
    }

    //multiLineStringLiteral
    //    : TRIPLE_QUOTE_OPEN (multiLineStringContent | multiLineStringExpression | MultiLineStringQuote)* TRIPLE_QUOTE_CLOSE
    //    ;
    fun multiLineStringLiteral(): Expr {
        TODO("multiLineStringLiteral")
    }

    //lineStringContent
    //    : LineStrText
    //    | LineStrEscapedChar
    //    | LineStrRef
    //    ;
    fun lineStringContent() {
        TODO("lineStringContent")
    }

    //lineStringExpression
    //    : LineStrExprStart NL* expression NL* RCURL
    //    ;
    fun lineStringExpression() {
        TODO("lineStringExpression")
    }

    //multiLineStringContent
    //    : MultiLineStrText
    //    | MultiLineStringQuote
    //    | MultiLineStrRef
    //    ;
    fun multiLineStringContent() {
        TODO("multiLineStringContent")
    }

    //multiLineStringExpression
    //    : MultiLineStrExprStart NL* expression NL* RCURL
    //    ;
    fun multiLineStringExpression() {
        TODO("multiLineStringExpression")
    }

    //lambdaLiteral
    //    : LCURL NL* (lambdaParameters? NL* ARROW NL*)? statements NL* RCURL
    //    ;
    fun lambdaLiteral(): Expr {
        expectAndRecover("{", "}") {
            opt {
                NLs()
                opt { lambdaParameters() }
                NLs()
                ARROW()
                NLs()
            }
            statements()
            NLs()
        }
        return IncompleteExpr("lambdaLiteral")
    }

    //lambdaParameters
    //    : lambdaParameter (NL* COMMA NL* lambdaParameter)* (NL* COMMA)?
    //    ;
    fun lambdaParameters(): List<Unit> {
        return parseList(oneOrMore = true) { lambdaParameter() }
    }

    //lambdaParameter
    //    : variableDeclaration
    //    | multiVariableDeclaration (NL* COLON NL* type)?
    //    ;
    fun lambdaParameter() {
        OR({ multiVariableDeclaration(); NLs(); COLON(); NLs(); type() }, { variableDeclaration() })
        TODO("lambdaParameter")
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
    fun anonymousFunction(): Expr {
        opt { expect("suspend") }
        NLs()
        expect("fun")
        opt { NLs(); expect("type"); NLs(); DOT() }
        NLs()
        parametersWithOptionalType()
        opt { NLs(); COLON(); NLs(); type() }
        opt { NLs(); typeConstraints() }
        opt { NLs(); functionBody() }
        TODO("anonymousFunction")
    }

    //functionLiteral
    //    : lambdaLiteral
    //    | anonymousFunction
    //    ;
    fun functionLiteral(): Expr = OR({ lambdaLiteral() }, { anonymousFunction() })

    //objectLiteral
    //    : DATA? NL* OBJECT (NL* COLON NL* delegationSpecifiers NL*)? (NL* classBody)?
    //    ;
    fun objectLiteral(): ObjectLiteralExpr {
        val isData = expectOpt("data")
        NLs()
        expect("object")
        val delegationSpecifiers: List<SubTypeInfo>? = opt {
            NLs()
            COLON()
            NLs()
            delegationSpecifiers().also { NLs() }
        }
        val body = opt {
            NLs()
            classBody()
        }
        return ObjectLiteralExpr(delegationSpecifiers = delegationSpecifiers, body = body, isData = isData)
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
        expect("super")
        val type = if (matches("<")) {
            expectAndRecover("<", ">") {
                NLs()
                type().also { NLs() }
            }
        } else {
            null
        }
        val label = if (expectOpt("@")) Identifier() else null
        return SuperExpr(label = label, type = type)
    }

    //ifExpression
    //    : IF NL* LPAREN NL* expression NL* RPAREN NL*
    //      ( controlStructureBody
    //      | controlStructureBody? NL* SEMICOLON? NL* ELSE NL* (controlStructureBody | SEMICOLON)
    //      | SEMICOLON)
    //    ;
    fun ifExpression(): Expr {
        NLs()
        expect("if")
        NLs()
        val cond = expectAndRecover("(", ")") {
            NLs()
            expression().also { NLs() }
        }
        NLs()
        if (expectOpt(";")) {
            return IfExpr(cond, EmptyStm())
        }
        val trueBody = opt { controlStructureBody() }
        NLs()
        opt { SEMICOLON() }
        NLs()
        val falseBody = if (expectOpt("else")) {
            opt { controlStructureBody() }.also {
                NLs()
                opt { SEMICOLON() }
                NLs()
            }
        } else {
            null
        }
        return IfExpr(cond, trueBody, falseBody)
    }

    //whenSubject
    //    : LPAREN (annotation* NL* VAL NL* variableDeclaration NL* ASSIGNMENT NL*)? expression RPAREN
    //    ;
    fun whenSubject(): WhenExpr.Subject {
        var decl: VariableDecl? = null
        val expr = expectAndRecover("(", ")") {
            opt {
                annotations()
                NLs()
                expect("val")
                NLs()
                decl = variableDeclaration()
                NLs()
                ASSIGNMENT()
                NLs()
            }
            expression()
        }
        return WhenExpr.Subject(expr, decl)
    }

    //whenExpression
    //    : WHEN NL* whenSubject? NL* LCURL NL* (whenEntry NL*)* NL* RCURL
    //    ;
    fun whenExpression(): WhenExpr {
        expect("when")
        NLs()
        val subject = opt { whenSubject() }
        NLs()
        val entries = expectAndRecover("{", "}") { zeroOrMore { NLs(); whenEntry() }.also { NLs() } } ?: emptyList()
        return WhenExpr(subject, entries)
    }

    //whenEntry
    //    : whenCondition (NL* COMMA NL* whenCondition)* (NL* COMMA)? NL* ARROW NL* controlStructureBody semi?
    //    | ELSE NL* ARROW NL* controlStructureBody semi?
    //    ;
    fun whenEntry(): WhenExpr.Entry {
        val conditions = if (matches("else")) {
            expect("else")
            null
        } else {
            parseList(separator = { expectOpt(",") }, doBreak = { matches("->") }) {
                whenCondition()
            }
        }
        NLs()
        expect("->")
        NLs()
        val body = controlStructureBody()
        semiOpt()
        return WhenExpr.Entry(conditions, body)
    }

    //whenCondition
    //    : expression
    //    | rangeTest
    //    | typeTest
    //    ;
    fun whenCondition(): WhenExpr.Condition {
        return when {
            matches("in") || matches("!in") || matches("is") || matches("!is") -> WhenExpr.Condition(op = inIsOperator().also { NLs() }, expr = expression())
            else -> WhenExpr.Condition(expr = expression())
        }
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
    //    : TRY NL* block ( (NL* catchBlock)+ (NL* finallyBlock)? | NL* finallyBlock )
    //    ;
    fun tryExpression(): Expr {
        expect("try")
        NLs()
        val body = block()
        val catches = zeroOrMore { NLs(); catchBlock() }
        NLs()
        val finally = finallyBlock()
        return TryCatchExpr(body, catches, finally)
    }

    //catchBlock
    //    : CATCH NL* LPAREN annotation* simpleIdentifier COLON type (NL* COMMA)? RPAREN NL* block
    //    ;
    fun catchBlock(): TryCatchExpr.Catch {
        expect("catch")
        NLs()
        var localName: String = "unknown"
        var type: TypeNode = SimpleType("unknown")
        expectAndRecover("(", ")") {
            annotations()
            localName = simpleIdentifier()
            COLON()
            type = type()
            opt { NLs(); COMMA() }
        }
        NLs()
        val block = block()
        return TryCatchExpr.Catch(localName, type, block)
    }

    //finallyBlock
    //    : FINALLY NL* block
    //    ;
    fun finallyBlock(): Stm {
        expect("finally")
        NLs()
        return block()
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
        //println("TODO: jumpExpression")
        return when {
            expectOpt("throw") -> {
                NLs()
                ThrowExpr(expression())
            }
            expectOpt("return") -> {
                val label = if (expectOpt("@")) Identifier() else null
                val expr = opt { expression() }
                ReturnExpr(expr, label)
            }
            expectOpt("continue") -> {
                val label = if (expectOpt("@")) Identifier() else null
                ContinueExpr(label)
            }
            expectOpt("break") -> {
                val label = if (expectOpt("@")) Identifier() else null
                BreakExpr(label)
            }
            else -> error("Unknown jump expression")
        }
    }

    //callableReference
    //    : receiverType? COLONCOLON NL* (simpleIdentifier | CLASS)
    //    ;
    fun callableReference(): Expr {
        val type = opt { receiverType() }
        expect("::")
        NLs()
        val kind = if (matches("class")) { CLASS(); "class" } else simpleIdentifier()
        return CallableReferenceExt(type, kind)
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

    fun inIsOperator(): String = expectAny("!in", "!is", "in", "is")

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
    fun prefixUnaryOperator(): UnaryPreOp = UnaryPreOp.BY_ID[expectAny("++", "--", "-", "+", "!")]!!

    //postfixUnaryOperator
    //    : INCR
    //    | DECR
    //    | EXCL_NO_WS excl
    //    ;
    fun postfixUnaryOperator(): UnaryPostOp = UnaryPostOp.BY_ID[expectAny("++", "--", "!!")]!!

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
                    else -> error("Invalid member accessor $this")
                }
            }
        }
    }

    //safeNav
    //    : QUEST_NO_WS DOT
    //    ;
    /** ?. */
    //fun safeNav(): Unit = expect("?.")

    // SECTION: modifiers
    //modifiers
    //    : (annotation | modifier)+
    //    ;
    fun modifiers(atLeastOne: Boolean = true): List<Any> {
        return multiple(atLeastOne = atLeastOne) {
            OR({ annotation() }, { modifier() })
        }
    }

    //parameterModifiers
    //    : (annotation | parameterModifier)+
    //    ;
    fun parameterModifiers(atLeastOne: Boolean = true): List<Any> {
        return multiple(atLeastOne = atLeastOne) { OR({ annotation() }, { parameterModifier() }) }
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
    fun modifier(): Modifier {
        return OR(
            { classModifier() },
            { memberModifier() },
            { visibilityModifier() },
            { functionModifier() },
            { propertyModifier() },
            { inheritanceModifier() },
            { parameterModifier() },
            { platformModifier() },
        ).also {
            NLs()
        }
    }

    //typeModifiers
    //    : typeModifier+
    //    ;
    fun typeModifiers(): List<Any> {
        return oneOrMore { typeModifier() }
    }

    //typeModifier
    //    : annotation
    //    | SUSPEND NL*
    //    ;
    fun typeModifier(): Any {
        return OR(
            { expect("suspend"); NLs(); "suspend" },
            { annotation() },
        )
    }

    //classModifier
    //    : ENUM
    //    | SEALED
    //    | ANNOTATION
    //    | DATA
    //    | INNER
    //    | VALUE
    //    ;
    fun classModifier(): ClassModifier? = ClassModifier.BY_ID[expectAnyOpt("enum", "sealed", "annotation", "data", "inner", "value")]

    //memberModifier
    //    : OVERRIDE
    //    | LATEINIT
    //    ;
    fun memberModifier(): MemberModifier? = MemberModifier.BY_ID[expectAnyOpt("override", "lateinit")]

    //visibilityModifier
    //    : PUBLIC
    //    | PRIVATE
    //    | INTERNAL
    //    | PROTECTED
    //    ;
    fun visibilityModifier(): VisibilityModifier = VisibilityModifier.BY_ID[expectAny("public", "private", "internal", "protected")]!!

    //varianceModifier
    //    : IN
    //    | OUT
    //    ;
    fun varianceModifier(): VarianceModifier = VarianceModifier.BY_ID[expectAny("in", "out")]!!

    //typeParameterModifiers
    //    : typeParameterModifier+
    //    ;
    fun typeParameterModifiers() {
        multiple(atLeastOne = true) { typeParameterModifier() }
    }

    //typeParameterModifier
    //    : reificationModifier NL*
    //    | varianceModifier NL*
    //    | annotation
    //    ;
    fun typeParameterModifier() {
        TODO("typeParameterModifier")
    }

    //functionModifier
    //    : TAILREC
    //    | OPERATOR
    //    | INFIX
    //    | INLINE
    //    | EXTERNAL
    //    | SUSPEND
    //    ;
    fun functionModifier(): FunctionModifier = FunctionModifier.BY_ID[expectAny("tailrec", "operator", "infix", "inline", "external", "suspend")]!!

    //propertyModifier
    //    : CONST
    //    ;
    fun propertyModifier(): PropertyModifier = PropertyModifier.BY_ID[expectAny("const")]!!

    //inheritanceModifier
    //    : ABSTRACT
    //    | FINAL
    //    | OPEN
    //    ;
    fun inheritanceModifier(): InheritanceModifier = InheritanceModifier.BY_ID[expectAny("abstract", "final", "open")]!!

    //parameterModifier
    //    : VARARG
    //    | NOINLINE
    //    | CROSSINLINE
    //    ;
    fun parameterModifier(): ParameterModifier = ParameterModifier.BY_ID[expectAny("vararg", "noinline", "crossinline")]!!

    //reificationModifier
    //    : REIFIED
    //    ;
    fun reificationModifier(): ReificationModifier = ReificationModifier.BY_ID[expectAny("reified")]!!

    //platformModifier
    //    : EXPECT
    //    | ACTUAL
    //    ;
    fun platformModifier(): PlatformModifier = PlatformModifier.BY_ID[expectAny("expect", "actual")]!!

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

    fun annotations(atLeastOne: Boolean = false): List<AnnotationNodes> {
        return multiple(atLeastOne = atLeastOne) { annotation() }
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
    //    TODO("multiAnnotation")
    //}

    //annotationUseSiteTarget
    //    : (AT_NO_WS | AT_PRE_WS) (FIELD | PROPERTY | GET | SET | RECEIVER | PARAM | SETPARAM | DELEGATE) NL* COLON
    //    ;
    fun annotationUseSiteTarget() {
        TODO("annotationUseSiteTarget")
    }

    //unescapedAnnotation
    //    : constructorInvocation
    //    | userType
    //    ;
    fun unescapedAnnotation(): AnnotationNode {
        val type = userType()
        val args = opt {
            NLs()
            valueArguments()
        }
        return AnnotationNode(type, args)
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

