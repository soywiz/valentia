package valentia.compiler

import valentia.ExternalInterface
import valentia.ast.Program
import valentia.gen.JSCodegen
import valentia.parser.ValentiaParser
import valentia.sema.SemaResolver
import kotlin.jvm.JvmName
import kotlin.time.measureTime

data class FileWithContents(val file: String, val content: String) {
    constructor(content: String) : this("unknown.kt", content)
}

data class CompileResult(
    val codegen: JSCodegen,
    val program: Program,
) {
    val jsString by lazy {
        codegen.indenter.indentToString()
    }
}

object ValentiaCompiler {
    @JvmName("compileFilePaths")
    fun compile(files: List<String>): CompileResult =
        compile(files.map { FileWithContents(it, ExternalInterface.fileReadString(it)) })

    fun compile(files: List<FileWithContents>): CompileResult {
        val program = Program()
        val module = program.getModule(null)
        val parsingTime = measureTime {
            for (file in files) {
                val fileContent = file.content
                val fileNodes = ValentiaParser.files(fileContent)
                module.addFiles(fileNodes)
            }
        }
        var resolvedProgram: Program = program
        val semanticAnalysisTime = measureTime { resolvedProgram = SemaResolver.resolve(program) }

        val codegen = JSCodegen()
        val generateCodeTime = measureTime {
            codegen.generateProgram(resolvedProgram)
        }

        //println("parsingTime=$parsingTime, semanticAnalysisTime=$semanticAnalysisTime, generateCodeTime=$generateCodeTime")
        return CompileResult(codegen, resolvedProgram)
    }

    fun compileAndRun(files: List<String>) {
        //println("Source:")
        val js = compile(files)
        //println(js)
        val jsFile = "/tmp/valentia.temp.js"
        ExternalInterface.fileWriteString(jsFile, js.jsString)
        val res = ExternalInterface.exec("deno", "run", "-A", "--unstable", jsFile)
        print(res.stdout)
        print(res.stderr)
        //println("---")
    }
}