package valentia.parser

open class ValentiaParser(str: String) : StrReader(str), KotlinParser {
    fun valentiaFile() {
        kotlinFile()
    }
}