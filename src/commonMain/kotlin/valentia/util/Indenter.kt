package valentia.util

interface Indenter {
    class Impl : Indenter {
        private var level: Int = 0
        private val lines: ArrayList<Line> = arrayListOf()

        override fun line(str: String): Line {
            return Line(level, str).also { lines += it }
        }

        override fun indent() {
            level++
        }

        override fun unindent() {
            --level
        }

        override fun indentToString(): String {
            return lines.filter { !it.opt || it.str != "" }.joinToString("\n")
        }

        override fun toString(): String = indentToString()
    }

    companion object {
        @PublishedApi internal val INDENT_LEVELS = Array(128) { "  ".repeat(it) }
        operator fun invoke(): Indenter = Impl()
    }

    data class Line(val indentLevel: Int, var str: String) {
        var opt: Boolean = false
        override fun toString(): String = "${INDENT_LEVELS[indentLevel]}$str"
        infix fun APPEND(str: String): Line {
            this.str += str
            return this
        }
    }

    fun line(str: String): Line
    fun indent()
    fun unindent()

    fun line(str: String, suffix: String = " {", newline: String = "}", block: Indenter.() -> Unit): Line {
        line("$str$suffix")
        indent {
            block(this)
        }
        return line(newline)
    }

    fun indentToString(): String

    //override fun toString(): String = indentToString()
}


inline fun <T> Indenter.indent(block: () -> T): T {
    indent()
    try {
        return block()
    } finally {
        unindent()
    }
}
