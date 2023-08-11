package valentia.sema

import valentia.ast.Decl
import valentia.ast.FileNode
import valentia.ast.Identifier

class Package(val identifier: Identifier?) {
    val symbols = LinkedHashMap<String, ArrayList<Decl>>()
    val files = arrayListOf<FileNode>()

    private fun getSymbolsByName(name: String): ArrayList<Decl> = symbols.getOrPut(name) { arrayListOf() }

    fun addFile(file: FileNode) {
        files += file
        file.symbolsByName.map { (name, decls) ->
            getSymbolsByName(name).addAll(decls)
        }
    }
}