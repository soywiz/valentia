package valentia

import valentia.compiler.ValentiaCompiler

fun main(args: Array<String>) {
    val items = args.toMutableList()
    if (items.isEmpty()) {
        items.add("--help")
    }
    loop@while (items.isNotEmpty()) {
        val item = items.removeFirst()
        when (item) {
            "version", "-v", "-version", "--version" -> {
                println("Valentia ${Valentia.VERSION}")
                return
            }
            "help", "-h", "-help", "--help" -> {
                println("Valentia ${Valentia.VERSION} - soywiz - 2023")
                println("")
                println("valentia <command> <args>")
                println("")
                println("  version  - Displays version")
                println("  help     - Shows this help")
                println("  run   <file.kt>/<folder>/<package.valentia>")
                println("")
                println("Example:")
                println("")
                println("  valentia run file.kt")
                println("")
                return
            }
            "run", "-r", "-run", "--run" -> {
                val files = items.toList()
                items.clear()
                ValentiaCompiler.compileAndRun(files)
            }
        }
    }
}
