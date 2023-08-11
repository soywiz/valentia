package valentia.gen

import valentia.ast.*
import valentia.sema.Module
import valentia.sema.Package
import valentia.sema.Program

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

    open fun generateFile(file: FileNode) {
        for (decl in file.topLevelDecls) {
            when (decl) {
                is ClassDecl -> generateClass(decl)
                is FunDecl -> generateFunction(decl)
            }
        }
    }

    open fun generateClass(clazz: ClassDecl) {
    }
    open fun generateFunction(func: FunDecl) {
    }
    open fun generateStm(stm: Stm) {
    }
    open fun generateExpr(expr: Expr): String {
        return when (expr) {
            is IdentifierExpr -> expr.id
            is BoolLiteralExpr -> "${expr.value}"
            is IntLiteralExpr -> "${expr.value}"
            is StringLiteralExpr -> "\"${expr.value}\"" // @TODO: Escaping
            is UnaryPostOpExpr -> {
                val exprStr = generateExpr(expr.expr)
                when (expr.op) {
                    UnaryPostOp.INCR -> "$exprStr++"
                    UnaryPostOp.DECR -> "$exprStr--"
                    UnaryPostOp.NOT_NULL -> exprStr
                }
            }
            is OpSeparatedExprs -> {
                val exprStrs = expr.exprs.map { generateExpr(it) }
                exprStrs[0] + " " + (0 until expr.ops.size).joinToString(" ") {
                    expr.ops[it] + " " + exprStrs[it + 1]
                }
            }
            is CallExpr -> {
                generateExpr(expr.expr) + "(" + expr.params.joinToString(", ") { generateExpr(it) } + ")"
            }
            else -> TODO("generateExpr: $expr")
        }
    }
}