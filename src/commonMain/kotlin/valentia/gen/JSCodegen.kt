package valentia.gen

import valentia.ast.*
import valentia.sema.*
import valentia.transform.TransformUnsupportedNodes
import valentia.util.Indenter
import valentia.util.indent

open class JSCodegen {
    var hasMainFunction = false

    var indenter = Indenter()

    open fun supportedNode(node: Node): Boolean {
        return true
    }

    open fun generateProgram(program: Program) {
        for (module in program.modulesById.values) {
            generateModule(module)
        }
        if (hasMainFunction) {
            indenter.line("main([])")
        }
    }
    open fun generateModule(module: Module) {
        for (file in module.filesByPackage.values) {
            generatePackage(file)
        }
    }

    open fun generatePackage(pack: Package) {
        indenter.line("// package: ${pack.identifier}")
        for (file in pack.files) {
            generateFile(file)
        }
    }

    var symbolProvider: SymbolProvider = EmptySymbolProvider

    fun <T> pushSymbolProvider(symbolProvider: SymbolProvider, block: () -> T): T {
        val parent = this.symbolProvider
        try {
            this.symbolProvider = this.symbolProvider + symbolProvider
            return block()
        } finally {
            this.symbolProvider = parent
        }
    }

    open fun generateFile(file: FileNode) {
        indenter.line("// topLevelDecls: ${file.topLevelDecls.size}")
        pushSymbolProvider(file.symbolProvider) {
            generateDecls(file.topLevelDecls, null)
        }
    }

    open fun generateDecls(decls: List<Decl?>?, parent: Decl?) {
        for (decl in (decls ?: emptyList())) {
            generateDecl(decl, parent)
        }
    }

    open fun generateDecl(decl: Decl?, parent: Decl?) {
        indenter.line("// decl: ${decl}")
        when (decl) {
            null -> Unit
            is ClassDecl -> generateClass(decl)
            is FunDecl -> generateFunction(decl)
            is ObjectDecl -> {
                indenter.line("class ${decl.name}") {
                    indenter.line("static #\$_singleton = null; static get #\$singleton() { if (!this.#\$_singleton) this.#\$_singleton = new Simple(); return this.#\$_singleton;  }")
                    generateDecls(decl.body, decl)
                }
            }
            is VariableDecl -> {
                if (FunctionModifier.EXTERNAL !in decl.modifiers) {
                    if (parent is ClassOrObjectDecl) {
                        indenter.line("${decl.jsName} = ${generateExpr(decl.expr)};")
                    } else {
                        indenter.line("let ${decl.jsName} = ${generateExpr(decl.expr)};")
                    }
                }
            }
            else -> TODO("decl=$decl")
        }
    }

    open fun generateClass(clazz: ClassDecl) {
        indenter.line("class ${clazz.name}") {
            for (decl in clazz.body ?: emptyList()) {
                when (decl) {
                    is FunDecl -> {
                        val params = decl.params.joinToString(", ") { it.id }
                        indenter.line("${decl.name}($params)") {
                            decl.body?.let {
                                generateStmsCompact(transformer.transform(it))
                            }
                        }
                    }
                    else -> TODO("generateClass: $decl")
                }
            }
        }
    }

    open fun generateFunction(func: FunDecl) {
        val params = func.params.joinToString(", ") { it.id }
        val suspendMod = if (func.isSuspend) "*" else ""
        indenter.line("function ${suspendMod}${func.jsName}($params)") {
            func.body?.let {
                generateStmsCompact(transformer.transform(it))
            }
        }
        if (func.name == "main") {
            hasMainFunction = true
        }
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

    fun Node.getTypeSafe(resolutionContext: ResolutionContext): TypeNode {
        try {
            return getType(resolutionContext)
        } catch (e: Throwable) {
            println("ERROR[UnknownType]: ${e.message}")
            //e.printStackTrace()
            return UnknownType
        }
    }

    open fun generateExpr(expr: Expr?): Any {
        return when (expr) {
            null -> "null"
            is IdentifierExpr -> IdWithContext(expr.id, symbolProvider)
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
                    else -> TODO("Unsupported $expr")
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
                //println("expr.id=${expr.id} : $symbols")

                val res = generateExpr(expr.expr)
                val paramsStr = "(" + expr.paramsPlusLambda.joinToString(", ") { generateExpr(it).toString() } + ")"
                if (res is IdWithContext) {
                    val funcType = FuncTypeNode(UnknownType, expr.params.map { NamedTypeNode(it.getTypeSafe(
                        DummyResolutionContext
                    )) })
                    val resolved = res.resolve(funcType)
                    println("Can't resolve $funcType")
                    println("RESOLVE: $funcType : $resolved")
                    (resolved?.jsName ?: res.id) + paramsStr
                } else {
                    res.toString() + paramsStr
                }
            }
            is NavigationExpr -> {
                if (expr.op != ".") error("Unsupported ${expr.op}")
                val keyStr = when (expr.key) {
                    is Expr -> generateExpr(expr.key)
                    else -> expr.key.toString()
                }
                "${generateExpr(expr.expr)}.$keyStr"
            }
            is LambdaFunctionExpr -> {
                val str = initLambdaBlock {
                    for (stm in expr.stms) generateStm(stm)
                }
                "() => { $str }"
            }
            is BreakExpr -> throw UnsupportedOperationException("break expression not supported")
            is ContinueExpr -> throw UnsupportedOperationException("continue expression not supported")
            //is BreakExpr -> if (expr.label != null) "break ${expr.label};" else "break;"
            //is ContinueExpr -> if (expr.label != null) "continue ${expr.label};" else "continue;"
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




    fun generateStmsCompact(stm: Stm?) {
        when (stm) {
            null -> return
            is Stms -> for (stm in stm.stms) generateStm(stm)
            else -> generateStm(stm)
        }
    }

    infix fun Indenter.Line.ELSE(block: () -> Unit) {
        str += " else {"
        indenter.indent {
            block()
        }
        indenter.line("}")
    }

    open fun generateStm(stm: Stm) {
        when (stm) {
            is AssignStm -> {
                indenter.line("${generateExpr(stm.lvalue)} ${stm.op} ${generateExpr(stm.expr)};")
            }
            is IfStm -> {
                val (pre, expr) = transformer.ensure(stm.cond)
                pre?.let { generateStm(pre) }
                if (expr != null) {
                    val lin = indenter.line("if (${generateExpr(expr)})") {
                        generateStmsCompact(stm.btrue)
                    }
                    if (stm.bfalse != null) {
                        lin ELSE {
                            generateStmsCompact(stm.bfalse)
                        }
                    }
                }
            }
            is LoopStm -> {
                val labelStr = if (stm.modifiers.label != null) "${stm.modifiers.label?.id}: " else ""
                when (stm) {
                    is ForLoopStm -> {
                        val (pre, expr) = transformer.ensure(stm.expr)
                        pre?.let { generateStm(pre) }
                        if (expr != null) {
                            indenter.line("${labelStr}for (const ${generateVarDecl(stm.vardecl)} of ${generateExpr(expr)})") {
                                generateStmsCompact(stm.body)
                            }
                        }
                    }
                    is WhileLoopStm -> {
                        val (pre, expr) = transformer.ensure(stm.cond)
                        pre?.let { generateStm(pre) }
                        if (expr != null) {
                            indenter.line("${labelStr}while (${generateExpr(expr)})") {
                                generateStmsCompact(stm.body)
                            }
                        }
                    }
                    is DoWhileLoopStm -> TODO()
                }
            }
            is Stms -> {
                indenter.line("") {
                    for (stm in stm.stms) {
                        generateStm(stm)
                    }
                }
            }
            is ExprStm -> {
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                expr?.let { indenter.line("${generateExpr(expr)};") }
            }
            is ReturnStm -> {
                if (stm.expr == null) {
                    indenter.line("return;")
                    return
                }
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                expr?.let { indenter.line("return ${generateExpr(expr)};") }
            }
            is BreakStm -> indenter.line("break ${stm.label ?: ""};")
            is ContinueStm -> indenter.line("continue ${stm.label ?: ""};")
            else -> TODO("generateStm: $stm")
        }
    }

    private fun generateVarDecl(vardecl: VariableDeclBase?): String {
        return when (vardecl) {
            is MultiVariableDecl -> "(" + vardecl.decls.map { it.id }.joinToString(", ") + ")"
            is VariableDecl -> vardecl.id
            null -> TODO("vardecl=null")
        }
    }
}
