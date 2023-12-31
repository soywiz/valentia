package valentia.ast.cfg

import valentia.ast.*

class BasicBlockBuilder(private val cfg: CFG = CFG()) {
    companion object {
        fun build(stm: Stm): CFG = BasicBlockBuilder().also { it.build(stm) }.finalize()
    }
    // This assumes expressions don't have control flow instructions. So it cannot include a return or an if in an expression

    fun build(stm: Stm): Pair<BasicBlock, BasicBlock> = build(stm.toList())

    fun build(stms: List<Stm>): Pair<BasicBlock, BasicBlock> {
        var entry = cfg.create()
        var current = entry

        fun createNewBlock() {
            val new = cfg.create()
            current.connect(new)
            current = new
        }

        fun createNewBlockIfRequired() {
            if (current.nodes.isNotEmpty()) {
                createNewBlock()
            }
        }

        for (stm in stms) {
            when (stm) {
                is IfStm -> {
                    val old = current
                    val new = cfg.create()
                    val (start, end) = build(stm.btrue.toList())
                    start.addNode(stm)
                    old.connect(start, stm.cond)
                    if (!end.leaf) {
                        end.connect(new)
                    }
                    if (stm.bfalse != null) {
                        val (start, end) = build(stm.bfalse.toList())
                        old.connect(start, stm.cond, negated = true)
                        if (!end.leaf) {
                            end.connect(new)
                        }
                    } else {
                        old.connect(new)
                    }
                    current = new
                }
                is WhileLoopStm -> {
                    createNewBlock()
                    current.addNode(stm)
                    val (start, end) = build(stm.body)
                    current.connect(start, stm.cond)
                    if (!end.leaf) {
                        end.connect(current) // Unconditional
                    }
                    val new = cfg.create()
                    current.connect(new)
                    current = new
                }
                is ForLoopStm -> {
                    createNewBlock()
                    val before = current
                    val (start, end) = build(stm.body)
                    start.decls.add(BasicBlock.DeclWithInfo(stm.vardecl))
                    start.addNode(stm)
                    before.connect(start)
                    end.connect(before)
                    createNewBlock()
                    before.connect(current)
                }
                is ReturnStm -> {
                    current.leaf = true
                    current.addNode(stm)
                    break
                }
                //is WhenExpr -> TODO()
                is DeclStm -> {
                    // @TODO: Expression might require another subgraph? maybe not if we have preprocessed so control flow cannot happen in expressions
                    // DECLS should start a basic block
                    createNewBlockIfRequired()
                    current.decls += BasicBlock.DeclWithInfo(stm.decl)
                    current.addNode(stm)
                }
                else -> {
                    current.addNode(stm)
                }
            }
        }

        return entry to current
    }

    fun finalize(): CFG {
        return cfg.also { it.finalize() }
    }
}