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
            this.symbolProvider = this.symbolProvider + symbolProvider
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
                val paramsStr = "(" + expr.params.joinToString(", ") { generateExpr(it).toString() } + ")"
                if (res is IdWithContext) {
                    val funcType = FuncTypeNode(UnknownType, expr.params.map { it.getType(DummyResolutionContext) })
                    val resolved = res.resolve(funcType) ?: error("Can't resolve $funcType")
                    println("RESOLVE: $funcType : $resolved")
                    resolved!!.jsName + paramsStr
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
            else -> TODO("generateExpr: $expr")
        }
    }
}