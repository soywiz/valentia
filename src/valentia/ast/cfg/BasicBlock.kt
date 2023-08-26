package valentia.ast.cfg

import valentia.ast.*

// This will allow to figure out paths and do smart casts and other analysis
class CFG {
    var lastId = 1
    val blocks = arrayListOf<BasicBlock>()
    val edges = arrayListOf<BasicBlock.Edge>()

    val firstBasicBlock get() = blocks.first()

    fun create(): BasicBlock {
        return BasicBlock(this, lastId++).also { blocks += it }
    }

    fun createEdge(from: BasicBlock, to: BasicBlock, condition: BasicBlock.Condition? = null): BasicBlock.Edge {
        return BasicBlock.Edge(from, to, condition).also { edges += it }
    }

    fun finalize() {
        for (block in blocks) {
            val conds = block.predecessors.mapNotNull { it.condition?.expr }
            block.conditions = if (conds.isEmpty()) null else OpSeparatedBinaryExprs(conds.dropLast(1).map { "||" }, conds)
        }
        // @TODO: Evaluate smart casts in order
    }

    override fun toString(): String = buildString {
        appendLine("Blocks:")
        for (block in blocks) appendLine(" - $block")
        appendLine("Edges:")
        for (edge in edges) appendLine(" - $edge")
    }
}

/** Basic piece of the CFG */
class BasicBlock(val cfg: CFG, val id: Int, val label: String? = null) {
    data class Condition(val expr: Expr, val negate: Boolean = false)

    data class DeclWithInfo(val decl: Decl, val smartCast: Type? = null)

    class Edge(
        val from: BasicBlock,
        val to: BasicBlock,
        val condition: Condition? = null
    ) {
        override fun toString(): String = "Edge: ${from.id} -> ${to.id} [$condition]"
    }

    var leaf = false
    var conditions: Expr? = null
    val nodes: ArrayList<Node> = arrayListOf<Node>()
    val decls: ArrayList<DeclWithInfo> = arrayListOf()
    val predecessors: ArrayList<Edge> = arrayListOf()
    val successors: ArrayList<Edge> = arrayListOf()
    val varsById by lazy { decls.associateBy { it.decl.declName } }

    // @TODO: Replace with a BitSet?
    fun ancestors(visited: HashSet<Int> = HashSet()): Sequence<BasicBlock> = sequence {
        val current = this@BasicBlock
        visited += current.id
        yield(current)
        for (predecesor in predecessors) {
            if (predecesor.from.id in visited) continue
            yieldAll(predecesor.from.ancestors(visited))
        }
    }

    fun locateVar(id: String): DeclWithInfo? {
        varsById[id]?.let { return it }
        for (predecessor in predecessors) {
            val pred = predecessor.from
            //OpSeparatedBinaryExprs(listOf("||"), )
        }
        for (item in ancestors()) {
        }
        return null
    }

    fun connect(dst: BasicBlock, condition: Expr? = null, negated: Boolean = true) {
        val edge = cfg.createEdge(this, dst, condition?.let { Condition(it, negated) })
        this.successors.add(edge)
        dst.predecessors.add(edge)
    }

    fun addNode(node: Node) {
        node._basicBlock = this
        nodes += node
    }

    override fun toString(): String = "BasicBlock[$id](nodes=$nodes, predecessors=${predecessors.map { it.from.id }}, successors=${successors.map { it.to.id }}, decls=$decls, conditions=$conditions)"
}
