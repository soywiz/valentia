package valentia.gen

import valentia.ast.*
import valentia.sema.Program
import valentia.transform.TransformUnsupportedNodes
import valentia.util.Indenter
import valentia.util.indent

open class JSCodegen : Codegen() {
    var hasMainFunction = false

    override fun generateProgram(program: Program) {
        super.generateProgram(program)
        if (hasMainFunction) {
            indenter.line("main([])")
        }
    }

    val transformer = TransformUnsupportedNodes {
        when (it) {
            // ReturnExpr should be ReturnStm in JS
            is ReturnExpr -> false
            else -> true
        }
    }

    override fun generateClass(clazz: ClassDecl) {
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

    override fun generateFunction(func: FunDecl) {
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

    override fun generateStm(stm: Stm) {
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

    override fun generateExpr(expr: Expr): Any {
        return super.generateExpr(expr)
    }
}
