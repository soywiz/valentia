package valentia.gen

import valentia.ast.*
import valentia.sema.*
import valentia.transform.TransformUnsupportedNodes
import valentia.util.Indenter
import valentia.util.indent

open class JSCodegen {
    var mainFunction: FunDecl? = null

    var indenter = Indenter()

    open fun supportedNode(node: Node): Boolean {
        return true
    }

    open fun generateProgram(program: Program) {
        val program = if (!program.semaResolved) {
            TODO("Not performed semantic analysis")
            SemaResolver.resolve(program)
        } else {
            program
        }

        indenter.line("#!/usr/bin/env -S deno run -A --unstable")
        for (module in program.modulesById.values) {
            generateModule(module)
        }
        if (mainFunction != null) {
            indenter.line("${mainFunction!!.jsName}([])")
        }
    }
    open fun generateModule(module: Module) {
        for (file in module.packagesById.values) {
            generatePackage(file)
        }
    }

    open fun generatePackage(pack: Package) {
        indenter.line("// package: ${pack.identifier}")
        for (file in pack.files) {
            generateFile(file)
        }
    }

    open fun generateFile(file: FileNode) {
        indenter.line("// topLevelDecls: ${file.topLevelDecls.size}")
        generateDecls(file.topLevelDecls, null)
    }

    open fun generateDecls(decls: List<Decl?>?, parent: Decl?, ctx: GenerateContext? = null) {
        for (decl in (decls ?: emptyList())) {
            generateDecl(decl, parent, ctx)
        }
    }

    data class GenerateContext(
        val settingFieldConstructor: Boolean = false,
    )

    open fun generateDecl(decl: Decl?, parent: Decl?, ctx: GenerateContext?) {
        //indenter.line("// decl: ${decl}")
        when (decl) {
            null -> Unit
            is ClassLikeDecl -> generateClass(decl)
            is FunDecl -> generateFunction(decl, parent)
            is VariableDecl -> {
                if (FunctionModifier.EXTERNAL !in decl.modifiers) {
                    val settingFieldConstructor = ctx?.settingFieldConstructor == true
                    val letStr = when {
                        settingFieldConstructor -> "this."
                        parent !is ClassLikeDecl -> "let "
                        else -> ""
                    }
                    //println("LET: $letStr : ${decl.declName}")
                    if (decl.getter != null) {
                        if (decl.receiver != null) {
                            generateFunction(decl.getter, parent)
                        } else {
                            indenter.line("get ${letStr}${decl.jsName}()") {
                                generateStm(decl.getter.body?.stms, parent)
                            }
                        }
                    } else {
                        val expr = decl.expr
                        var assign = ""
                        if (expr != null) {
                            val (pre, expr) = transformer.ensure(decl.expr, transformContext)
                            pre?.let { generateStm(pre, parent) }
                            assign = " = ${generateExpr(expr)}"
                        }
                        indenter.line("${letStr}${decl.jsName}$assign;")
                    }
                }
            }
            is BaseConstructorDecl -> {
                val clazzDecl = parent as ClassLikeDecl

                if (decl is PrimaryConstructorDecl) {
                    for (p in decl.classParams) {
                        if (p.kind != null) {
                            // @TODO: Detect type to set to null, or to number, etc.
                            indenter.line("${p.id} = null;")
                        }
                    }
                }

                val params = decl.params.toJsString()
                indenter.line("${decl.jsName}($params)") {
                    if (decl is PrimaryConstructorDecl) {
                        for (p in decl.classParams) {
                            if (p.kind != null) {
                                indenter.line("this.${p.id} = ${p.id};")
                            }
                        }
                        for (field in clazzDecl.fields) {
                            generateDecl(field, decl, GenerateContext(settingFieldConstructor = true))
                            //indenter.line("this.${field.jsName} = null;")
                        }
                        val parentClass = decl.parentDecl as ClassLikeDecl
                        for (init in parentClass.bodyAll.filterIsInstance<InitDecl>()) {
                            generateStmCompact(init.stm, decl)
                        }
                    }
                    val delegatedCall = decl.constructorDelegationCall
                    if (delegatedCall != null) {
                        when (delegatedCall.kind) {
                            DelegationCallKind.THIS -> {
                                val callType = delegatedCall.getNodeType()
                                val clazz = parent as ClassLikeDecl
                                val parentConstructor = clazz?.constructors?.firstOrNull {
                                    val constructorType = it.getNodeType()
                                    TypeMatching.canAssignTo(
                                        callType,
                                        constructorType,
                                    )
                                }
                                //println("parentConstructor=$parentConstructor")
                                indenter.line("this.${parentConstructor?.jsName}(${delegatedCall.exprs.joinToString(", ") { generateExpr(it).toString() }});")
                            }
                            DelegationCallKind.SUPER -> TODO()
                            null -> TODO()
                        }
                    }
                    generateStm(decl.body, parent)
                    indenter.line("return this;")
                }
            }
            // Already included in the main constructor
            is InitDecl -> {
                Unit
            }
            else -> TODO("decl=$decl")
        }
    }

    open fun generateClass(clazz: ClassLikeDecl) {
        clazz.externalJsBody?.let { indenter.line(it) }

        // Do not output external declarations
        if (Modifier.EXTERNAL in clazz) return

        val subTypes = clazz.subTypes ?: emptyList()
        var extends: ClassLikeDecl? = null
        for (subType in subTypes) {
            val decl = subType.type.resolveType(subType.type) as? ClassLikeDecl ?: continue
            //val decl = currentResolutionContext[subType.type]?.decls?.firstOrNull() as? ClassLikeDecl ?: continue
            if (!decl.kind.isInterface) {
                extends = decl
            }
        }
        val extendsStr = if (extends != null) " extends ${extends.jsName}" else ""
        //println("EXTENDS: ${extends}")
        indenter.line("class ${clazz.jsName}$extendsStr") {
            if (clazz is ObjectDecl) {
                indenter.line("static #\$_singleton = null; static get #\$singleton() { if (!this.#\$_singleton) this.#\$_singleton = new ${clazz.name}(); return this.#\$_singleton;  }")
            }

            if (clazz.primaryConstructor != null) {
                for (decl in clazz.bodyAll.filterIsInstance<VariableDecl>().filter { !it.delegation }) {
                    val type = decl.getNodeType()
                    val nullValue = type.defaultValueStr
                    indenter.line("${decl.jsName} = $nullValue;")
                    //generateDecl(decl, clazz)
                }
            }

            //generateDecls(clazz.bodyAll, clazz)
            for (decl in clazz.bodyAll) {
                if (clazz.primaryConstructor != null && (decl is VariableDecl && decl.isField)) continue
                generateDecl(decl, clazz, null)
            }
        }
    }

    //var headerLine: Indenter.Line? = null
    var transformContext = TransformUnsupportedNodes.TransformContext()

    open fun generateFunction(func: FunDecl, parent: Decl?) {
        if (Modifier.EXTERNAL in func) return

        val params = func.allParams.toJsString()

        //indenter.line("${decl.name}($params)") {
        //    decl.body?.let {
        //        generateStmsCompact(transformer.transform(it))
        //    }
        //}

        val functionMod = if (parent is ClassLikeDecl) "" else "function "
        val functionTypeMode = if (func.typeParams != null) " /* ${func.typeParams.joinToString(", ")} */ " else ""
        val suspendMod = if (func.isSuspend) "*" else ""
        if (func.jsName == "next\$1") {
            indenter.line("*[Symbol.iterator]()") {
                line("while (this.hasNext\$1())") {
                    line("yield this.next\$1();")
                }
            }
            //indenter.line("next()") {
            //    line("const done = !this.hasNext\$1();")
            //    line("if (done) return { done: true };")
            //    line("return { value: this.next\$1(), done: false };")
            //}
        }
        indenter.line("${functionMod}$functionTypeMode${suspendMod}${func.jsName}($params)") {
            func.body?.let {
                val oldContext = transformContext
                val headerLine = indenter.line("").also { it.opt = true }
                try {
                    transformContext = TransformUnsupportedNodes.TransformContext()
                    generateStmsCompact(transformer.transform(it.stms), func)
                } finally {
                    if (transformContext.temps.isNotEmpty()) {
                        headerLine.str += "let " + transformContext.temps.joinToString(", ") { "$it" } + ";"
                    }
                    transformContext = oldContext
                }
            }
        }
        if (func.isTopLevel && func.name == "main") {
            mainFunction = func
        }
    }

    fun generateThis(node: Node): String {
        val decl = node.currentDecl
        return when {
            decl is FunDecl && decl.receiver != null -> "\$this"
            else -> "this"
        }
    }

    open fun generateExpr(expr: Expr?): Any {
        return when (expr) {
            null -> "null"
            //        is ClassOrObjectDecl -> "(new $name$paramsStr)"
            //        is BaseConstructorDecl -> "(new ${resolved.parent?.jsName}).${resolved.jsName}$paramsStr"
            //        is FunDecl -> "$thisStr$name$paramsStr"
            is IdentifierExpr -> {
                val thisStr = if (expr.addThis) "${generateThis(expr)}." else ""
                val resolved = expr.resolvedDecl
                val name = resolved?.jsName ?: expr.id
                when (resolved) {
                    is BaseConstructorDecl -> {
                        val clazzParent = resolved.parent
                        val externalJsName = clazzParent?.externalJsName
                        if (externalJsName != null) {
                            "new $externalJsName"
                        } else {
                            "(new ${clazzParent?.jsName}).$name"
                        }
                    }
                    is ClassDecl -> "new ${resolved.jsName}"
                    is VariableDecl -> "$thisStr$name"
                    is FuncValueParam -> "$thisStr$name"
                    is FunDecl -> "$thisStr${resolved.jsName}"
                    else -> {
                        TODO("resolved=$resolved, thisStr=$thisStr, name='$name'")
                        "$thisStr$name"
                    }
                }

            }
            //is CallIdExpr -> {
            //    val resolved = expr.resolvedDecl ?: return "\$CALL_ID!!!!"
            //    val name = resolved.jsName
            //    val thisStr = if (expr.addThis) "this." else ""
            //    val paramsStr = "(" + expr.paramsPlusLambda.joinToString(", ") { generateExpr(it).toString() } + ")"
            //    when (resolved) {
            //        is ClassOrObjectDecl -> "(new $name$paramsStr)"
            //        is BaseConstructorDecl -> "(new ${resolved.parent?.jsName}).${resolved.jsName}$paramsStr"
            //        is FunDecl -> "$thisStr$name$paramsStr"
            //        //is IdentifierExpr -> "$name$paramsStr"
            //        else -> TODO("resolved=$resolved, expr=$expr")
            //    }
            //}
            is NavigationExpr -> {
                if (expr.op != ".") error("Unsupported ${expr.op}")
                //println("expr.resolvedDecl=${expr.resolvedDecl}")
                val resolvedDecl = expr.resolvedDecl
                val keyStr = when (expr.key) {
                    is Expr -> generateExpr(expr.key)
                    else -> {
                        resolvedDecl?.jsName ?: expr.key.toString()
                    }
                }

                run {
                //if (resolvedDecl is FunDecl && resolvedDecl.receiver != null && exprExpr is NavigationExpr) {
                //    val args2 = listOf(generateExpr(exprExpr.expr), *args.toTypedArray())
                //    val paramsStr = "(" + args2.joinToString(", ") + ")"
                //    "${resolvedDecl.jsName}$paramsStr"
                //} else {
                    "${generateExpr(expr.expr)}.$keyStr"
                }
            }
            is BoolLiteralExpr -> "${expr.value}"
            is IntLiteralExpr -> "${expr.value}"
            is CharLiteralExpr -> "${expr.value.code}"
            is StringLiteralExpr -> "\"${expr.value}\"" // @TODO: Escaping
            is UnaryPostOpExpr -> {
                val exprStr = generateExpr(expr.expr)
                when (expr.op) {
                    UnaryPostOp.INCR -> "$exprStr++"
                    UnaryPostOp.DECR -> "$exprStr--"
                    UnaryPostOp.NOT_NULL -> exprStr
                }
            }
            is UnaryPreOpExpr -> {
                val exprStr = generateExpr(expr.expr)
                when (expr.op) {
                    UnaryPreOp.MINUS -> "-($exprStr)"
                    UnaryPreOp.PLUS -> "+($exprStr)"
                    UnaryPreOp.EXCL -> "!($exprStr)"
                    UnaryPreOp.INCR -> "++($exprStr)"
                    UnaryPreOp.DECR -> "--($exprStr)"
                }
            }
            //is OpSeparatedBinaryExprs -> {
            //    val exprStrs = expr.exprs.map { generateExpr(it) }
            //    exprStrs[0].toString() + " " + (0 until expr.ops.size).joinToString(" ") {
            //        expr.ops[it] + " " + exprStrs[it + 1]
            //    }
            //}
            is BinaryOpExpr -> {
                val leftType = expr.left.getNodeType()
                val rightType = expr.right.getNodeType()
                val leftStr = generateExpr(expr.left)
                val rightStr = generateExpr(expr.right)
                if (expr.resolvedFunc != null) {
                    //"${expr.resolvedFunc!!.jsName}($leftStr, $rightStr)"
                    "$leftStr.${expr.resolvedFunc!!.jsName}($rightStr)"
                } else {
                    val op = when (expr.op) {
                        "xor" -> "^"
                        "or" -> "|"
                        "and" -> "&"
                        "shl" -> "<<"
                        "shr" -> ">>"
                        "ushr" -> ">>>"
                        else -> expr.op
                    }
                    when (expr.getNodeType()) {
                        BoolType -> "((($leftStr) $op ($rightStr))!=0)"
                        IntType -> "((($leftStr) $op ($rightStr))|0)"
                        else -> "(($leftStr) $op ($rightStr))"
                    }
                }
            }
            is CallExpr -> {
                //val symbols = symbolProvider[expr.id]
                //println("expr.id=${expr.id} : $symbols")

                val exprExpr = expr.expr

                val resolvedDecl = when (exprExpr) {
                    //is NavigationExpr -> exprExpr.resolvedDecl
                    else -> expr.resolvedDecl
                }
                val args = expr.paramsPlusLambda.map { generateExpr(it).toString() }
                if (resolvedDecl is FunDecl && resolvedDecl.receiver != null && exprExpr is NavigationExpr) {
                    val args2 = listOf(generateExpr(exprExpr.expr), *args.toTypedArray())
                    val paramsStr = "(" + args2.joinToString(", ") + ")"
                    "${resolvedDecl.jsName}$paramsStr"
                } else {
                    //println("resolvedDecl=$resolvedDecl")
                    val resStr = generateExpr(exprExpr)
                    val paramsStr = "(" + args.joinToString(", ") + ")"
                    "$resStr$paramsStr"
                }
            }
            is LambdaFunctionExpr -> {
                // When creating the lambda, the locals from other scopes are passed at the time of creation so:
                //   let captured = 10
                //   const func = ((captured) => () => 1 + captured)(captured)
                //   captured++ // This doesn't affect, since the lambda was already created with the older captured value
                // if locals from parent scope are not used, we can simply:
                //   () => 1 + captured

                val str = initLambdaBlock {
                    val stms = expr.body.stms.stms
                    for ((index, stm) in stms.withIndex()) {
                        val isLast = (index == stms.size - 1)
                        if (isLast && stm is ExprStm) {
                            generateStmCompact(ReturnStm(stm.expr).copyFrom(stm), null)
                        } else {
                            generateStmCompact(stm, null)
                        }
                    }
                }
                val params = expr.allParams.map { it.jsName }
                val paramsStr = params.joinToString(", ")
                // TODO: determine locals from upper scopes
                val capturedArgs = expr.body.varCaptures.map { it.declName }
                val capturedArgsStr = capturedArgs.joinToString(", ")
                when {
                    capturedArgsStr != "" -> "(($capturedArgsStr) => ($paramsStr) => { $str })($capturedArgsStr)"
                    else -> "($paramsStr) => { $str }"
                }
            }
            is BreakExpr -> throw UnsupportedOperationException("break expression not supported")
            is ContinueExpr -> throw UnsupportedOperationException("continue expression not supported")
            is InterpolatedStringExpr -> {
                val out = arrayListOf<String>()
                out += "`"
                for (chunk in expr.chunks) {
                    when (chunk) {
                        is InterpolatedStringExpr.StringChunk -> {
                            out += chunk.string
                        }
                        is InterpolatedStringExpr.ExpressionChunk -> {
                            val exprStr = generateExpr(chunk.expr).toString()
                            out += "\${$exprStr}"
                        }
                    }
                }
                out += "`"
                out.joinToString("")
            }
            is ThisExpr -> generateThis(expr)
            //is BreakExpr -> if (expr.label != null) "break ${expr.label};" else "break;"
            //is ContinueExpr -> if (expr.label != null) "continue ${expr.label};" else "continue;"
            is TempExpr -> "${expr.temp}"
            is TernaryExpr -> {
                val cond = generateExpr(expr.cond).toString()
                val trueStr = generateExpr(expr.trueExpr).toString()
                val falseStr = generateExpr(expr.falseExpr).toString()
                "(($cond) ? ($trueStr) : ($falseStr))"
            }
            is CallableReferenceExt -> {
                check(expr.kind == "class") { "Other callable is unsupported" }
                if (expr._variableDecl != null) {
                    //"Object.getPrototypeOf(${expr.type})"
                    "${expr.type}.constructor"
                } else {
                    "${expr.resolveType(expr.type ?: UnknownType)?.jsName}"
                }
            }
            is IndexedExpr -> {
                "${generateExpr(expr.expr)}[${expr.indices.joinToString(", ") { generateExpr(it).toString() }}]"
            }
            else -> TODO("generateExpr: $expr")
        }
    }

    open fun initLambdaBlock(block: () -> Unit): String {
        val oldIndenter = indenter
        try {
            indenter = Indenter()
            block()
            return indenter.indentToString()
        } finally {
            indenter = oldIndenter
        }
    }

    val transformer = TransformUnsupportedNodes {
        when (it) {
            // ReturnExpr should be ReturnStm in JS
            is ReturnExpr -> false
            else -> true
        }
    }

    fun generateStmsCompact(stm: Stm?, parent: Decl?) {
        when (stm) {
            null -> return
            is Stms -> for (stm in stm.stms) generateStm(stm, parent)
            else -> generateStm(stm, parent)
        }
    }

    infix fun Indenter.Line.ELSE(block: () -> Unit) {
        str += " else {"
        indenter.indent {
            block()
        }
        indenter.line("}")
    }
    open fun generateStmCompact(stm: Stm?, parent: Decl?) {
        if (stm is Stms) {
            for (stm in stm.stms) generateStm(stm, parent)
        } else {
            generateStm(stm, parent)
        }
    }
    open fun generateStm(stm: Stm?, parent: Decl?) {
        _generateStm(transformer.ensure(stm, transformContext), parent)
    }

    open fun _generateStm(stm: Stm?, parent: Decl?) {
        when (stm) {
            null -> Unit
            is AssignStm -> {
                indenter.line("${generateExpr(stm.lvalue)} ${stm.op} ${generateExpr(stm.expr)};")
            }
            is IfStm -> {
                val (pre, expr) = transformer.ensure(stm.cond, transformContext)
                pre?.let { generateStm(pre, parent) }
                if (expr != null) {
                    val lin = indenter.line("if (${generateExpr(expr)})") {
                        generateStmsCompact(stm.btrue, parent)
                    }
                    if (stm.bfalse != null) {
                        lin ELSE {
                            generateStmsCompact(stm.bfalse, parent)
                        }
                    }
                }
            }
            is LoopStm -> {
                val labelStr = if (stm.modifiers.label != null) "${stm.modifiers.label?.id}: " else ""
                when (stm) {
                    is ForLoopStm -> {
                        val (pre, expr) = transformer.ensure(stm.expr, transformContext)
                        pre?.let { generateStm(pre, parent) }
                        if (expr != null) {
                            //if (expr is BinaryOpExpr && expr.left.getNodeType() == IntType && expr.right.getNodeType() == IntType && (expr.op == ".." || expr.op == "..<" || expr.op == "until" || expr.op == "downTo")) {
                            if (expr is BinaryOpExpr && (expr.op == ".." || expr.op == "..<" || expr.op == "until" || expr.op == "downTo")) {
                                val varg = generateVarDecl(stm.vardecl)
                                //println("expr=$expr")
                                val comparisonOp = when (expr.op) {
                                    "downTo" -> ">="
                                    "..<", "until" -> "<"
                                    else -> "<="
                                }
                                val incrOp = when (expr.op) {
                                    "downTo" -> "--"
                                    else -> "++"
                                }
                                val LOOPSTART = transformContext.createTemp(IntType)
                                val LOOPEND = transformContext.createTemp(IntType)
                                indenter.line("$LOOPSTART = ${generateExpr(expr.left)};")
                                indenter.line("$LOOPEND = ${generateExpr(expr.right)};")
                                indenter.line("${labelStr}for (let $varg = $LOOPSTART; $varg $comparisonOp $LOOPEND; $varg$incrOp)") {
                                    generateStmsCompact(stm.body, parent)
                                }
                            } else {
                                indenter.line("${labelStr}for (const ${generateVarDecl(stm.vardecl)} of (${generateExpr(expr)}))") {
                                    generateStmsCompact(stm.body, parent)
                                }
                            }
                        }
                    }
                    is WhileLoopStm -> {
                        val (pre, expr) = transformer.ensure(stm.cond, transformContext)
                        pre?.let { generateStm(pre, parent) }
                        if (expr != null) {
                            indenter.line("${labelStr}while (${generateExpr(expr)})") {
                                generateStmsCompact(stm.body, parent)
                            }
                        }
                    }
                    is DoWhileLoopStm -> {
                        val (pre, expr) = transformer.ensure(stm.cond, transformContext)
                        pre?.let { generateStm(pre, parent) }
                        if (expr != null) {
                            indenter.line("${labelStr}do") {
                                generateStmsCompact(stm.body, parent)
                            } APPEND " while (${generateExpr(expr)})"
                        }
                    }
                }
            }
            is Stms -> {
                indenter.line("") {
                    for (stm in stm.stms) {
                        generateStm(stm, parent)
                    }
                }
            }
            is ExprStm -> {
                val (pre, expr) = transformer.ensure(stm.expr, transformContext)
                pre?.let { generateStm(pre, parent) }
                expr?.let { indenter.line("${generateExpr(expr)};") }
            }
            is ReturnStm -> {
                if (stm.expr == null) {
                    indenter.line("return;")
                    return
                }
                val (pre, expr) = transformer.ensure(stm.expr, transformContext)
                pre?.let { generateStm(pre, parent) }
                expr?.let { indenter.line("return ${generateExpr(expr)};") }
            }
            is BreakStm -> indenter.line("break ${stm.label ?: ""};")
            is ContinueStm -> indenter.line("continue ${stm.label ?: ""};")
            is DeclStm -> generateDecl(stm.decl, parent, null)
            is ThrowStm -> indenter.line("throw ${generateExpr(stm.expr)};")
            else -> TODO("generateStm: $stm")
        }
    }

    private fun generateVarDecl(vardecl: VariableDeclBase?): String {
        return when (vardecl) {
            is MultiVariableDecl -> "(" + vardecl.decls.joinToString(", ") { it.id } + ")"
            is VariableDecl -> vardecl.id
            is FuncValueParam -> vardecl.id
            null -> TODO("vardecl=null")
        }
    }

    fun Type.toJsString(): String = when (this) {
        is SimpleType -> this.name
        else -> "$this"
    }

    fun List<FuncValueParam>.toJsString(): String {
        return this.joinToString(", ") { "${it.id} /*: ${it.type?.toJsString()}*/" }
    }

    val Type.defaultValueStr: String get() = when (this) {
        IntType, CharType -> "0"
        BoolType -> "false"
        else -> "null"
    }
}
