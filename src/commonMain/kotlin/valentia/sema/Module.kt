package valentia.sema

import valentia.ast.FileNode
import valentia.ast.Identifier

class Module(val id: String? = null) {
    val filesByPackage = LinkedHashMap<Identifier?, Package>()

    fun getPackage(identifier: Identifier?): Package {
        return filesByPackage.getOrPut(identifier) { Package(identifier) }
    }

    fun addFile(file: FileNode) {
        getPackage(file._package).addFile(file)
    }
}