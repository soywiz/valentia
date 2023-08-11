package valentia.sema

class Program {
    val modulesById = LinkedHashMap<String?, Module>()

    fun getModule(id: String?): Module {
        return modulesById.getOrPut(id) { Module(id) }
    }
}