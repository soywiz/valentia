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
        val shebang = shebangLineOpt()
        NLs()
        val fileAnnotations = zeroOrMore { fileAnnotation() }
        val _package = packageHeader()
        val imports = importList()
        val topLevelDecls = arrayListOf<Decl>()
        while (true) {
            NLs()
            if (eof) break
            val topLevelObject = topLevelObject()
                ?: error("Expected topLevelObject but found ${peek()} $this")
            topLevelDecls += topLevelObject
        }
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
        opt { shebangLineOpt() }
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
    fun shebangLineOpt(): String? {
        return ShebangLineOpt().also { NLs() }
    }

    // fileAnnotation
    //    : (AT_NO_WS | AT_PRE_WS) FILE NL* COLON NL* (LSQUARE unescapedAnnotation+ RSQUARE | unescapedAnnotation) NL*
    //    ;
    fun fileAnnotation(): AnnotationNodes? {
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
    fun importHeader(): ImportNode? {
        if (!expectOpt("import")) return null
        val id = identifier()
        val all = if (matches(".") && peek(1).str == "*") { skip(2); true } else false
        var alias: String? = importAlias()
        semiOpt()
        return ImportNode(id, alias, all)
    }

    // importAlias
    //    : AS simpleIdentifier
    //    ;
    fun importAlias(): String? {
        if (!expectOpt("as")) return null
        return simpleIdentifier()
    }

    // topLevelObject
    //    : declaration semis?
    //    ;
    fun topLevelObject(): Decl? {
        NLs()
        return declaration()?.also {
            semis(atLeastOne = false)
        }
    }

    // typeAlias
    //    : modifiers? TYPE_ALIAS NL* simpleIdentifier (NL* typeParameters)? NL* ASSIGNMENT NL* type
    //    ;
    fun typeAlias(modifiers: Modifiers): TypeAliasDecl? {
        if (!expectOpt("typealias")) return null
        NLs()
        val id = simpleIdentifier() ?: error("Typealias not includes a valid identifier")
        NLs()
        val types = if (matches("<")) typeParameters() else null
        NLs()
        expect("=")
        NLs()
        val type = type() ?: error("Expected type")
        return TypeAliasDecl(id, type, types, modifiers)
    }

    // declaration
    //    : classDeclaration
    //    | objectDeclaration
    //    | functionDeclaration
    //    | propertyDeclaration
    //    | typeAlias
    //    ;
    fun declaration(mods: Modifiers = modifiers()): Decl? {
        debug("TODO: declaration")
        return when (val str = peek().str) {
            "typealias" -> typeAlias(mods)!!
            "class", "interface" -> classDeclaration(mods)
            "object" -> objectDeclaration(mods)
            "var", "val" -> propertyDeclaration(mods)
            "fun" -> classDeclarationOpt(mods) ?: functionDeclaration(mods)
            else -> null
        }
    }

// SECTION: classes

    // classDeclaration
    //    : modifiers? (CLASS | (FUN NL*)? INTERFACE) NL* simpleIdentifier
    //      (NL* typeParameters)? (NL* primaryConstructor)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* typeConstraints)?
    //      (NL* classBody | NL* enumClassBody)?
    //    ;
    fun classDeclarationOpt(modifiers: Modifiers): ClassDecl? {
        if (matches("class")) return classDeclaration(modifiers)
        if (matches("interface")) return classDeclaration(modifiers)
        if (matches("fun") && (peekSkipping(1).str == "interface")) return classDeclaration(modifiers)
        return null
    }
    fun classDeclaration(modifiers: Modifiers): ClassDecl {
        debug("TODO: classDeclaration")
        val kind = OR(
            { if (expectOpt("class")) "class" else null },
            {
                val isFun = expectOpt("fun")
                NLs()
                if (!expectOpt("interface")) return@OR null
                if (isFun) "fun interface" else "interface"
            },
        )
        NLs()
        val className = simpleIdentifier()
        NLs()
        val types = if (matches("<")) typeParameters() else null
        NLs()
        val mods = modifiers()
        val primaryConstructor = if (matches("constructor") || matches("(")) primaryConstructor(mods) else null
        NLs()
        val subTypes = if (expectOpt(":")) { NLs(); delegationSpecifiers() } else null
        NLs()
        val typeConstraints = if (matches("where")) typeConstraints() else null
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
    fun primaryConstructor(mods: Modifiers = modifiers()): List<ClassParameter> {
        debug("TODO: primaryConstructor")
        if (expectOpt("constructor")) {
            NLs()
        }
        return classParameters()
    }

    //classBody
    //    : LCURL NL* classMemberDeclarations NL* RCURL
    //    ;
    fun classBody(): List<Decl>? {
        if (!matches("{")) return null
        return expectAndRecover("{", "}", reason = "classBody") {
            NLs(); classMemberDeclarations().also { NLs() }
        }
    }

    //classParameters
    //    : LPAREN NL* (classParameter (NL* COMMA NL* classParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    //
    fun classParameters(): List<ClassParameter> {
        val list = expectAndRecover("(", ")") {
            NLs()
            parseListNew({ expectOpt(",") }, oneOrMore = false, trailingSeparator = true, end = { matches(")") }) {
                classParameter()
            }.also {
                NLs()
            }
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
        var modifiers: Modifiers = modifiers()
        val valOrVar = expectAnyOpt("val", "var")
        NLs()
        //val id: String = simpleIdentifier()
        val id: String = simpleIdentifierOpt()
            ?: modifiers.items.last().toString().also {
                modifiers = modifiers.copy(modifiers.items.dropLast(1))
            }
        COLON()
        NLs()
        val type = type()
        val expr = if (expectOptNLs("=")) {
            NLs(); expression()
        } else null
        return ClassParameter(id)
    }

    fun <T> parseListNew(
        separator: () -> Boolean = { expectOpt(",") },
        trailingSeparator: Boolean = false,
        oneOrMore: Boolean = true,
        end: () -> Boolean = { eof },
        block: () -> T?
    ): List<T>? {
        val spos = pos
        NLs()
        val out = arrayListOf<T>()
        while (hasMore) {
            NLs()
            if (end()) break
            NLs()
            out += block() ?: break
            if (!separator()) break
        }
        if (trailingSeparator) {
            separator()
            NLs()
        }
        if (oneOrMore && out.isEmpty()) {
            pos = spos
            //error("oneOrMore=true : $out")
            return null
        }
        return out
    }

    //delegationSpecifiers
    //    : annotatedDelegationSpecifier (NL* COMMA NL* annotatedDelegationSpecifier)*
    //    ;
    fun delegationSpecifiers(): List<SubTypeInfo>? {
        return parseListNew({ expectOpt(",") }, end = { matches("{") }, oneOrMore = true, trailingSeparator = false) {
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
    fun delegationSpecifier(): SubTypeInfo? {
        return ORNullable(
            { constructorInvocation() },
            { explicitDelegation() },
            { userType()?.let { BasicSubTypeInfo(it) } },
            {
                val isSuspend = expectOpt("suspend")
                NLs()
                functionType()?.let { BasicSubTypeInfo(if (isSuspend) it.suspendable() else it) }
            },
        )
    }

    //constructorInvocation
    //    : userType NL* valueArguments
    //    ;
    fun constructorInvocation(): ConstructorInvocation? {
        val spos = pos
        val type = userType() ?: return null.also { pos = spos }
        NLs()
        val args = valueArguments() ?: return null.also { pos = spos }
        return ConstructorInvocation(type, args)
    }

    //annotatedDelegationSpecifier
    //    : annotation* NL* delegationSpecifier
    //    ;
    fun annotatedDelegationSpecifier(): SubTypeInfo? {
        val annotations = annotations(atLeastOne = false).also { NLs() }
        return delegationSpecifier()?.annotated(annotations)
    }
    data class ExpressionState(val allowLambda: Boolean) {
        companion object {
            val DISALLOW_LAMBDA = ExpressionState(allowLambda = false)
        }
    }

    //explicitDelegation
    //    : (userType | functionType) NL* BY NL* expression
    //    ;
    fun explicitDelegation(): ExplicitDelegation? {
        val spos = pos
        val type = ORNullable(
            { userType() },
            { functionType() }
        ) ?: return null.also { pos = spos }
        if (!expectOptNLs("by")) return null.also { pos = spos }
        NLs()
        val delegate = expressionSure(ExpressionState.DISALLOW_LAMBDA)
        return ExplicitDelegation(type, delegate)
    }

    //typeParameters
    //    : LANGLE NL* typeParameter (NL* COMMA NL* typeParameter)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeParametersOpt(): List<TypeParameter>? = if (matchesNLs("<")) typeParameters() else null
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
        NLs()
        val type = if (expectOpt(":")) { NLs(); type() } else null
        return TypeParameter(id, type)
    }

    //typeConstraints
    //    : WHERE NL* typeConstraint (NL* COMMA NL* typeConstraint)*
    //    ;
    fun typeConstraints(): List<TypeConstraint>? {
        debug("TODO: typeConstraints")
        if (!expectOpt("where")) return null
        NLs()
        return parseListNew({ expectOpt(",") }, end = { eof || matches("{") }, oneOrMore = true) {
        //return parseListNew({ expectOpt(",") }, oneOrMore = true) {
            typeConstraint()
        }
        //return parseList(oneOrMore = true) { typeConstraint() }
    }

    //typeConstraint
    //    : annotation* simpleIdentifier NL* COLON NL* type
    //    ;
    fun typeConstraint(): TypeConstraint? {
        debug("TODO: typeConstraint")
        val spos = pos
        val annotations = annotations()
        val id = simpleIdentifierOpt() ?: return null.also { pos = spos }
        NLs()
        COLON()
        NLs()
        val type = type() ?: error("Expected type")
        return TypeConstraint(id, type, annotations)
    }

// SECTION: classMembers

    //classMemberDeclarations
    //    : (classMemberDeclaration semis?)*
    //    ;
    fun classMemberDeclarations(): List<Decl> {
        return zeroOrMore {
            if (matches("}")) return@zeroOrMore null
            classMemberDeclaration().also {
                //println("DECL: $it")
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
    fun classMemberDeclaration(): Decl? {
        val mods = modifiers()
        return when (peek().str) {
            "constructor" -> secondaryConstructor(mods)
            "companion" -> companionObject(mods)
            "init" -> anonymousInitializer(mods)
            else -> declaration(mods)
        }
    }

    //anonymousInitializer
    //    : INIT NL* block
    //    ;
    fun anonymousInitializer(mods: Modifiers = modifiers()): InitDecl {
        expect("init")
        NLs()
        val block = block() ?: error("Expect block after init")
        return InitDecl(block)
    }

    //companionObject
    //    : modifiers? COMPANION NL* DATA? NL* OBJECT
    //      (NL* simpleIdentifier)?
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun companionObject(modifiers: Modifiers = modifiers()): CompanionObjectDecl? {
        val spos = pos
        debug("TODO: companionObject")
        if (!expectOptNLs("companion")) return null.also { pos = spos }
        val isData = expectOptNLs("data")
        if (!expectOptNLs("object")) return null.also { pos = spos }
        val name = opt {
            NLs()
            simpleIdentifierOpt()
        }
        val delegations = if (expectOptNLs(":")) { NLs(); delegationSpecifiers() } else null
        val body = opt {
            NLs()
            classBody()
        }
        return CompanionObjectDecl(name)
    }

    //functionValueParameters
    //    : LPAREN NL* (functionValueParameter (NL* COMMA NL* functionValueParameter)* (NL* COMMA)?)? NL* RPAREN
    //    ;
    fun functionValueParameters(): List<FuncValueParam>? {
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            val params = arrayListOf<FuncValueParam>()
            while (hasMore) {
                NLs()
                if (matches(")")) break
                if (expectOpt(",")) continue
                params += functionValueParameter() ?: error("Not a valid value parameter $this")
                if (!expectOptNLs(",")) break
            }
            NLs()
            params
        }
        //return parseListWithStartEnd("(", ")", oneOrMore = false) {
        //    functionValueParameter()
        //}
    }

    //functionValueParameter
    //    : parameterModifiers? parameter (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameter(): FuncValueParam? {
        val mods = parameterModifiers(atLeastOne = false)
        val param = parameter() ?: return null
        NLs()
        val assignment = if (expectOpt("=")) {
            NLs()
            expression()
        } else {
            null
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
    fun functionDeclaration(modifiers: Modifiers): FunDecl? {
        debug("TODO: functionDeclaration")
        //var funcNameOpt: String? = null
        val spos = pos
        if (!expectOpt("fun")) return null.also { pos = spos }
        //try {
            NLs()
            val typeParams = typeParametersOpt()
            NLs()
            val receiver = opt {
                //if (matches("(")) return@opt null
                var it = receiverType()
                if (!matchesNLs(".")) {
                    if (it is MultiType && it.types.size >= 2) {
                        it = it.copy(it.types.dropLast(1))
                        while (peek().str != "." && pos > 0) pos--
                    }
                }
                if (!expectOpt(".")) return@opt null
                it
            }
            NLs()
            val funcName = simpleIdentifier()
            //funcNameOpt = funcName
            NLs()
            val params = functionValueParameters() ?: return null.also { pos = spos }
            NLs()
            val retType = if (expectOpt(":")) { NLs(); type() } else null
        NLs()
            val typeConstraints = typeConstraints()
            NLs()
            val body = if (matches("=") || matches("{")) functionBody() else null
            return FunDecl(funcName, params, retType = retType, where = typeConstraints, body = body, modifiers = modifiers)
        //} catch (e: Throwable) {
        //    Throwable("Function was not able to be parsed '$funcNameOpt' :: ${readAbsoluteRange(spos, pos)}", e).printStackTrace()
        //    TODO("Function was not able to be parsed '$funcNameOpt': ${readAbsoluteRange(spos, pos)} :: $e")
        //}
    }

    //functionBody
    //    : block
    //    | ASSIGNMENT NL* expression
    //    ;
    fun functionBody(): Stm? {
        return when (peek().str) {
            "=" -> {
                expect("=")
                NLs()
                ReturnStm(expression())
            }
            "{" -> block()
            else -> null
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
    fun variableDeclaration(): VariableDecl? {
        val spos = pos
        val annotations = annotations(atLeastOne = false)
        NLs()
        val id = simpleIdentifierOpt() ?: return null.also { pos = spos }
        NLs()
        val type = if (expectOpt(":")) { NLs(); type() } else null
        return VariableDecl(id, type, annotations = annotations)
    }

    //multiVariableDeclaration
    //    : LPAREN NL* variableDeclaration (NL* COMMA NL* variableDeclaration)* (NL* COMMA)? NL* RPAREN
    //    ;
    fun multiVariableDeclaration(): MultiVariableDecl? {
        if (!matches("(")) return null
        val vars = expectAndRecover("(", ")", nullIfNotMatching = true) {
            parseListNew(trailingSeparator = true, end = { matches(")") }) {
                variableDeclaration()
            }
        } ?: return null
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
    fun propertyDeclaration(modifiers: Modifiers, stm: Boolean = false): VariableDeclBase? {
        debug("TODO: propertyDeclaration")
        val kind = peek().str
        if (!expectOpt("val") && !expectOpt("var")) return null
        //expectAny("val", "var")
        val typeParams = typeParametersOpt()
        val receiver = opt {
            NLs()
            var res = receiverType()
            if (res is MultiType && res.types.size >= 2) {
                res = res.copy(res.types.dropLast(1))
                while (peek().str != "." && pos > 0) pos--
            }
            if (res is MultiType && res.types.size == 1) {
                res = res.types[0]
            }
            NLs()
            if (expectOpt(".")) res else null
        }
        NLs()
        val decl = OR({ multiVariableDeclaration() }, { variableDeclaration() }, name = "propertyDeclaration.decl")
        opt { typeConstraints() }
        var delegation = false
        NLs()
        val expr = when {
            matches("by") -> {
                delegation = true
                propertyDelegate()
            }
            expectOpt("=") -> {
                NLs()
                val expr = expression()
                expr
            }
            else -> {
                null
            }
        }
        expectOptNLs(";")
        NLs()
        if (!stm) {
            zeroOrMore {
                modifiers()
                when {
                    matches("get") -> getter()
                    matches("set") -> setter()
                    else -> return@zeroOrMore null
                }
                NLs()
                semiOpt()
            }
        } else {
            null
        }
        return decl.let { if (expr != null) it.withAssignment(expr, delegation = delegation, receiver = receiver) else it }
    }

    //propertyDelegate
    //    : BY NL* expression
    //    ;
    fun propertyDelegate(): Expr? {
        if (!expectOpt("by")) return null
        NLs()
        return expression()
    }

    //getter
    //    : modifiers? GET
    //      (NL* LPAREN NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun getter(modifiers: Modifiers = modifiers()): Any? {
        if (!expectOpt("get")) return null
        opt {
            NLs()
            val res = expectAndRecover("(", ")", nullIfNotMatching = true) {
                NLs()
            } ?: return@opt null
            opt {
                if (!expectOptNLs(":")) return@opt null
                NLs()
                type()
            }
            NLs()
            functionBody()
        }
        return Unit
    }

    //setter
    //    : modifiers? SET
    //      (NL* LPAREN NL* functionValueParameterWithOptionalType (NL* COMMA)? NL* RPAREN (NL* COLON NL* type)? NL* functionBody)?
    //    ;
    fun setter(modifiers: Modifiers = modifiers()): Any? {
        if (!expectOpt("set")) return null
        opt {
            NLs()
            val res = expectAndRecover("(", ")", nullIfNotMatching = true) {
                NLs()
                functionValueParameterWithOptionalType().also { expectOptNLs(",") }
            } ?: return@opt null
            opt {
                if (!expectOptNLs(":")) return@opt null
                NLs()
                type()
            }
            NLs()
            functionBody()
        }
        return Unit
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
    fun parametersWithOptionalType(): List<ParameterOptType>? {
        if (!matches("(")) return null
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            NLs()
            val items = parseListNew({ expectOpt(",") }, trailingSeparator = true, end = { matches(")") }) {
                functionValueParameterWithOptionalType()
            }
            items ?: emptyList()
        }
    }

    //functionValueParameterWithOptionalType
    //    : parameterModifiers? parameterWithOptionalType (NL* ASSIGNMENT NL* expression)?
    //    ;
    fun functionValueParameterWithOptionalType(): ParameterOptType {
        val modifiers = parameterModifiers(atLeastOne = false)
        val id = simpleIdentifier().also { NLs() }

        val type = optionalTypeWithColon()
        val expr = opt {
            NLs()
            if (!expectOpt("=")) return@opt null
            NLs()
            expression()
        }
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
    fun parameter(): Parameter? {
        val spos = pos
        val id = simpleIdentifierOpt() ?: return null
        NLs()
        if (!expectOpt(":")) {
            pos = spos
            return null
        }
        NLs()
        val type = type() ?: error("Expected type")
        return Parameter(id, type)
    }

    //objectDeclaration
    //    : modifiers? OBJECT
    //      NL* simpleIdentifier
    //      (NL* COLON NL* delegationSpecifiers)?
    //      (NL* classBody)?
    //    ;
    fun objectDeclaration(modifiers: Modifiers): Decl? {
        debug("TODO: objectDeclaration")
        if (!expectOpt("object")) return null
        NLs()
        val name = simpleIdentifierOpt() ?: return null
        val delegations = if (expectOptNLs(":")) {
            NLs()
            delegationSpecifiers()
        } else {
            null
        }
        val body = opt {
            NLs()
            classBody()
        }
        return ObjectDecl(name)
    }

    //secondaryConstructor
    //    : modifiers? CONSTRUCTOR NL* functionValueParameters (NL* COLON NL* constructorDelegationCall)? NL* block?
    //    ;
    fun secondaryConstructor(modifiers: Modifiers = modifiers()): ConstructorDecl? {
        val spos = pos
        debug("TODO: secondaryConstructor")
        expect("constructor")
        NLs()
        val params = functionValueParameters() ?: return null.also { pos = spos }
        val constructorDelegationCall = if (expectOptNLs(":")) {
            NLs()
            constructorDelegationCall()
        } else {
            null
        }
        NLs()
        val body = opt { block() }
        return ConstructorDecl(params = params, body = body, constructorDelegationCall = constructorDelegationCall)
    }

    //constructorDelegationCall
    //    : (THIS | SUPER) NL* valueArguments
    //    ;
    fun constructorDelegationCall(): ConstructorDelegationCall? {
        val kind = expectAny("this", "super")
        NLs()
        val args = valueArguments() ?: return null
        return ConstructorDelegationCall(kind, args)
    }

    // SECTION: enumClasses
    //enumClassBody
    //    : LCURL NL* enumEntries? (NL* SEMICOLON NL* classMemberDeclarations)? NL* RCURL
    //    ;
    fun enumClassBody(): List<Decl> {
        debug("TODO: enumClassBody")
        expectAndRecover("{", "}", reason = "enumClassBody") {
            NLs()
            enumEntries(oneOrMore = false)
            opt {
                if (expectOptNLs(";")) {
                    NLs()
                    classMemberDeclarations()
                } else {
                    null
                }
            }
            NLs()
        }
        return emptyList()
    }

    //enumEntries
    //    : enumEntry (NL* COMMA NL* enumEntry)* NL* COMMA?
    //    ;
    fun enumEntries(oneOrMore: Boolean = true): List<EnumEntry>? {
        return parseListNew(oneOrMore = oneOrMore, separator = { expectOpt(",") }, end = { matches("}") || matches(";") }) {
            enumEntry()
        }
    }

    //enumEntry
    //    : (modifiers NL*)? simpleIdentifier (NL* valueArguments)? (NL* classBody)?
    //    ;
    fun enumEntry(modifiers: Modifiers = modifiers()): EnumEntry {
        debug("TODO: enumEntry")
        val id = simpleIdentifier()
        NLs()
        val arguments = valueArguments()
        NLs()
        val body = classBody()
        return EnumEntry(id)
    }

    fun optionalTypeWithColon(): TypeNode? {
        return if (expectOpt(":")) { NLs(); type() } else null
    }

    // SECTION: types
    //type
    //    : typeModifiers? (functionType | parenthesizedType | nullableType | typeReference | definitelyNonNullableType)
    //    ;
    fun type(): TypeNode? {
        val spos = pos
        debug("TODO: type")

        val modifiers = typeModifiers(atLeastOne = false)

        return ORNullable(
            { nullableType() },
            { functionType() },
            { parenthesizedType() },
            { definitelyNonNullableType() },
            { typeReference() },
        )
        /*
        // @TODO: Receiver here

        val receiver = opt {
            val ref = typeReference()
            if (!expectOptNLs(".")) return@opt null
            ref
        }

        val type: TypeNode = if (matches("(")) {
            val types = functionTypeParameters()
            if (!expectOptNLs("->")) return null.also { pos = spos }
            val retType = type()
            //FuncTypeNode(retType, types, receiver)
            if (retType != null) {
                FuncTypeNode(retType, types, receiver)
            } else {
                types.firstOrNull()?.type
                    ?: FuncTypeNode(retType, types, receiver)
                    //?: return null.also { pos = spos }
                    //?: error("types=$types -> retType=$retType :: $this")
            }
        } else {
            typeReference() ?: return null.also { pos = spos }
        }
        val isNullable = expectOptNLs("?")
        while (expectOptNLs("?")) Unit
        return if (isNullable) type.nullable() else type
        */
    }

    //typeReference
    //    : userType
    //    | DYNAMIC
    //    ;
    fun typeReference(): TypeNode? {
        if (expectOpt("dynamic")) return DynamicType
        return userType()
    }

    //nullableType
    //    : (typeReference | parenthesizedType) NL* quest+
    //    ;
    fun nullableType(): TypeNode? {
        val type = ORNullable(
            { parenthesizedType() },
            { typeReference() },
        ) ?: return null
        NLs()
        var count = 0
        while (hasMore && expectOpt("?")) count++
        return if (count > 0) type.nullable() else null
    }

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
    fun userType(): TypeNode? {
        //println("TODO: userType")
        val base = simpleUserType() ?: return null
        val more = arrayListOf<TypeNode>()
        while (hasMore) {
            val spos = pos
            if (!expectOptNLs(".")) break
            NLs()
            val stype = simpleUserType()
            if (stype == null) {
                pos = spos
                break
            }
            more += stype
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
    fun simpleUserType(): TypeNode? {
        debug("TODO: simpleUserType")
        val id = simpleIdentifierOpt() ?: return null
        val generic = if (expectOptNLs("<", consume = false)) typeArguments() else null
        val simple = SimpleType(id)
        return if (generic != null) GenericType(simple, generic) else simple
    }

    //typeProjection
    //    : typeProjectionModifiers? type
    //    | MULT
    //    ;
    fun typeProjection(): TypeNode? {
        return ORNullable(
            {
                typeProjectionModifiers(atLeastOne = false)
                type()
            },
            {
                if (expectOpt("*")) SimpleType("*") else null
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
    fun typeProjectionModifier(): Any? {
        return ORNullable(
            { varianceModifier().also { NLs() } },
            { annotation() }
        )
    }

    //functionType
    //    : (receiverType NL* DOT NL*)? functionTypeParameters NL* ARROW NL* type
    //    ;
    fun functionType(): FuncTypeNode? {
        val spos = pos
        val receiver = opt {
            val type = receiverType()
            NLs()
            if (!expectOpt(".")) return@opt null
            NLs()
            type
        }
        val typeParams = functionTypeParameters() ?: return null.also { pos = spos }
        if (!expectOptNLs("->")) return null.also { pos = spos }
        NLs()
        val ret = type() ?: error("Expected type")
        return FuncTypeNode(ret, typeParams, receiver)
    }

    //functionTypeParameters
    //    : LPAREN NL* (parameter | type)? (NL* COMMA NL* (parameter | type))* (NL* COMMA)? NL* RPAREN
    //    ;
    fun functionTypeParameters(): List<NamedTypeNode>? {
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            val first = ORNullable({ parameter()?.let { NamedTypeNode(it) } }, { type()?.let { NamedTypeNode(it) } })
            val rest = zeroOrMore {
                NLs()
                if (!expectOpt(",")) return@zeroOrMore null
                NLs()
                ORNullable({ parameter()?.let { NamedTypeNode(it) } }, { type()?.let { NamedTypeNode(it) } })
            }
            NLs()
            expectOpt(",")
            NLs()
            listOfNotNull(first) + rest
        }
    }

    //parenthesizedType
    //    : LPAREN NL* type NL* RPAREN
    //    ;
    fun parenthesizedType(): TypeNode? {
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            NLs(); type().also { NLs() }
        }
    }

    //receiverType
    //    : typeModifiers? (parenthesizedType | nullableType | typeReference)
    //    ;
    fun receiverType(): TypeNode? {
        val spos = pos
        val modifiers = zeroOrMore { typeModifier() }
        return ORNullable({ parenthesizedType() }, { nullableType() }, { typeReference() })?.withModifiers(modifiers) ?: return null.also { pos = spos }
        //return nullableType().withModifiers(modifiers)
        //val type = type() ?: return null.also { pos = spos }
        //return type.withModifiers(modifiers)
    }

    //parenthesizedUserType
    //    : LPAREN NL* (userType | parenthesizedUserType) NL* RPAREN
    //    ;
    fun parenthesizedUserType(): TypeNode? {
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            NLs()
            ORNullable({ userType() }, { parenthesizedUserType() }).also { NLs() }
        }
    }

    //definitelyNonNullableType
    //    : typeModifiers? (userType | parenthesizedUserType) NL* AMP NL* typeModifiers? (userType | parenthesizedUserType)
    //    ;
    fun definitelyNonNullableType(): DefinitelyNonNullableType? {
        val spos = pos
        val mods1 = typeModifiers(atLeastOne = false)
        val type1 = ORNullable({ userType() }, { parenthesizedUserType() }) ?: return null.also { pos = spos }
        NLs()
        if (!expectOpt("&")) return null.also { pos = spos }
        NLs()
        val mods2 = typeModifiers(atLeastOne = false)
        val type2 = ORNullable({ userType() }, { parenthesizedUserType() }) ?: return null.also { pos = spos }
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
        val modifiers = Modifiers(zeroOrMore {
            NLs()
            if (matches("@")) annotation() else label()
        })
        NLs()
        return when (val ftoken = peek().str) {
            "var", "val" -> DeclStm(propertyDeclaration(modifiers, stm = true) ?: error("var-val"))
            "for", "while", "do" -> loopStatement() ?: error("Couldn't parse loop: '$ftoken'")
            "fun" -> declaration()!!.let { DeclStm(it) }
            else -> {
                if (ftoken == "suspend" && peekSkipping(1).str == "fun") {
                    declaration()!!.let { DeclStm(it) }
                } else {
                    OR(
                        { assignment() },
                        {
                            expression()?.let {
                                ExprStm(it)
                            }
                        },
                        { declaration()?.let { DeclStm(it) } },
                        name = "statement"
                    )
                }
            }
        }.withModifiers(modifiers)
    }

    //label
    //    : simpleIdentifier (AT_NO_WS | AT_POST_WS) NL*
    //    ;
    fun label(): LabelNode? {
        val spos = pos
        val id = simpleIdentifierOpt() ?: return null
        if (!expectOpt("@")) {
            pos = spos
            return null
        }
        NLs()
        return LabelNode(id)
    }

    //controlStructureBody
    //    : block
    //    | statement
    //    ;
    fun controlStructureBody(): Stm {
        return when {
            matches("{") -> block()!!
            else -> statement()
        }
    }

    //block
    //    : LCURL NL* statements NL* RCURL
    //    ;
    fun block(): Stm? {
        if (!matches("{")) return null
        return expectAndRecoverSure("{", "}", reason = "block") {
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
    fun loopStatement(): LoopStm? = when (peek().str) {
        "for" -> forStatement()
        "while" -> whileStatement()
        "do" -> doWhileStatement()
        else -> null
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
            expr = expressionSure()
        }
        NLs()

        val body = if (expectOpt(";")) null else controlStructureBody()
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
        val body: Stm = OR(
            { if (expectOpt(";")) EmptyStm() else null },
            { controlStructureBody() }
        )
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
    fun assignment(): AssignStm? {
        var op = "="
        val lvalue = ORNullable(
            {
                val res = directlyAssignableExpression()
                if (!expectOpt("=")) return@ORNullable null
                op = "="
                res
            },
            { assignableExpression().also { Hidden(); op = assignmentAndOperator() ?: return@ORNullable null } },
            name = "assignment"
        ) ?: return null
        NLs()
        val expr = expressionSure()
        return AssignStm(lvalue, op, expr)
    }

    //semi
    //    : (SEMICOLON | NL) NL*
    //    ;
    fun semi() {
        expectOpt(";")
        NLs()
    }

    fun semiOpt() {
        semi()
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

    private inline fun binop(op: () -> String?, initialNLs: Boolean = true, next: () -> Expr?): Expr? {
        val spos = pos
        val ops = arrayListOf<String>()
        val exprs = arrayListOf<Expr>()
        Hidden()
        val expr = next()
        if (expr == null) {
            pos = spos
            //TODO("expr=null")
            return null
        }
        exprs += expr
        zeroOrMore {
            if (initialNLs) NLs()
            val op = op() ?: return@zeroOrMore null
            NLs()
            val expr = next()
            if (expr == null) {
                pos = spos
                //TODO("expr=null :: $this")
                return null
            }
            ops += op
            exprs += expr
        }
        if (ops.isEmpty()) {
            check(exprs.size == 1)
            return exprs.first()
        }
        return OpSeparatedExprs(ops, exprs).enrich(this, spos)
    }

    fun expressionSure(state: ExpressionState? = null): Expr {
        val spos = pos
        return expression(state) ?: error("Expression expected but found ${run { pos = spos; peek() }} : $this")
    }

    //expression
    //    : disjunction
    //    ;
    fun expression(state: ExpressionState? = null): Expr? = disjunction(state)

    //disjunction
    //    : conjunction (NL* DISJ NL* conjunction)*
    //    ;
    fun disjunction(state: ExpressionState? = null): Expr? = binop({ expectAnyOpt("||") }, initialNLs = true) { conjunction(state) }

    //conjunction
    //    : equality (NL* CONJ NL* equality)*
    //    ;
    fun conjunction(state: ExpressionState? = null): Expr? = binop({ expectAnyOpt("&&") }, initialNLs = true) { equality(state) }

    //equality
    //    : comparison (equalityOperator NL* comparison)*
    //    ;
    fun equality(state: ExpressionState? = null): Expr? = binop({ expectAnyOpt(EQUALITY_OPERATOR) }, initialNLs = false) { comparison(state) }

    //comparison
    //    : genericCallLikeComparison (comparisonOperator NL* genericCallLikeComparison)*
    //    ;
    fun comparison(state: ExpressionState? = null): Expr? = binop({ expectAnyOpt(COMPARISON_OPERATOR) }, initialNLs = false) { genericCallLikeComparison(state) }

    //genericCallLikeComparison
    //    : infixOperation callSuffix*
    //    ;
    fun genericCallLikeComparison(state: ExpressionState? = null): Expr? {
        var res: Expr = infixOperation(state) ?: return null
        zeroOrMore {
            callSuffix(res, state)?.also { res = it }
        }
        //println("TODO: genericCallLikeComparison")
        return res
    }

    //infixOperation
    //    : elvisExpression (inOperator NL* elvisExpression | isOperator NL* type)*
    //    ;
    fun infixOperation(state: ExpressionState? = null): Expr? {
        var res: Expr = elvisExpression(state) ?: return null
        loop@while (true) {
            val op = expectAnyOpt(IN_IS_OPERATOR) ?: break
            NLs()
            res = when (op) {
                "!in", "in" -> RangeTestExpr(res, op, elvisExpression() ?: error("Expected expression"))
                "!is", "is" -> TypeTestExpr(res, op, type() ?: error("Expected type"))
                else -> return null
            }
        }
        return res
    }

    //elvisExpression
    //    : infixFunctionCall (NL* elvis NL* infixFunctionCall)*
    //    ;
    fun elvisExpression(state: ExpressionState? = null): Expr? = binop({ expectAnyOpt("?:") }, initialNLs = true) { infixFunctionCall(state) }

    //elvis
    //    : QUEST_NO_WS COLON
    //    ;
    //fun elvis() = expect("?:")

    //infixFunctionCall
    //    : rangeExpression (simpleIdentifier NL* rangeExpression)*
    //    ;
    fun infixFunctionCall(state: ExpressionState? = null): Expr? {
        return binop({ simpleIdentifierOpt() }, initialNLs = false) { rangeExpression(state) }
    }

    //rangeExpression
    //    : additiveExpression ((RANGE | RANGE_UNTIL) NL* additiveExpression)*
    //    ;
    fun rangeExpression(state: ExpressionState? = null): Expr? {
        return binop({ expectAnyOpt(RANGE_OPS) }, initialNLs = false) { additiveExpression(state) }
    }

    //additiveExpression
    //    : multiplicativeExpression (additiveOperator NL* multiplicativeExpression)*
    //    ;
    fun additiveExpression(state: ExpressionState? = null): Expr? {
        return binop({ additiveOperator() }, initialNLs = false) { multiplicativeExpression(state) }
    }

    //multiplicativeExpression
    //    : asExpression (multiplicativeOperator NL* asExpression)*
    //    ;
    fun multiplicativeExpression(state: ExpressionState? = null): Expr? {
        return binop({ multiplicativeOperator() }, initialNLs = false) { asExpression(state) }
    }

    //asExpression
    //    : prefixUnaryExpression (NL* asOperator NL* type)*
    //    ;
    fun asExpression(state: ExpressionState? = null): Expr? {
        val spos = pos
        var res: Expr = prefixUnaryExpression(state) ?: return null.also { pos = spos }
        val asTypes = zeroOrMore {
            NLs()
            val kind = asOperator() ?: return@zeroOrMore null
            NLs()
            val type = type() ?: error("Expected type")
            kind to type
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
    fun prefixUnaryExpression(state: ExpressionState? = null): Expr? {
        val spos = pos
        val prefixes = zeroOrMore { unaryPrefix(state) }
        debug("TODO: prefixUnaryExpression : $prefixes")
        var res: Expr = postfixUnaryExpression(state) ?: return null.also { pos = spos }
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
    fun unaryPrefix(state: ExpressionState? = null): Disjunction3<UnaryPreOp, AnnotationNodes, LabelNode>? {
        return ORDis(
            { prefixUnaryOperator().also { NLs() } },
            { annotation() },
            { label() },
        )
    }

    //postfixUnaryExpression
    //    : primaryExpression postfixUnarySuffix*
    //    ;
    fun postfixUnaryExpression(state: ExpressionState? = null): Expr? {
        debug("TODO: postfixUnaryExpression")
        var res: Expr = primaryExpression(state) ?: return null
        zeroOrMore {
            postfixUnarySuffix(res, state)?.let { res = it }
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
    fun postfixUnarySuffix(expr: Expr, state: ExpressionState? = null): Expr? {
        expectAnyOpt(UnaryPostOp.BY_ID)?.let {
            return UnaryPostOpExpr(expr, it)
        }
        // valueArguments: LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
        // annotatedLambda: annotation* label? NL* lambdaLiteral
        NLs()
        if (matches("<") || matches("(") || matches("{")) {
            debug("TODO: postfixUnarySuffix.callSuffix")
            return callSuffix(expr, state)
        }
        if (matches("[")) {
            val params = expectAndRecoverSure("[", "]") {
                parseList(separator = { expectOpt(",") }, doBreak = { matches("]") }) {
                    expression() ?: error("Expression expected")
                }
            }
            return IndexedExpr(expr, params)
        }
        val op = expectAnyOpt(".", "::", consume = false)
        if (op != null || expectOpt("?")) {
            return navigationSuffix(expr)
        }
        return null
    }

    //directlyAssignableExpression
    //    : postfixUnaryExpression assignableSuffix
    //    | simpleIdentifier
    //    | parenthesizedDirectlyAssignableExpression
    //    ;
    fun directlyAssignableExpression(): AssignableExpr? {
        //println("TODO: directlyAssignableExpression")
        return if (matches("(")) {
            parenthesizedDirectlyAssignableExpression()
        } else {
            val expr = postfixUnaryExpression() ?: return null
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
    fun parenthesizedDirectlyAssignableExpression(): AssignableExpr? {
        if (!matches("(")) return null
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            NLs()
            directlyAssignableExpression().also { NLs() }
        }
    }

    //assignableExpression
    //    : prefixUnaryExpression
    //    | parenthesizedAssignableExpression
    //    ;
    fun assignableExpression(): AssignableExpr? {
        return ORNullable(
            { prefixUnaryExpression() as? AssignableExpr? },
            { parenthesizedAssignableExpression() },
            name = "assignableExpression"
        )
    }

    //parenthesizedAssignableExpression
    //    : LPAREN NL* assignableExpression NL* RPAREN
    //    ;
    fun parenthesizedAssignableExpression(): AssignableExpr? {
        if (!matches("(")) return null
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            NLs()
            assignableExpression().also {
                NLs()
            }
        }
    }

    //assignableSuffix
    //    : typeArguments
    //    | indexingSuffix
    //    | navigationSuffix
    //    ;
    fun assignableSuffix(expr: Expr): AssignableExpr? {
        return when (peek().str) {
            "<" -> TypeArgumentsAssignableSuffixExpr(expr, typeArguments()!!)
            "[" -> indexingSuffix(expr)
            "::", "?.", "." -> navigationSuffix(expr)
            else -> return null
        }
    }

    //indexingSuffix
    //    : LSQUARE NL* expression (NL* COMMA NL* expression)* (NL* COMMA)? NL* RSQUARE
    //    ;
    fun indexingSuffix(expr: Expr): AssignableExpr =
        IndexedExpr(expr, parseListWithStartEnd("[", "]", oneOrMore = true, separator = { expectOpt(",") }) {
            expression() ?: error("Expression expected")
        })

    //navigationSuffix
    //    : memberAccessOperator NL* (simpleIdentifier | parenthesizedExpression | CLASS)
    //    ;
    fun navigationSuffix(expr: Expr): AssignableExpr {
        val op = memberAccessOperator() ?: error("Unexpected memberAccessOperator")
        NLs()
        if (matches("class")) {
            return NavigationExpr(op, expr, "class")
        }
        if (matches("(")) {
            return NavigationExpr(op, expr, parenthesizedExpression()!!)
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
    fun callSuffix(expr: Expr, state: ExpressionState? = null): Expr? {
        val spos = pos
        val typeArgs = opt { typeArguments() }
        //val args = opt { valueArguments() }
        val args = if (matches("(")) {
            valueArguments()
        } else {
            null
        }

        val lambdaArg = if (state?.allowLambda != false) {
            if (args == null) {
                annotatedLambda()
            } else {
                opt { annotatedLambda() }
            }
        } else {
            null
        }

        if (args == null && lambdaArg == null) {
            pos = spos
            return null
        }

        return CallExpr(expr, args ?: emptyList(), lambdaArg, typeArgs)
    }

    //annotatedLambda
    //    : annotation* label? NL* lambdaLiteral
    //    ;
    fun annotatedLambda(): Expr? {
        zeroOrMore { annotation() }
        opt { label() }
        NLs()
        return lambdaLiteral()
    }

    //typeArguments
    //    : LANGLE NL* typeProjection (NL* COMMA NL* typeProjection)* (NL* COMMA)? NL* RANGLE
    //    ;
    fun typeArguments(): List<TypeNode>? {
        //println("${this.tokens}")
        if (!matches("<")) return null
        return expectAndRecover("<", ">", nullIfNotMatching = true) {
            NLs()
            parseListNew(trailingSeparator = true, end = { matches(">") }) {
                typeProjection()
            }
        }
    }

    //valueArguments
    //    : LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* (NL* COMMA)? NL*)? RPAREN
    //    ;
    fun valueArguments(): List<Expr>? {
        debug("valueArguments: $this")
        if (!matches("(")) return null
        return expectAndRecover("(", ")", nullIfNotMatching = true) {
            parseList(oneOrMore = false, separator = { expectOpt(",") }, doBreak = { matches(")") }) {
                valueArgument()
            }.filterNotNull()
        }.also {
            debug("/valueArguments: $this")
        }
    }

    //valueArgument
    //    : annotation? NL* (simpleIdentifier NL* ASSIGNMENT NL*)? MULT? NL* expression
    //    ;
    fun valueArgument(): Expr? {
        val spos = pos
        val annotations = annotation()
        NLs()
        val namedArgument = opt {
            val id = simpleIdentifierOpt() ?: return@opt null
            NLs()
            if (!expectOpt("=")) return@opt null
            NLs()
            id
        }
        val spread = expectOptNLs("*")
        NLs()
        return expression() ?: return null.also { pos = spos }
            //?: error("Expression expected [valueArgument] in $this")
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
    fun primaryExpression(state: ExpressionState? = null): Expr? {
        //literalConstantOpt()?.let { return it }

        debug("TODO: primaryExpression")

        //if (peek() is StringInterpolationToken) return stringLiteral() ?: error("Not a string literal")
        //if (matches("[")) return collectionLiteral() ?: error("Not a collection literal")
        //if (matches("this")) return thisExpression()
        //if (matches("if")) return ifExpression()
        //if (matches("when")) return whenExpression()
        //if (matches("try")) return tryExpression()
        //if (matches("super")) return superExpression()
        //if (matches("{")) return lambdaLiteral() ?: error("Not a lambda literal")
        ////if (matches("suspend")) return functionLiteral()
        //if (matches("fun")) return anonymousFunction() ?: error("Not an anonymous function")
        //if (matches("object")) return objectLiteral()
        //if (matches("throw") || matches("return") || matches("continue") || matches("break")) return jumpExpression()
        return when (peek().str) {
            "(" -> parenthesizedExpression()
            "this" -> thisExpression() ?: error("Not a this")
            "super" -> superExpression() ?: error("Not a super")
            "if" -> ifExpression() ?: error("Not a if")
            "when" -> whenExpression() ?: error("Not a when")
            "try" -> tryExpression() ?: error("Not a try")
            "throw", "return", "continue", "break" -> jumpExpression() ?: error("Not a jump")
            "[" -> collectionLiteral() ?: error("Not a collection literal")
            else -> {
                ORNullable(
                    { literalConstantOpt() },
                    { stringLiteral() },
                    { callableReference() },
                    { functionLiteral() },
                    { objectLiteral() },
                    { simpleIdentifierOpt()?.let { IdentifierExpr(it) } },
                )
            }
        }
    }

    //parenthesizedExpression
    //    : LPAREN NL* expression NL* RPAREN
    //    ;
    fun parenthesizedExpression(): Expr? {
        if (!matches("(")) return null
        return expectAndRecover("(", ")", nullIfNotMatching = true) { NLs(); expression().also { NLs() } }
    }

    //collectionLiteral
    //    : LSQUARE NL* (expression (NL* COMMA NL* expression)* (NL* COMMA)? NL*)? RSQUARE
    //    ;
    fun collectionLiteral(): CollectionLiteralExpr? {
        if (!matches("[")) return null
        return CollectionLiteralExpr(parseListWithStartEnd("[", "]", separator = { expectOpt("," )}) {
            expression() ?: error("Expression expected")
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
        if (peek() is CharLiteralToken) {
            return CharacterLiteral()
        }
        expectAnyOpt("false", "true")?.let { return BoolLiteralExpr(it == "true") }
        if (expectOpt("null")) return NullLiteralExpr()
        val token = expectOpt<NumberToken>() ?: return null
        if (!token.isFloat) {
            return IntLiteralExpr(token.value.toLong(), token.isLong, token.isUnsigned)
        }
        TODO("literalConstantOpt: token=$token : ${token.isFloat}")
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
    fun stringLiteral(): Expr? = enrichOpt {
        val str = expectOpt<StringInterpolationToken>() ?: return@enrichOpt null
        val chunks = str.tokens.map { token ->
            when (token) {
                is EscapeStringPartToken -> InterpolatedStringExpr.StringChunk("${token.c}")
                is ExpressionStringPartToken -> {
                    InterpolatedStringExpr.ExpressionChunk(
                        createParser(token.expr).expression()
                            ?: error("Expression expected in $this")
                    )
                }
                is LiteralStringPartToken -> InterpolatedStringExpr.StringChunk(token.str)
            }
        }
        return@enrichOpt when {
            chunks.isEmpty() -> StringLiteralExpr("")
            chunks.size == 1 && chunks.first() is InterpolatedStringExpr.StringChunk -> StringLiteralExpr((chunks.first() as InterpolatedStringExpr.StringChunk).string)
            else -> InterpolatedStringExpr(chunks)
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
    fun lambdaLiteral(): Expr? {
        if (!matches("{")) return null
        debug("TODO: lambdaLiteral")
        val (params, stms) = expectAndRecoverSure("{", "}", reason = "lambdaLiteral") {
            val params = opt {
                NLs()
                val params = lambdaParameters(oneOrMore = false)
                if (!expectOptNLs("->")) return@opt null
                NLs()
                params
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
    fun lambdaParameters(oneOrMore: Boolean = true): List<VariableDeclBase>? {
        return parseListNew(oneOrMore = oneOrMore, trailingSeparator = true) { lambdaParameter() }
    }

    //lambdaParameter
    //    : variableDeclaration
    //    | multiVariableDeclaration (NL* COLON NL* type)?
    //    ;
    fun lambdaParameter(): VariableDeclBase? {
        if (matches("(")) {
            val mvd = multiVariableDeclaration()
            NLs()
            val type = if (expectOpt(":")) { NLs(); type() } else null
            return mvd?.copy(type = type)
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
    fun anonymousFunction(): AnonymousFunctionExpr? {
        val spos = pos
        val isSuspend = expectOpt("suspend")
        if (!expectOptNLs("fun")) return null.also { pos = spos}
        val receiver = opt {
            NLs()
            val type = type()
            if (!expectOptNLs(".")) return@opt null
            type
        }
        NLs()
        val paramsWithOptType = parametersWithOptionalType()
            //?: return null.also { pos = spos }
            ?: error("Expected parameters $this")
        val retType = if (expectOptNLs(":")) { NLs(); type() } else null
        val typeConstraints = opt { NLs(); typeConstraints() }
        val body = opt { NLs(); functionBody() }
        return AnonymousFunctionExpr(FunDecl("", paramsWithOptType.map { FuncValueParam(it.id, it.type ?: UnknownType) }, retType, receiver = receiver, where = typeConstraints, body = body, modifiers = if (isSuspend) Modifiers(FunctionModifier.SUSPEND) else Modifiers.EMPTY))
    }

    //functionLiteral
    //    : lambdaLiteral
    //    | anonymousFunction
    //    ;
    fun functionLiteral(): Expr? {
        if (matches("{")) return lambdaLiteral()
        return anonymousFunction()
    }

    //objectLiteral
    //    : DATA? NL* OBJECT (NL* COLON NL* delegationSpecifiers NL*)? (NL* classBody)?
    //    ;
    fun objectLiteral(): ObjectLiteralExpr? {
        val spos = pos
        val isData = expectOpt("data")
        if (!expectOptNLs("object")) {
            pos = spos
            return null
        }
        val delegationSpecifiers: List<SubTypeInfo>? = when {
            expectOptNLs(":") -> delegationSpecifiers().also { NLs() }
            else -> null
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
    fun thisExpression(): ThisExpr? = enrichOpt {
        if (!expectOpt("this")) return@enrichOpt null
        val id = if (expectOpt("@")) identifier() else null
        ThisExpr(id)
    }

    //superExpression
    //    : SUPER (LANGLE NL* type NL* RANGLE)? (AT_NO_WS simpleIdentifier)?
    //    | SUPER_AT
    //    ;
    fun superExpression(): SuperExpr? {
        if (!expectOpt("super")) return null
        val type = when {
            matches("<") -> expectAndRecover("<", ">") { NLs(); type().also { NLs() } }
            else -> null
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
    fun ifExpression(): Expr? {
        if (!expectOpt("if")) return null
        NLs()
        val cond = expectAndRecoverSure("(", ")") {
            NLs()
            expression().also { NLs() } ?: error("Expression expected")
        }
        val trueBody = if (expectOptNLs(";")) {
            EmptyStm()
        } else {
            controlStructureBody()
        }
        expectOptNLs(";")
        val falseBody = if (expectOptNLs("else")) {
            opt {
                NLs()
                controlStructureBody()
            }.also {
                expectOptNLs(";")
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
                if (!expectOpt("val")) return@opt null
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
    fun whenExpression(): WhenExpr? {
        if (!expectOpt("when")) return null
        NLs()
        val subject = when {
            matches("(") -> whenSubject()
            else -> null
        }
        NLs()
        val entries = expectAndRecover("{", "}", reason = "when") {
            zeroOrMore {
                NLs()
                if (matches("}")) null else whenEntry()
            }.also { NLs() }
        } ?: emptyList()
        return WhenExpr(subject, entries)
    }

    //whenEntry
    //    : whenCondition (NL* COMMA NL* whenCondition)* (NL* COMMA)? NL* ARROW NL* controlStructureBody semi?
    //    | ELSE NL* ARROW NL* controlStructureBody semi?
    //    ;
    fun whenEntry(): WhenExpr.Entry? {
        val spos = pos
        val conditions = when {
            expectOpt("else") -> null
            else -> parseList(separator = { expectOpt(",") }, doBreak = { matches("->") }) {
                whenCondition()
            }
        }
        if (!expectOptNLs("->")) {
            error("When entry expected arrow")
            pos = spos
            return null
        }
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
    fun whenCondition(): WhenExpr.ConditionBase {
        return when {
            matches("in") || matches("!in") -> {
                WhenExpr.ConditionIn(op = inIsOperator().also { NLs() }, expr = expression() ?: error("Expression expected in/ $this"))
            }
            matches("is") || matches("!is") -> {
                WhenExpr.ConditionIs(op = inIsOperator().also { NLs() }, type = type() ?: error("Expression expected is $this"))
            }
            else -> WhenExpr.Condition(expr = expression() ?: error("Expression expected"))
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
    fun tryExpression(): Expr? {
        if (!expectOpt("try")) return null
        NLs()
        val body = block() ?: error("Expect block in try")
        val catches = arrayListOf<TryCatchExpr.Catch>()
        var finally: Stm? = null
        loop@while (hasMore) {
            val spos = pos
            NLs()
            val peek = peek().str
            when (peek) {
                "catch" -> catches += catchBlock()
                "finally" -> {
                    finally = finallyBlock()
                    break@loop
                }
                else -> {
                    pos = spos
                    break@loop
                }
            }
        }
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
            val type = type() ?: error("Expected type")
            NLs()
            if (expectOpt(",")) Unit
            Triple(annotations, localName, type)
        }
        NLs()
        val block = block() ?: error("Expect block after catch")
        return TryCatchExpr.Catch(localName, type, block)
    }

    //finallyBlock
    //    : FINALLY NL* block
    //    ;
    fun finallyBlock(): Stm? {
        if (!expectOpt("finally")) return null
        NLs()
        return block() ?: error("Expect block after finally")
    }

    //jumpExpression
    //    : THROW NL* expression
    //    | (RETURN | RETURN_AT) expression?
    //    | CONTINUE
    //    | CONTINUE_AT
    //    | BREAK
    //    | BREAK_AT
    //    ;
    fun jumpExpression(): Expr? {
        //println("TODO: jumpExpression")
        return when {
            expectOpt("throw") -> {
                NLs()
                ThrowExpr(expressionSure())
            }
            expectOpt("return") -> {
                val label = if (expectOpt("@")) Identifier() else null
                val expr = expression()
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
            else -> null
        }
    }

    //callableReference
    //    : receiverType? COLONCOLON NL* (simpleIdentifier | CLASS)
    //    ;
    fun callableReference(): Expr? {
        val spos = pos
        val type = opt { receiverType() }
        if (!expectOpt("::")) {
            pos = spos
            return null
        }
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
    fun assignmentAndOperator(): String? = expectAnyOpt(ASSIGNMENT_OPERATOR)

    //equalityOperator
    //    : EXCL_EQ
    //    | EXCL_EQEQ
    //    | EQEQ
    //    | EQEQEQ
    //    ;
    fun equalityOperator(): String? = expectAnyOpt(EQUALITY_OPERATOR)

    //comparisonOperator
    //    : LANGLE
    //    | RANGLE
    //    | LE
    //    | GE
    //    ;
    fun comparisonOperator(): String? = expectAnyOpt(COMPARISON_OPERATOR)

    fun inIsOperator(): String? = expectAnyOpt(IN_IS_OPERATOR)

    //inOperator
    //    : IN
    //    | NOT_IN
    //    ;
    fun inOperator(): String? = expectAnyOpt(IN_OPERATOR)

    //isOperator
    //    : IS
    //    | NOT_IS
    //    ;
    fun isOperator(): String? = expectAnyOpt(IS_OPERATOR)

    //additiveOperator
    //    : ADD
    //    | SUB
    //    ;
    fun additiveOperator(): String? = expectAnyOpt(ADDITIVE_OPERATOR)

    //multiplicativeOperator
    //    : MULT
    //    | DIV
    //    | MOD
    //    ;
    fun multiplicativeOperator(): String? = expectAnyOpt(MULTIPLICATIVE_OPERATOR)

    //asOperator
    //    : AS
    //    | AS_SAFE
    //    ;
    fun asOperator(): String? = expectAnyOpt(AS_OPERATOR)

    //prefixUnaryOperator
    //    : INCR
    //    | DECR
    //    | SUB
    //    | ADD
    //    | excl
    //    ;
    fun prefixUnaryOperator(): UnaryPreOp? = expectAnyOpt(UnaryPreOp.BY_ID)

    //postfixUnaryOperator
    //    : INCR
    //    | DECR
    //    | EXCL_NO_WS excl
    //    ;
    fun postfixUnaryOperator(): UnaryPostOp? = expectAnyOpt(UnaryPostOp.BY_ID)

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
    fun memberAccessOperator(): String? {
        val spos = pos
        return when {
            expectOpt("::") -> "::"
            else -> {
                NLs()
                when {
                    expectOpt("?") -> {
                        if (!expectOpt(".")) return null.also { pos = spos }
                        "?."
                    }
                    expectOpt(".") -> "."
                    else -> null
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
    fun modifiers(): Modifiers {
        val mods = arrayListOf<Any>()
        loop@while (hasMore) {
            mods += if (matchesNLs("@")) {
                annotation()
            } else {
                modifier()
            } ?: break@loop
        }
        return Modifiers(mods)
    }

    //parameterModifiers
    //    : (annotation | parameterModifier)+
    //    ;
    fun parameterModifiers(atLeastOne: Boolean = true): Modifiers {
        return Modifiers(multiple(atLeastOne = atLeastOne) { ORNullable({ annotation() }, { parameterModifier() }) })
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
        return modifierOrNull()
    }

    fun modifierOrNull(): Modifier? {
        return expectAnyOptNLs(ALL_MODIFIERS).also { NLs() }
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
    fun typeModifier(): Any? {
        return when {
            expectOptNLs("suspend") -> FunctionModifier.SUSPEND
            expectOptNLs("@") -> {
                pos--
                annotation()
            }
            else -> null
        }
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
    fun varianceModifier(): VarianceModifier? = expectAnyOpt(VarianceModifier.BY_ID)

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
    fun functionModifier(): FunctionModifier? = expectAnyOpt(FunctionModifier.BY_ID)

    //propertyModifier
    //    : CONST
    //    ;
    fun propertyModifier(): PropertyModifier? = expectAnyOpt(PropertyModifier.BY_ID)

    //inheritanceModifier
    //    : ABSTRACT
    //    | FINAL
    //    | OPEN
    //    ;
    fun inheritanceModifier(): InheritanceModifier? = expectAnyOpt(InheritanceModifier.BY_ID)

    //parameterModifier
    //    : VARARG
    //    | NOINLINE
    //    | CROSSINLINE
    //    ;
    fun parameterModifier(): ParameterModifier? = expectAnyOpt(ParameterModifier.BY_ID)

    //reificationModifier
    //    : REIFIED
    //    ;
    fun reificationModifier(): ReificationModifier? = expectAnyOpt(ReificationModifier.BY_ID)

    //platformModifier
    //    : EXPECT
    //    | ACTUAL
    //    ;
    fun platformModifier(): PlatformModifier? = expectAnyOpt(PlatformModifier.BY_ID)

    // SECTION: annotations
    //annotation
    //    : (singleAnnotation | multiAnnotation) NL*
    //    ;
    fun annotation(): AnnotationNodes? {
        val spos = pos
        if (!expectOpt("@")) return null
        val useSite = expectAnyOpt(ANNOTATION_SITES)
        if (useSite != null) {
            NLs()
            expect(":")
        }
        val nodes = if (matches("[")) {
            expectAndRecoverSure("[", "]") {
                AnnotationNodes(oneOrMore { unescapedAnnotation() })
            }
        } else {
            unescapedAnnotation()?.let { AnnotationNodes(listOf(it)) }
        } ?: return null.also { pos = spos }
        NLs()
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
    fun unescapedAnnotation(): AnnotationNode? {
        val type = userType() ?: return null
        NLs()
        val args = if (matches("(")) valueArguments() else null
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
        val spos = pos
        return simpleIdentifierOpt() ?: error("Not a simple identifier ${run { pos = spos; peek() }} in $this")
    }

    fun simpleIdentifierOpt(): String? {
        return expectAnyOpt(SIMPLE_IDENTIFIER) ?: IdentifierOpt()
    }

    //identifier
    //    : simpleIdentifier (NL* DOT simpleIdentifier)*
    //    ;
    fun identifier(): Identifier {
        val ids = arrayListOf<String>()

        ids += simpleIdentifier()
        while (hasMore) {
            val spos = pos
            NLs()
            if (expectOpt(".")) {
                val id = simpleIdentifierOpt()
                if (id != null) {
                    ids += id
                } else {
                    pos = spos
                    break
                }
            } else {
                break
            }
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
    fun ShebangLineOpt(): String? {
        return expectOpt<ShebangToken>()?.str
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
    private fun NL(): Unit {
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
    fun DOT(): String = expectRet(".")

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
        val spos = pos
        return IdentifierOpt() ?: run { pos = spos; error("not an identifier but ${peek()} in $this") }
    }
    fun IdentifierOpt(): String? {
        val idToken = expectOpt<IDToken>()
            ?: return null
        if (idToken.str in HARD_KEYWORDS) {

            return null
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
        if (!eof) throw EofNotFoundException("Not EOF found but ${peek()} at $this")
    }

    companion object {
        val HARD_KEYWORDS = setOf(
            "return", "for", "while", "do", "else", "when", "if", "super", "is", "in", "class", "interface", "fun", "var", "val", "try"
        )

        val SIMPLE_IDENTIFIER = setOf(
            "abstract", "annotation", "by", "catch", "companion", "constructor", "crossinline", "data", "dynamic",
            "enum", "external", "final", "finally", "get", "import", "infix", "init", "inline", "inner", "internal",
            "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public",
            "reified", "sealed", "tailrec", "set", "vararg", "where", "field", "property", "receiver", "param",
            "setparam", "delegate", "file", "expect", "actual", "const", "suspend", "value"
        )

        val RANGE_OPS = setOf("..<", "..")
        val ASSIGNMENT_OPERATOR = setOf("+=", "-=", "*=", "/=", "%=")
        val EQUALITY_OPERATOR = setOf("!==", "===", "!=", "==")
        val COMPARISON_OPERATOR = setOf(">=", "<=", "<", ">")
        val IS_OPERATOR = setOf("!is", "is")
        val IN_OPERATOR = setOf("!in", "in")
        val IN_IS_OPERATOR = IS_OPERATOR + IN_OPERATOR
        val ADDITIVE_OPERATOR = setOf("+", "-")
        val MULTIPLICATIVE_OPERATOR = setOf("*", "/", "%")
        val AS_OPERATOR = setOf("as?", "as")
        val ANNOTATION_SITES = setOf("field", "property", "get", "set", "receiver", "param", "setparam", "delegate", "file")

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

    fun expectOptNLs(str: String, consume: Boolean = true): Boolean {
        val spos = pos
        NLs()
        return matches(str, consume = consume).also {
            if (!it) pos = spos
        }
    }

    fun <T> expectAnyOptNLs(strs: Map<String, T>): T? {
        val spos = pos
        NLs()
        val str = peek().str
        val res = strs[str] ?: return null.also { pos = spos }
        return res?.also { skip() }
    }


    fun matchesNLs(str: String): Boolean = expectOptNLs(str, consume = false)
}

class EofNotFoundException(message: String) : IllegalStateException(message)