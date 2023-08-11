package valentia.gen

import valentia.ast.*
import valentia.transform.TransformUnsupportedNodes
import valentia.util.Indenter

open class JSCodegen : Codegen(), Indenter by Indenter() {
    val transformer = TransformUnsupportedNodes {
        when (it) {
            // ReturnExpr should be ReturnStm in JS
            is ReturnExpr -> false
            else -> true
        }
    }

    override fun generateClass(clazz: ClassDecl) {
        line("class ${clazz.name}") {
        }
    }

    override fun generateFunction(func: FunDecl) {
        val params = func.params.joinToString(", ") { it.id }
        line("function ${func.name}($params)") {
            func.body?.let {
                generateStmsCompact(transformer.transform(it))
            }
        }
    }

    fun generateStmsCompact(stm: Stm) {
        if (stm is Stms) {
            for (stm in stm.stms) {
                generateStm(stm)
            }
        } else {
            generateStm(stm)
        }
    }

    override fun generateStm(stm: Stm) {
        when (stm) {
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
                    return line("return;")
                }
                val (pre, expr) = transformer.ensure(stm.expr)
                pre?.let { generateStm(pre) }
                expr?.let { line("return ${generateExpr(expr)};") }
            }
            else -> TODO("generateStm: $stm")
        }
    }

    override fun generateExpr(expr: Expr): String {
        return super.generateExpr(expr)
    }
}
