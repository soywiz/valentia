package valentia.sema

import valentia.ast.FileNode
import valentia.ast.Identifier

class Package(val identifier: Identifier?) {
    val files = arrayListOf<FileNode>()

    fun addFile(file: FileNode) {
        files += file
    }
}