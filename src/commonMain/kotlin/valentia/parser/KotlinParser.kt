package valentia.parser

import valentia.ast.*
import valentia.util.Disjunction3

open class KotlinParser(tokens: List<Token>) : TokenReader(tokens), BaseTokenParser {
    constructor(str: String) : this(ValentiaTokenizer(str).tokenize())

    open fun createParser(str: String): KotlinParser = KotlinParser(str)
    open fun createParser(tokens: List<Token>): KotlinParser = KotlinParser(tokens)

    override fun reportError(e: Throwable) {
        debug("reportError: PARSER ERROR: $e")
        e.printStackTrace()
    }

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
        NLs()
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
                expect(".")
                expect("*")
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
    fun topLevelObject(): Decl {
        NLs()
        return declaration().also {
            semis(atLeastOne = false)
        }
    }

    // typeAlias
    //    : modifiers? TYPE_ALIAS NL* simpleIdentifier (NL* typeParameters)? NL* ASSIGNMENT NL* type
    //    ;
    fun typeAlias(): Decl {
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
    fun declaration(): Decl {
        debug("TODO: declaration")
        NLs()
        if (matches("typealias")) return typeAlias()
        if (matches("class")) return classDeclaration()
        if (matches("interface")) return classDeclaration()
        if (matches("object")) return objectDeclaration()
        if (matches("var") || matches("val")) return propertyDeclaration()
        //if (matches("fun")) return functionDeclaration()
        return OR(
            { functionDeclaration() },
            { classDeclaration() },
            { objectDeclaration() },
            { propertyDeclaration() },
            { typeAlias() },
            name = "declaration"
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
        debug("TODO: classDeclaration")
        val modifiers = modifiers(atLeastOne = false)
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
        val types = opt { NLs(); typeParameters() }
        val primaryConstructor = opt { NLs(); primaryConstructor() }
        val subTypes = opt { NLs(); COLON(); NLs(); delegationSpecifiers() }
        val typeConstraints = opt { NLs(); typeConstraints() }
        NLs()
        val decls = if (matches("{")) {
            if (modifiers.isEnum) {
                enumClassBody()
            } else {
                classBody()
            }
            //OR({ classBody() }, { enumClassBody() })
        } else {
            null
        }
        NLs()
        return ClassDecl(kind = kind, name = className, subTypes = subTypes, body = decls)
    }

    // primaryConstructor
    //    : (modifiers? CONSTRUCTOR NL*)? classParameters
    //    ;
    fun primaryConstructor(): List<ClassParameter> {
        debug("TODO: primaryConstructor")
        val optModifiers: Modifiers? = opt { modifiers(atLeastOne = false).also { CONSTRUCTOR(); NLs() } }
        return classParameters()
    }

    //classBody
    //    : LCURL NL* classMemberDeclarations NL* RCURL
    //    ;
    fun classBody(): List<Decl> {
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
        val list = expectAndRecover("(", ")") {
            NLs()
            opt {
                val param = classParameter()
                val extra = zeroOrMore {
                    NLs()
                    COMMA()
                    NLs()
                    classParameter()
                }
                opt {
                    NLs()
                    COMMA()
                }
                listOf(param) + extra
            }.also { NLs() }
        } ?: emptyList()

        return list
        //return parseListWithStartEnd("(", ")", oneOrMore = true, separator = { expectOpt(",") }) {
        //    classParameter()
        //}
    }

    //classParameter
    //    : modifiers? (VAL | VAR)? NL* simpleIdentifier COLON NL* type (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun classParameter(): ClassParameter {
        debug("TODO: classParameter")
        opt { modifiers(atLeastOne = false) }
        opt { expectAnyOpt("val", "var") }
        NLs()
        val id = simpleIdentifier()
        COLON()
        NLs()
        val type = type()
        opt { NLs(); ASSIGNMENT(); NLs(); expression() }
        return ClassParameter(id)
    }

    fun <T> parseListNew(
        separator: () -> Unit = { COMMA() },
        trailingSeparator: Boolean = false,
        oneOrMore: Boolean = true,
        block: () -> T
    ): List<T> {
        val one = if (oneOrMore) block() else opt { block() }
        val more = zeroOrMore {
            NLs()
            separator()
            NLs()
            block()
        }
        if (trailingSeparator) {
            opt {
                NLs()
                separator()
            }
        }
        return listOfNotNull(one) + more
    }

    //delegationSpecifiers
    //    : annotatedDelegationSpecifier (NL* COMMA NL* annotatedDelegationSpecifier)*
    //    ;
    fun delegationSpecifiers(): List<SubTypeInfo> {
        return parseListNew({ COMMA() }, trailingSeparator = false) { annotatedDelegationSpecifier() }
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
            {
                val isSuspend = expectOpt("suspend")
                NLs()
                val res = functionType()
                BasicSubTypeInfo(if (isSuspend) res.suspendable() else res)
            },
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
        debug("TODO: typeParameter")
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
    fun typeConstraints(): List<TypeConstraint> {
        debug("TODO: typeConstraints")
        expect("where")
        NLs()
        val tc = typeConstraint()
        val more = zeroOrMore {
            NLs()
            expect(",")
            NLs()
            typeConstraint()
        }
        return listOf(tc) + more
        //return parseList(oneOrMore = true) { typeConstraint() }
    }

    //typeConstraint
    //    : annotation* simpleIdentifier NL* COLON NL* type
    //    ;
    fun typeConstraint(): TypeConstraint {
        debug("TODO: typeConstraint")
        val annotations = annotations()
        val id = simpleIdentifier()
        NLs()
        COLON()
        NLs()
        val type = type()
        return TypeConstraint(id, type, annotations)
    }

// SECTION: classMembers

    //classMemberDeclarations
    //    : (classMemberDeclaration semis?)*
    //    ;
    fun classMemberDeclarations(): List<Decl> {
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
    fun classMemberDeclaration(): Decl {
        if (matches("constructor")) {
            return secondaryConstructor()
        }
        if (matches("companion")) {
            return companionObject()
        }
        if (matches("init")) {
            return anonymousInitializer()
        }
        return OR(
            { declaration() },
            { companionObject() },
            { anonymousInitializer() },
            { secondaryConstructor() },
            name = "classMemberDeclaration"
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
        debug("TODO: companionObject")
        val modifiers = modifiers(atLeastOne = false)
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
        return expectAndRecoverSure("(", ")") {
            parseListNew(trailingSeparator = true, oneOrMore = false) {
                functionValueParameter()
            }
        }
        //return parseListWithStartEnd("(", ")", oneOrMore = false) {
        //    functionValueParameter()
        //}
    }

    //functionValueParameter
    //    : parameterModifiers? parameter (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameter(): FuncValueParam {
        parameterModifiers(atLeastOne = false)
        val param = parameter()
        opt {
            NLs()
            ASSIGNMENT()
            NLs()
            expression()
        }
        debug("TODO: functionValueParameter")
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
    fun functionDeclaration(): Decl {
        debug("TODO: functionDeclaration")
        val modifiers = modifiers(atLeastOne = false)
        //NLs()
        expect("fun")
        val typeParams = opt {
            NLs()
            typeParameters()
        }
        val receiver = opt {
            NLs()
            receiverType().let {
                var it = it
                NLs()
                if (!matches(".")) {
                    if (it is MultiType && it.types.size >= 2) {
                        it = it.copy(it.types.dropLast(1))
                        pos--
                        pos--
                    }
                }
                DOT()
                it
            }
        }
        NLs()
        val funcName = simpleIdentifier()
        NLs()
        val params = functionValueParameters()
        val retType = opt {
            NLs()
            COLON()
            NLs()
            type()
        }
        val typeConstraints = opt {
            NLs()
            typeConstraints()
        }
        val body = opt {
            NLs()
            functionBody()
        }
        return FunDecl(funcName, params, retType = retType, where = typeConstraints, body = body, modifiers = modifiers)
    }

    //functionBody
    //    : block
    //    | ASSIGNMENT NL* expression
    //    ;
    fun functionBody(): Stm? {
        NLs()
        if (expectOpt("=")) {
            NLs()
            return ReturnStm(expression())
        } else if (matches("{")) {
            return block()
        } else {
            return null
        }
        //debug("TODO: functionBody")
        //return OR(
        //    { block() },
        //    { ASSIGNMENT(); NLs(); ExprStm(expression()) }
        //)
    }

    //variableDeclaration
    //    : annotation* NL* simpleIdentifier (NL* COLON NL* type)?
    //    ;
    fun variableDeclaration(): VariableDecl {
        val annotations = annotations(atLeastOne = false)
        val id = simpleIdentifier()
        val type = opt {
            NLs()
            COLON()
            NLs()
            type()
        }
        return VariableDecl(id, type, annotations = annotations)
    }

    //multiVariableDeclaration
    //    : LPAREN NL* variableDeclaration (NL* COMMA NL* variableDeclaration)* (NL* COMMA)? NL* RPAREN
    //    ;
    fun multiVariableDeclaration(): MultiVariableDecl {
        //val vars = arrayListOf<VariableDecl>()
        val vars = expectAndRecoverSure("(", ")") {
            parseListNew(trailingSeparator = true) { variableDeclaration() }
            /*
            NLs()
            vars += variableDeclaration()
            while (hasMore) {
                NLs()
                if (expectOpt(")")) {
                    skip(-1)
                    break
                }
                if (expectOpt(",")) {
                    NLs()
                    vars += variableDeclaration()
                }
            }

             */
        }
        return MultiVariableDecl(vars)
    }

    fun variableDeclarationOrMultiVariableDeclaration(): VariableDeclBase {
        return OR({ variableDeclaration() }, { multiVariableDeclaration() })
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
    fun propertyDeclaration(): VariableDeclBase {
        debug("TODO: propertyDeclaration")
        val modifiers = modifiers(atLeastOne = false)
        expectAny("val", "var")
        opt { NLs(); typeParameters() }
        opt { NLs(); receiverType(); NLs(); DOT() }
        NLs()
        val decl = OR({ multiVariableDeclaration() }, { variableDeclaration() }, name = "propertyDeclaration.decl")
        opt { typeConstraints() }
        var delegation = false
        NLs()
        val expr = if (matches("by")) {
            delegation = true
            propertyDelegate()
        } else {
            opt { ASSIGNMENT(); NLs(); expression() }
        }

        //val expr = if (expectOpt("by")) {
        //    delegation = true
        //    NLs()
        //    val expr = expression()
        //    expr
        //} else {
        //    opt { ASSIGNMENT(); NLs(); expression() }
        //}
        opt { NLs(); SEMICOLON() }
        NLs()
        zeroOrMore {
            modifiers(atLeastOne = false)
            when {
                matches("get") -> getter()
                matches("set") -> setter()
                else -> return@zeroOrMore null
            }
            NLs()
            semiOpt()
        }
        return decl.let { if (expr != null) it.withAssignment(expr, delegation = delegation) else it }
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
            expectAndRecover("(", ")") {
                NLs()
            }
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
            expectAndRecover("(", ")") {
                NLs()
                functionValueParameterWithOptionalType()
                opt {
                    NLs()
                    COMMA()
                }
            }
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
    //    : LPAREN NL*
    //      (
    //          functionValueParameterWithOptionalType
    //          (NL* COMMA NL* functionValueParameterWithOptionalType)*
    //          (NL* COMMA)?
    //      )?
    //      NL* RPAREN
    //    ;
    fun parametersWithOptionalType(): List<ParameterOptType> {
        return expectAndRecover("(", ")") {
            NLs()
            opt {
                val one = functionValueParameterWithOptionalType()
                val rest = zeroOrMore {
                    NLs()
                    COMMA()
                    NLs()
                    functionValueParameterWithOptionalType()
                }
                opt { NLs(); COMMA() }
                listOfNotNull(one) + rest
            }.also { NLs() }
        } ?: emptyList()
    }

    //functionValueParameterWithOptionalType
    //    : parameterModifiers? parameterWithOptionalType (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameterWithOptionalType(): ParameterOptType {
        val modifiers = parameterModifiers(atLeastOne = false)
        val id = simpleIdentifier().also { NLs() }
        val type = opt { COLON(); NLs(); type() }
        val expr = opt { NLs(); ASSIGNMENT(); NLs(); expression() }
        return ParameterOptType(id, type, modifiers = modifiers, expr = expr)
    }

    //parameterWithOptionalType
    //    : simpleIdentifier NL* (COLON NL* type)?
    //    ;
    //fun parameterWithOptionalType(): ParameterOptType {
    //    return ParameterOptType(id, type)
    //}

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
    fun objectDeclaration(): Decl {
        debug("TODO: objectDeclaration")
        opt { modifiers(atLeastOne = false) }
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
        debug("TODO: secondaryConstructor")
        modifiers(atLeastOne = false)
        CONSTRUCTOR()
        NLs()
        val params = functionValueParameters()
        val constructorDelegationCall = opt {
            NLs()
            COLON()
            NLs()
            constructorDelegationCall()
        }
        NLs()
        val body = opt { block() }
        return ConstructorDecl(params = params, body = body, constructorDelegationCall = constructorDelegationCall)
    }

    //constructorDelegationCall
    //    : (THIS | SUPER) NL* valueArguments
    //    ;
    fun constructorDelegationCall(): ConstructorDelegationCall {
        val kind = expectAny("this", "super")
        NLs()
        val args = valueArguments()
        return ConstructorDelegationCall(kind, args)
    }

    // SECTION: enumClasses
    //enumClassBody
    //    : LCURL NL* enumEntries? (NL* SEMICOLON NL* classMemberDeclarations)? NL* RCURL
    //    ;
    fun enumClassBody(): List<Decl> {
        debug("TODO: enumClassBody")
        expectAndRecover("{", "}") {
            NLs()
            enumEntries(oneOrMore = false)
            opt {
                NLs()
                SEMICOLON()
                NLs()
                classMemberDeclarations()
            }
            NLs()
        }
        return emptyList()
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
        debug("TODO: enumEntry")
        val modifiers = modifiers(atLeastOne = false)
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
        debug("TODO: type")

        val modifiers = typeModifiers(atLeastOne = false)

        // @TODO: Receiver here

        val type: TypeNode = if (matches("(")) {
            val types = functionTypeParameters()
            //val types = expectAndRecoverSure("(", ")") {
            //    parseListNew({ COMMA() }) {
            //        NLs()
            //        type()
            //    }
            //}
            NLs()
            val retType = if (expectOpt("->")) {
                type()
            } else {
                null
            }
            if (retType != null) {
                FuncTypeNode(retType, types)
            } else {
                types.first().type
            }
        } else {
            typeReference()
        }
        NLs()
        val isNullable = expectOpt("?")
        while (matches("?")) skip()
        return if (isNullable) type.nullable() else type

        /*
        val type = when {
            matches("(") -> {
                expectAndRecover("(", ")") {
                    NLs()
                    type().also { NLs() }
                } ?: UnknownType
            }
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

         */
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
        debug("TODO: simpleUserType")
        val id = simpleIdentifier()
        NLs()
        val generic = if (matches("<")) typeArguments() else null
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
            name = "typeProjection"
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
        val receiver = opt { receiverType().also { NLs(); DOT(); NLs() } }
        val typeParams = functionTypeParameters()
        NLs()
        expect("->")
        NLs()
        val ret = type()
        return FuncTypeNode(ret, typeParams, receiver)
    }

    //functionTypeParameters
    //    : LPAREN NL* (parameter | type)? (NL* COMMA NL* (parameter | type))* (NL* COMMA)? NL* RPAREN
    //    ;
    fun functionTypeParameters(): List<NamedTypeNode> {
        return expectAndRecoverSure("(", ")") {
            val first = opt { OR({ NamedTypeNode(parameter()) }, { NamedTypeNode(type()) }) }
            val rest = zeroOrMore {
                NLs()
                COMMA()
                NLs()
                OR({ NamedTypeNode(parameter()) }, { NamedTypeNode(type()) })
            }
            opt {
                NLs()
                COMMA()
            }
            NLs()
            listOfNotNull(first) + rest
        }
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
        val modifiers = zeroOrMore { typeModifier() }
        //return nullableType().withModifiers(modifiers)
        return type().withModifiers(modifiers)
    }

    //parenthesizedUserType
    //    : LPAREN NL* (userType | parenthesizedUserType) NL* RPAREN
    //    ;
    fun parenthesizedUserType(): TypeNode {
        return expectAndRecoverSure("(", ")") {
            NLs()
            OR({ userType() }, { parenthesizedUserType() }).also { NLs() }
        }
    }

    //definitelyNonNullableType
    //    : typeModifiers? (userType | parenthesizedUserType) NL* AMP NL* typeModifiers? (userType | parenthesizedUserType)
    //    ;
    fun definitelyNonNullableType(): DefinitelyNonNullableType {
        val mods1 = typeModifiers(atLeastOne = false)
        val type1 = OR({ userType() }, { parenthesizedUserType() })
        NLs()
        expect("&")
        NLs()
        val mods2 = typeModifiers(atLeastOne = false)
        val type2 = OR({ userType() }, { parenthesizedUserType() })
        return DefinitelyNonNullableType(type1, mods1, type2, mods2)
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
        //NLs()
        val modifiers = Modifiers(zeroOrMore { OR({ label() }, { annotation() }) })
        NLs()
        return if (matches("var") || matches("val")) {
            DeclStm(declaration())
        } else {
            OR(
                { loopStatement() },
                { DeclStm(declaration()) },
                { assignment() },
                { ExprStm(expression()) },
                name = "statement"
            )
        }.withModifiers(modifiers)
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
            matches("{") -> block()
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
        var annotations: Annotations = Annotations.EMPTY
        var expr: Expr = EmptyExpr()
        var vardecl: VariableDeclBase? = null
        expectAndRecoverSure("(", ")") {
            annotations = annotations()
            vardecl = variableDeclarationOrMultiVariableDeclaration()
            expect("in")
            expr = expression()
        }
        NLs()

        val body = if (expectOpt(";")) null else opt { controlStructureBody() }
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
            { assignableExpression().also { Hidden(); op = assignmentAndOperator() } },
            name = "assignment"
        )
        NLs()
        val expr = expression()
        return AssignStm(lvalue, op, expr)
    }

    //semi
    //    : (SEMICOLON | NL) NL*
    //    ;
    fun semi() {
        if (matches(";")) {
            skip(1)
        }
        while (peek() is HiddenToken || peek() is NLToken) {
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
        while (peek().str == ";" || peek() is SpacesToken) {
            skip()
            count++
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
        debug("TODO: prefixUnaryExpression : $prefixes")
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
        debug("TODO: postfixUnaryExpression")
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
        //val oldPos = pos
        //try {
        //    out += block() ?: break
        //} catch (e: IllegalStateException) {
        //    pos = oldPos
        //    break
        //}

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
        if (matches("<") || matches("(")) {
            debug("TODO: postfixUnarySuffix.callSuffix")
            return callSuffix(expr)
        }
        if (matches("[")) {
            val params = expectAndRecoverSure("[", "]") {
                parseList(separator = { expectOpt(",") }, doBreak = { matches("]") }) {
                    expression()
                }
            }
            return IndexedExpr(expr, params)
        }
        val op = expectAnyOpt(".", "?.", "::", consume = false)
        if (op != null) {
            return navigationSuffix(expr)
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
        return if (matches("(")) {
            parenthesizedDirectlyAssignableExpression()
        } else {
            val expr = postfixUnaryExpression()
            if (expr is AssignableExpr) {
                expr
            } else {
                assignableSuffix(expr)
            }
        }

        //return OR(
        //    { parenthesizedDirectlyAssignableExpression() },
        //    { assignableSuffix(postfixUnaryExpression()) },
        //    { IdentifierExpr(simpleIdentifier()) },
        //    name = "directlyAssignableExpression"
        //)
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
        return OR(
            {
                val expr = prefixUnaryExpression()
                //(expr as? AssignableExpr?) ?: TODO("expr=$expr !is AssignableExpr")
                (expr as? AssignableExpr?) ?: error("expr=$expr !is AssignableExpr")
            },
            { parenthesizedAssignableExpression() },
            name = "assignableExpression"
        )
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
    fun assignableSuffix(expr: Expr): AssignableExpr {
        return when {
            matches("<") -> TypeArgumentsAssignableSuffixExpr(expr, typeArguments())
            matches("[") -> indexingSuffix(expr)
            matches("::") || matches("?.") || matches(".") -> navigationSuffix(expr)
            else -> error("Not assignable suffix $this")
        }
    }

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
        val op = memberAccessOperator()
        NLs()
        if (matches("class")) {
            return NavigationExpr(op, expr, "class")
        }
        if (matches("(")) {
            return NavigationExpr(op, expr, parenthesizedExpression())
        }
        val id = simpleIdentifier()
        return NavigationExpr(op, expr, id)
    }

    //data class CallSuffix(
    //    val typeArgs: List<TypeNode>? = null,
    //    val args: List<Expr> = emptyList(),
    //    val lambdaArg: Expr? = null,
    //)

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

        //val args = when {
        //    matches("(") -> valueArguments()
        //    else -> null
        //}
        //val lambdaArg = when {
        //    // @TODO: This can has
        //    matches("{") -> annotatedLambda()
        //    else -> null
        //}
        //if (args == null && lambdaArg == null) return null
        //val lambdaArg = if (args == null) {
        //    annotatedLambda()
        //} else {
        //    opt { annotatedLambda() }
        //}

        return CallExpr(expr, args, lambdaArg, typeArgs)
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
        //println("${this.tokens}")
        return expectAndRecoverSure("<", ">") {
            NLs()
            parseListNew(trailingSeparator = true) {
                typeProjection()
            }.also { NLs() }
        }
    }

    //valueArguments
    //    : LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
    //    ;
    fun valueArguments(): List<Expr> {
        debug("valueArguments: $this")
        return expectAndRecoverSure("(", ")") {
            parseList(oneOrMore = false, separator = { expectOpt(",") }, doBreak = { matches(")") }) {
                valueArgument()
            }
        }.also {
            debug("/valueArguments: $this")
        }
    }

    //valueArgument
    //    : annotation? NL* (simpleIdentifier NL* ASSIGNMENT NL*)? MULT? NL* expression
    //    ;
    fun valueArgument(): Expr {
        val annotations = opt { annotation() }
        NLs()
        val namedArgument = opt { simpleIdentifier().also { NLs();ASSIGNMENT();NLs() } }
        val spread = opt { MULT() }
        NLs()
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
        if (matches("(")) return parenthesizedExpression()
        literalConstantOpt()?.let { return it }

        debug("TODO: primaryExpression")

        if (peek() is StringInterpolationToken) return stringLiteral()
        if (matches("[")) return collectionLiteral()
        if (matches("this")) return thisExpression()
        if (matches("if")) return ifExpression()
        if (matches("when")) return whenExpression()
        if (matches("try")) return tryExpression()
        if (matches("super")) return superExpression()
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
        return expectAndRecoverSure("(", ")") { NLs(); expression().also { NLs() } }
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
        Hidden()
        if (peek() is CharLiteralToken) {
            return CharacterLiteral()
        }
        expectAnyOpt("false", "true")?.let { return BoolLiteralExpr(it == "true") }
        if (expectOpt("null")) return NullLiteralExpr()
        val token = expectOpt<NumberToken>() ?: return null
        if (!token.isFloat) {
            return IntLiteralExpr(token.value.toLong(), token.isLong, token.isUnsigned)
        }
        TODO("literalConstantOpt: token=$token")
        //val numLit = when {
        //    expectOpt("0x") || expectOpt("0X") -> numericLiteral(radix = 16) { HexDigit(it) }
        //    expectOpt("0o") || expectOpt("0O") -> numericLiteral(radix = 8) { it in '0'..'7' }
        //    expectOpt("0b") || expectOpt("0B") -> numericLiteral(radix = 2) { it in '0'..'1' }
        //    else -> numericLiteral(radix = 10) { DecDigit(it) }
        //} ?: return null
        //val isUnsigned = expectAnyOpt("u", "U")
        //val isLong = expectAnyOpt("l", "L")
        //return numLit.copy(isLong = isLong != null, isUnsigned = isUnsigned != null)
    }

    //stringLiteral
    //    : lineStringLiteral
    //    | multiLineStringLiteral
    //    ;
    fun stringLiteral(): Expr = enrich {
        val str = expect<StringInterpolationToken>()
        val chunks = str.tokens.map { token ->
            when (token) {
                is EscapeStringPartToken -> InterpolatedStringExpr.StringChunk("${token.c}")
                is ExpressionStringPartToken -> InterpolatedStringExpr.ExpressionChunk(createParser(token.expr).expression())
                is LiteralStringPartToken -> InterpolatedStringExpr.StringChunk(token.str)
            }
        }
        return@enrich if (chunks.isEmpty()) {
            StringLiteralExpr("")
        } else if (chunks.size == 1 && chunks.first() is InterpolatedStringExpr.StringChunk) {
            StringLiteralExpr((chunks.first() as InterpolatedStringExpr.StringChunk).string)
        } else {
            InterpolatedStringExpr(chunks)
        }

        /*
        TODO("stringLiteral $str")
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
            }
            else -> error("Not a string literal")
        }

         */
    }

    //lineStringLiteral
    //    : QUOTE_OPEN (lineStringContent | lineStringExpression)* QUOTE_CLOSE
    //    ;
    //fun lineStringLiteral(): Expr {
    //    QUOTE_OPEN()
    //    TODO("lineStringLiteral")
    //}

    //multiLineStringLiteral
    //    : TRIPLE_QUOTE_OPEN (multiLineStringContent | multiLineStringExpression | MultiLineStringQuote)* TRIPLE_QUOTE_CLOSE
    //    ;
    //fun multiLineStringLiteral(): Expr {
    //    TODO("multiLineStringLiteral")
    //}

    //lineStringContent
    //    : LineStrText
    //    | LineStrEscapedChar
    //    | LineStrRef
    //    ;
    //fun lineStringContent() {
    //    TODO("lineStringContent")
    //}

    //lineStringExpression
    //    : LineStrExprStart NL* expression NL* RCURL
    //    ;
    //fun lineStringExpression() {
    //    TODO("lineStringExpression")
    //}

    //multiLineStringContent
    //    : MultiLineStrText
    //    | MultiLineStringQuote
    //    | MultiLineStrRef
    //    ;
    //fun multiLineStringContent() {
    //    TODO("multiLineStringContent")
    //}

    //multiLineStringExpression
    //    : MultiLineStrExprStart NL* expression NL* RCURL
    //    ;
    //fun multiLineStringExpression() {
    //    TODO("multiLineStringExpression")
    //}

    //lambdaLiteral
    //    : LCURL NL* (lambdaParameters? NL* ARROW NL*)? statements NL* RCURL
    //    ;
    fun lambdaLiteral(): Expr {
        debug("TODO: lambdaLiteral")
        val (params, stms) = expectAndRecoverSure("{", "}") {
            val params = opt {
                NLs()
                opt { lambdaParameters() }.also {
                    NLs()
                    ARROW()
                    NLs()
                }
            }
            val stms = statements(oneOrMore = false)
            NLs()
            params to stms
        }
        return LambdaFunctionExpr(stms, params)
    }

    //lambdaParameters
    //    : lambdaParameter (NL* COMMA NL* lambdaParameter)* (NL* COMMA)?
    //    ;
    fun lambdaParameters(): List<Unit> {
        return parseListNew(trailingSeparator = true) { lambdaParameter() }
    }

    //lambdaParameter
    //    : variableDeclaration
    //    | multiVariableDeclaration (NL* COLON NL* type)?
    //    ;
    fun lambdaParameter(): VariableDeclBase {
        if (matches("(")) {
            val mvd = multiVariableDeclaration();
            val type = opt {
                NLs()
                COLON()
                NLs()
                type()
            }
            return mvd.copy(type = type)
        } else {
            return variableDeclaration()
        }
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
    fun anonymousFunction(): AnonymousFunctionExpr {
        val isSuspend = expectOpt("suspend")
        NLs()
        expect("fun")
        val receiver = opt { NLs(); type().also { NLs(); DOT() } }
        NLs()
        val paramsWithOptType = parametersWithOptionalType()
        val retType = opt { NLs(); COLON(); NLs(); type() }
        val typeConstraints = opt { NLs(); typeConstraints() }
        val body = opt { NLs(); functionBody() }
        return AnonymousFunctionExpr(FunDecl("", paramsWithOptType.map { FuncValueParam(it.id, it.type ?: UnknownType) }, retType, receiver = receiver, where = typeConstraints, body = body, modifiers = if (isSuspend) Modifiers(FunctionModifier.SUSPEND) else Modifiers.EMPTY))
    }

    //functionLiteral
    //    : lambdaLiteral
    //    | anonymousFunction
    //    ;
    fun functionLiteral(): Expr {
        if (matches("{")) return lambdaLiteral()
        return anonymousFunction()
    }

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
        val cond = expectAndRecoverSure("(", ")") {
            NLs()
            expression().also { NLs() }
        }
        NLs()
        val trueBody = if (expectOpt(";")) {
            EmptyStm()
        } else {
            controlStructureBody()
        }
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
        val subject = when {
            matches("(") -> whenSubject()
            else -> null
        }
        NLs()
        val entries = expectAndRecover("{", "}") {
            zeroOrMore {
                NLs()
                whenEntry()
            }.also { NLs() }
        } ?: emptyList()
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
        val finally = opt { finallyBlock() }
        return TryCatchExpr(body, catches, finally)
    }

    //catchBlock
    //    : CATCH NL* LPAREN annotation* simpleIdentifier COLON type (NL* COMMA)? RPAREN NL* block
    //    ;
    fun catchBlock(): TryCatchExpr.Catch {
        expect("catch")
        NLs()
        val (annotations, localName, type) = expectAndRecoverSure("(", ")") {
            val annotations = annotations()
            val localName = simpleIdentifier().also { COLON() }
            val type = type().also { opt { NLs(); COMMA() } }
            Triple(annotations, localName, type)
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
    fun assignmentAndOperator(): String = expectAny(ASSIGNMENT_OPERATOR)

    //equalityOperator
    //    : EXCL_EQ
    //    | EXCL_EQEQ
    //    | EQEQ
    //    | EQEQEQ
    //    ;
    fun equalityOperator(): String = expectAny(EQUALITY_OPERATOR)

    //comparisonOperator
    //    : LANGLE
    //    | RANGLE
    //    | LE
    //    | GE
    //    ;
    fun comparisonOperator(): String = expectAny(COMPARISON_OPERATOR)

    fun inIsOperator(): String = expectAny(IN_IS_OPERATOR)

    //inOperator
    //    : IN
    //    | NOT_IN
    //    ;
    fun inOperator(): String = expectAny(IN_OPERATOR)

    //isOperator
    //    : IS
    //    | NOT_IS
    //    ;
    fun isOperator(): String = expectAny(IS_OPERATOR)

    //additiveOperator
    //    : ADD
    //    | SUB
    //    ;
    fun additiveOperator(): String = expectAny(ADDITIVE_OPERATOR)

    //multiplicativeOperator
    //    : MULT
    //    | DIV
    //    | MOD
    //    ;
    fun multiplicativeOperator(): String = expectAny(MULTIPLICATIVE_OPERATOR)

    //asOperator
    //    : AS
    //    | AS_SAFE
    //    ;
    fun asOperator(): String = expectAny(AS_OPERATOR)

    //prefixUnaryOperator
    //    : INCR
    //    | DECR
    //    | SUB
    //    | ADD
    //    | excl
    //    ;
    fun prefixUnaryOperator(): UnaryPreOp = expectAny(UnaryPreOp.BY_ID)

    //postfixUnaryOperator
    //    : INCR
    //    | DECR
    //    | EXCL_NO_WS excl
    //    ;
    fun postfixUnaryOperator(): UnaryPostOp = expectAny(UnaryPostOp.BY_ID)

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
    fun modifiers(atLeastOne: Boolean): Modifiers {
        return Modifiers(multiple(atLeastOne = atLeastOne) {
            NLs()
            when {
                matches("@") -> annotation()
                else -> modifier()
            }
        })
    }

    //parameterModifiers
    //    : (annotation | parameterModifier)+
    //    ;
    fun parameterModifiers(atLeastOne: Boolean = true): Modifiers {
        return Modifiers(multiple(atLeastOne = atLeastOne) { OR({ annotation() }, { parameterModifier() }) })
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
    fun modifier(): Modifier? {
        Hidden()
        val token = read()
        return ALL_MODIFIERS[token.str].also { NLs() }
    }

    //typeModifiers
    //    : typeModifier+
    //    ;
    fun typeModifiers(atLeastOne: Boolean = true): Modifiers {
        return Modifiers(multiple(atLeastOne = atLeastOne) { typeModifier() })
    }

    //typeModifier
    //    : annotation
    //    | SUSPEND NL*
    //    ;
    fun typeModifier(): Any {
        Hidden()
        if (expectOpt("suspend")) {
            NLs()
            return FunctionModifier.SUSPEND
        }
        return annotation()
    }

    //classModifier
    //    : ENUM
    //    | SEALED
    //    | ANNOTATION
    //    | DATA
    //    | INNER
    //    | VALUE
    //    ;
    fun classModifier(): ClassModifier? = expectAnyOpt(ClassModifier.BY_ID)

    //memberModifier
    //    : OVERRIDE
    //    | LATEINIT
    //    ;
    fun memberModifier(): MemberModifier? = expectAnyOpt(MemberModifier.BY_ID)

    //visibilityModifier
    //    : PUBLIC
    //    | PRIVATE
    //    | INTERNAL
    //    | PROTECTED
    //    ;
    fun visibilityModifier(): VisibilityModifier = expectAny(VisibilityModifier.BY_ID)

    //varianceModifier
    //    : IN
    //    | OUT
    //    ;
    fun varianceModifier(): VarianceModifier = expectAny(VarianceModifier.BY_ID)

    //typeParameterModifiers
    //    : typeParameterModifier+
    //    ;
    fun typeParameterModifiers(): Modifiers = Modifiers(multiple(atLeastOne = true) { typeParameterModifier() })

    //typeParameterModifier
    //    : reificationModifier NL*
    //    | varianceModifier NL*
    //    | annotation
    //    ;
    fun typeParameterModifier(): Any {
        return OR(
            { reificationModifier(); NLs() },
            { varianceModifier(); NLs() },
            { annotation() },
        )
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
        val nodes = if (matches("[")) {
            expectAndRecoverSure("[", "]") {
                AnnotationNodes(oneOrMore { unescapedAnnotation() })
            }
        } else {
            AnnotationNodes(listOf(unescapedAnnotation()))
        }
        return AnnotationNodes(nodes.annotations.map { it.copy(useSite = useSite) })
    }

    fun annotations(atLeastOne: Boolean = false): Annotations {
        return Annotations(multiple(atLeastOne = atLeastOne) { annotation() })
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
    //fun annotationUseSiteTarget() {
    //    println("TODO: annotationUseSiteTarget")
    //}

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
        val res = expectAnyOpt(SIMPLE_IDENTIFIER) ?: Identifier()
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
        return expect<ShebangToken>().str
    }

    //DelimitedComment
    //    : '/*' ( DelimitedComment | . )*? '*/'
    //      -> channel(HIDDEN)
    //    ;
    //fun DelimitedComment(): Unit {
    //    expect("/*")
    //    while (hasMore) {
    //        if (matches("/*")) DelimitedComment()
    //        if (expectOpt("*/")) break
    //        skip(1)
    //    }
    //}

    //LineComment
    //    : '//' ~[\r\n]*
    //      -> channel(HIDDEN)
    //    ;
    //fun LineComment(): Unit {
    //    expect("//")
    //    readUntil { it == '\r' || it == '\n' }
    //}

    //WS
    //    : [\u0020\u0009\u000C]
    //      -> channel(HIDDEN)
    //    ;
    /** WhiteSpace */
    //fun WS(): Unit = TODO("WS")

    //NL: '\n' | '\r' '\n'?;
    fun NL(): Unit {
        if (peek() is NLToken) {
            skip()
        }
    }

    // NL*
    fun NLs(): Unit {
        loop@while (hasMore) {
            Hidden()
            if (peek() is NLToken) NL() else break@loop
        }
        Hidden()
    }

    //fragment Hidden: DelimitedComment | LineComment | WS;
    //override
    fun Hidden(): Unit {
        loop@while (hasMore) {
            val c = peek()
            when (c) {
                is HiddenToken -> skip()
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
    //fun EXCL_WS(): Unit = TODO("! Hidden")

    //EXCL_NO_WS: '!';
    //fun EXCL_NO_WS(): Unit = TODO("!")

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
    //fun AT_POST_WS(): Unit = TODO("'@' (Hidden | NL)")

    //AT_PRE_WS: (Hidden | NL) '@' ;
    //fun AT_PRE_WS(): Unit = TODO("(Hidden | NL) '@'")

    //AT_BOTH_WS: (Hidden | NL) '@' (Hidden | NL);
    //fun AT_BOTH_WS(): Unit = TODO("(Hidden | NL) '@' (Hidden | NL)")

    //QUEST_WS: '?' Hidden;
    //fun QUEST_WS(): Unit = TODO("'?' Hidden")

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
    //fun RETURN_AT(): Unit = TODO("return@ Identifier")

    //CONTINUE_AT: 'continue@' Identifier;
    //fun CONTINUE_AT(): Unit = TODO("continue@ Identifier")

    //BREAK_AT: 'break@' Identifier;
    //fun BREAK_AT(): Unit = TODO("break@ Identifier")

    //THIS_AT: 'this@' Identifier;
    //fun THIS_AT(): Unit = TODO("this@ Identifier")

    //SUPER_AT: 'super@' Identifier;
    //fun SUPER_AT(): Unit = TODO("super@' Identifier")

    //FILE: 'file';
    fun FILE(): Unit = expect("file")

    //FIELD: 'field';
    fun FIELD(): Unit = expect("field")

    //PROPERTY: 'property';
    fun PROPERTY(): Unit = expect("property")

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
    //fun NOT_IS(): Unit = TODO()

    //NOT_IN: '!in' (Hidden | NL);
    //fun NOT_IN(): Unit = TODO()

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
    //fun DecDigits(): Unit = TODO("DecDigits")

    //fragment DoubleExponent: [eE] [+-]? DecDigits;
    //fun DoubleExponent(): Unit = TODO("DoubleExponent")

    //RealLiteral
    //    : FloatLiteral
    //    | DoubleLiteral
    //    ;
    //fun RealLiteral(): Unit = TODO("RealLiteral")

    //FloatLiteral
    //    : DoubleLiteral [fF]
    //    | DecDigits [fF]
    //    ;
    //fun FloatLiteral(): Unit = TODO("FloatLiteral")

    //DoubleLiteral
    //    : DecDigits? '.' DecDigits DoubleExponent?
    //    | DecDigits DoubleExponent
    //    ;
    //fun DoubleLiteral(): Unit = TODO("DoubleLiteral")

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

    //fun numericLiteralSure(radix: Int, isDigit: (Char) -> Boolean): IntLiteralExpr {
    //    return numericLiteral(radix, isDigit) ?: error("Not a base-$radix literal in $this")
    //}

    //fun numericLiteral(radix: Int, isDigit: (Char) -> Boolean): IntLiteralExpr? {
    //    var n = 0
    //    val c = peekChar(n)
    //    var lastC = c
    //    if (!isDigit(c)) return null
    //    while (hasMore) {
    //        n++
    //        val c = peekChar(n)
    //        lastC = c
    //        if (!(isDigit(c) || c == '_')) break
    //    }
    //    if (lastC == '_') return null
    //    return IntLiteralExpr(read(n).replace("_", "").toLong(radix = radix))
    //}

    //HexLiteral
    //    : '0' [xX] HexDigit HexDigitOrSeparator* HexDigit
    //    | '0' [xX] HexDigit
    //    ;
    //fun HexLiteral(c: Char): Unit = TODO("HexLiteral")
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
    fun CharacterLiteral(): CharLiteralExpr {
        val c = expect<CharLiteralToken>()
        return CharLiteralExpr(c.char)
        //expect("'")
        //val str = StringBuilder()
        //loop@while (hasMore) {
        //    val c = peekChar()
        //    when (c) {
        //        '\\' -> TODO("Not implemented character escaping")
        //        '\'' -> break@loop
        //        else -> str.append(readChar())
        //    }
        //}
        //expect("'")
        //return CharLiteralExpr(str.toString().firstOrNull() ?: '\u0000')
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
        val idToken = expect<IDToken>()
        if (idToken.str in HARD_KEYWORDS) {
            error("not an identifier")
        }
        return idToken.str
        //var n = 0
        //val c = peekChar()
        //if (c == '`') {
        //    error("backticked identifier")
        //}
        //if (!(Letter(c) || c == '_')) {
        //    error("Not a valid identifier starting with '$c'")
        //}
        //while (hasMore) {
        //    n++
        //    val c = peekChar(n)
        //    if (!(Letter(c) || c == '_' || UnicodeDigit(c))) {
        //        break
        //    }
        //}
        //val str = peek(n)
        //when (str) {
        //    "return", "for", "while", "do", "else", "when", "if", "super", "is", "in", "class", "interface", "fun", "var", "val"
        //    -> error("not an identifier")
        //}
        //skip(n)
        //return str
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
    //fun IdentifierOrSoftKey() {
    //    TODO("IdentifierOrSoftKey")
    //}

    //FieldIdentifier
    //    : '$' IdentifierOrSoftKey
    //    ;
    //fun FieldIdentifier() {
    //    TODO("FieldIdentifier")
    //}

    //fragment UniCharacterLiteral
    //    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    //    ;
    //fun UniCharacterLiteral(): Unit = TODO()

    //fragment EscapedIdentifier
    //    : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | '$')
    //    ;
    //fun EscapedIdentifier(): Unit = TODO()

    //fragment EscapeSeq
    //    : UniCharacterLiteral
    //    | EscapedIdentifier
    //    ;
    //fun EscapeSeq(): Unit = TODO()

    // SECTION: characters
    //fragment Letter
    //    : UNICODE_CLASS_LU
    //    | UNICODE_CLASS_LL
    //    | UNICODE_CLASS_LT
    //    | UNICODE_CLASS_LM
    //    | UNICODE_CLASS_LO
    //    ;
    //fun Letter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

    // SECTION: strings
    //QUOTE_OPEN: '"' -> pushMode(LineString);
    val QUOTE: String get() = "\""
    //fun QUOTE_OPEN(): Unit = TODO()

    //TRIPLE_QUOTE_OPEN: '"""' -> pushMode(MultiLineString);
    val TRIPLE_QUOTE: String get() = "\"\"\""
    //fun TRIPLE_QUOTE_OPEN(): Unit = TODO()

    //mode LineString;
    //
    //QUOTE_CLOSE
    //    : '"' -> popMode
    //    ;
    //fun QUOTE_CLOSE(): Unit = TODO()

    //LineStrRef
    //    : FieldIdentifier
    //    ;
    //fun LineStrRef(): Unit = TODO()

    //LineStrText
    //    : ~('\\' | '"' | '$')+ | '$'
    //    ;
    //fun LineStrText(): Unit = TODO()

    //LineStrEscapedChar
    //    : EscapedIdentifier
    //    | UniCharacterLiteral
    //    ;
    //fun LineStrEscapedChar(): Unit = TODO()

    //LineStrExprStart
    //    : '${' -> pushMode(DEFAULT_MODE)
    //    ;
    //fun LineStrExprStart(): Unit = TODO()

    //mode MultiLineString;
    //
    //TRIPLE_QUOTE_CLOSE
    //    : MultiLineStringQuote? '"""' -> popMode
    //    ;
    //fun TRIPLE_QUOTE_CLOSE(): Unit = TODO()

    //MultiLineStringQuote
    //    : '"'+
    //    ;
    //fun MultiLineStringQuote(): Unit = TODO()

    //MultiLineStrRef
    //    : FieldIdentifier
    //    ;
    //fun MultiLineStrRef(): Unit = TODO()

    //MultiLineStrText
    //    :  ~('"' | '$')+ | '$'
    //    ;
    //fun MultiLineStrText(): Unit = TODO()

    //MultiLineStrExprStart
    //    : '${' -> pushMode(DEFAULT_MODE)
    //    ;
    //fun MultiLineStrExprStart(): Unit = TODO()

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

    override fun EOF() {
        NLs()
        check(eof) { "Not EOF found but ${peek()}" }
    }

    companion object {
        val HARD_KEYWORDS = setOf(
            "return", "for", "while", "do", "else", "when", "if", "super", "is", "in", "class", "interface", "fun", "var", "val"
        )

        val SIMPLE_IDENTIFIER = setOf(
            "abstract", "annotation", "by", "catch", "companion", "constructor", "crossinline", "data", "dynamic",
            "enum", "external", "final", "finally", "get", "import", "infix", "init", "inline", "inner", "internal",
            "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public",
            "reified", "sealed", "tailrec", "set", "vararg", "where", "field", "property", "receiver", "param",
            "setparam", "delegate", "file", "expect", "actual", "const", "suspend", "value"
        )

        val ASSIGNMENT_OPERATOR = setOf("+=", "-=", "*=", "/=", "%=")
        val EQUALITY_OPERATOR = setOf("!==", "===", "!=", "==")
        val COMPARISON_OPERATOR = setOf(">=", "<=", "<", ">")
        val IS_OPERATOR = setOf("!is", "is")
        val IN_OPERATOR = setOf("!in", "in")
        val IN_IS_OPERATOR = IS_OPERATOR + IN_OPERATOR
        val ADDITIVE_OPERATOR = setOf("+", "-")
        val MULTIPLICATIVE_OPERATOR = setOf("*", "/", "%")
        val AS_OPERATOR = setOf("as?", "as")

        val VARIANCE_MODIFIERS = VarianceModifier.entries.map { it.id }.toSet()
        val REIFICATION_MODIFIERS = ReificationModifier.entries.map { it.id }.toSet()

        val CLASS_MODIFIERS = ClassModifier.entries.associateBy { it.id }
        val MEMBER_MODIFIERS = MemberModifier.entries.associateBy { it.id }
        val VISIBILITY_MODIFIERS = VisibilityModifier.entries.associateBy { it.id }
        val FUNCTION_MODIFIERS = FunctionModifier.entries.associateBy { it.id }
        val PROPERTY_MODIFIERS = PropertyModifier.entries.associateBy { it.id }
        val INHERITANCE_MODIFIERS = InheritanceModifier.entries.associateBy { it.id }
        val PARAMETER_MODIFIERS = ParameterModifier.entries.associateBy { it.id }
        val PLATFORM_MODIFIERS = PlatformModifier.entries.associateBy { it.id }

        val ALL_MODIFIERS: Map<String, Modifier> = CLASS_MODIFIERS + MEMBER_MODIFIERS + VISIBILITY_MODIFIERS + FUNCTION_MODIFIERS + PROPERTY_MODIFIERS + INHERITANCE_MODIFIERS + PARAMETER_MODIFIERS + PLATFORM_MODIFIERS
    }
}
