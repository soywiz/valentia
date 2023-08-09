package valentia.parser

import valentia.ast.FileNode

open class ValentiaParser(str: String) : StrReader(str), KotlinParser {
    fun valentiaFile(): FileNode {
        return kotlinFile()
    }
}