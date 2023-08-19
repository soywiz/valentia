package valentia.compiler

import valentia.ExternalInterface
import valentia.ast.Program
import valentia.gen.JSCodegen
import valentia.parser.ValentiaParser

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
        val js = compile(files)
        //println(js)
        val jsFile = "/tmp/valentia.temp.js"
        ExternalInterface.fileWriteString(jsFile, js)
        val res = ExternalInterface.exec("deno", "run", "-A", "--unstable", jsFile)
        print(res.stdout)
        print(res.stderr)
        //println("---")
    }
}