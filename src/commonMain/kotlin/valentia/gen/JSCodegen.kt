package valentia.gen

import valentia.ast.*
import valentia.sema.Program
import valentia.transform.TransformUnsupportedNodes
import valentia.util.Indenter
import valentia.util.indent

open class JSCodegen : Codegen(), Indenter by Indenter() {
    var hasMainFunction = false

    override fun generateProgram(program: Program) {
        super.generateProgram(program)
        if (hasMainFunction) {
            line("main([])")
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
        line("class ${clazz.name}") {
            for (decl in clazz.body ?: emptyList()) {
                when (decl) {
                    is FunDecl -> {
                        val params = decl.params.joinToString(", ") { it.id }
                        line("${decl.name}($params)") {
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
        line("function ${func.jsName}($params)") {
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
        indent {
            block()
        }
        line("}")
    }

    override fun generateStm(stm: Stm) {
        when (stm) {
            is AssignStm -> {
                line("${generateExpr(stm.lvalue)} ${stm.op} ${generateExpr(stm.expr)};")
            }
            is IfStm -> {
                val (pre, expr) = transformer.ensure(stm.cond)
                pre?.let { generateStm(pre) }
                if (expr != null) {
                    val lin = line("if (${generateExpr(expr)})") {
                        generateStmsCompact(stm.btrue)
                    }
                    if (stm.bfalse != null) {
                        lin ELSE {
                            generateStmsCompact(stm.bfalse)
                        }
                    }
                }
            }
            is ForLoopStm -> {
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                if (expr != null) {
                    line("for (const ${stm.vardecl}) of ${generateExpr(expr)}") {
                        generateStmsCompact(stm.body)
                    }
                }
            }
            is WhileLoopStm -> {
                val (pre, expr) = transformer.ensure(stm.cond)
                pre?.let { generateStm(pre) }
                if (expr != null) {
                    line("while (${generateExpr(expr)})") {
                        generateStmsCompact(stm.body)
                    }
                }
            }
            is Stms -> {
                line("") {
                    for (stm in stm.stms) {
                        generateStm(stm)
                    }
                }
            }
            is ExprStm -> {
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                expr?.let { line("${generateExpr(expr)};") }
            }
            is ReturnStm -> {
                if (stm.expr == null) {
                    line("return;")
                    return
                }
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                expr?.let { line("return ${generateExpr(expr)};") }
            }
            else -> TODO("generateStm: $stm")
        }
    }

    override fun generateExpr(expr: Expr): Any {
        return super.generateExpr(expr)
    }
}
