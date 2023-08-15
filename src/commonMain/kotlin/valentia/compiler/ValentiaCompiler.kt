package valentia.compiler

import valentia.ExternalInterface
import valentia.ast.FileNode
import valentia.gen.JSCodegen
import valentia.parser.ValentiaParser
import valentia.sema.Program

object ValentiaCompiler {
    fun compile(files: List<String>): String {
        val program = Program()
        val module = program.getModule(null)
        for (file in files) {
            val fileContent = ExternalInterface.fileReadString(file)
            val fileNode = ValentiaParser.file(fileContent)
            module.addFile(fileNode)
        }
        val codegen = JSCodegen()
        codegen.generateProgram(program)
        return codegen.indenter.indentToString()
    }

    fun compileAndRun(files: List<String>) {
        //println("Source:")
        println(compile(files))
        //println("---")
    }
}