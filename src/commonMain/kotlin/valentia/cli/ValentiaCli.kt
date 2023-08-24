package valentia.cli

import valentia.Valentia
import valentia.compiler.ValentiaCompiler
import valentia.util.LocalFile

object ValentiaCli {
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
                    println("  [source] : <file.kt>/<folder>/<valentia.toml>")
                    println("")
                    println("  version  - Displays version")
                    println("  help     - Shows this help")
                    println("  run       [source]   - Compiles & runs generated code with Deno")
                    println("  compile   [source] > [output.js]  - Generates JS code")
                    println("  gradle    [source]   - Creates a gradle project to use existing tools for editing")
                    println("  edit      [source]   - Creates a gradle project and open `idea` to use existing tools for editing")
                    println("  new       [source]   - Creates a new valentia project")
                    println("")
                    println("Example:")
                    println("")
                    println("  valentia run file.kt")
                    println("  valentia compile file.kt > output.js")
                    println("")
                    return
                }
                "run", "-r", "-run", "--run" -> {
                    val files = items.toList()
                    items.clear()
                    if (files.isEmpty()) error("Missing file/s to execute")
                    ValentiaCompiler.compileAndRun(files)
                }
                "compile", "-c", "-compile", "--compile" -> {
                    val files = items.toList()
                    items.clear()
                    if (files.isEmpty()) error("Missing file/s to compile")
                    println(ValentiaCompiler.compile(files))
                }
                "edit", "-e", "-edit", "--edit" -> {
                    val files = items.toList()
                    items.clear()
                    println("TODO")
                }
                "new" -> {
                    val folder = LocalFile(items.removeFirstOrNull() ?: error("Folder not specified"))
                    val stat = folder.statOpt()
                    if (stat != null && !stat.isDir) error("Exists and not a directory '$folder'")
                    if (stat != null && folder.list().isNotEmpty()) error("Folder is not empty '$folder'")
                    folder.mkdirs()
                    folder["valentia.toml"].writeString("""
                        name = ${folder.baseName}
                        authors = ["dummy"]
                        [dependencies]
                    """.trimIndent())
                    folder["src"].mkdirs()
                    folder["test"].mkdirs()
                    folder["src"]["main.kt"].writeString("""
                        fun main() {
                            println("Hello World!")
                        }
                    """.trimIndent())
                }
                else -> {
                    error("Unknown command $item")
                }
            }
        }
    }
}