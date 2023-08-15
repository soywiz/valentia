package valentia.sema

import valentia.ast.Decl
import valentia.ast.TypeNode

fun interface SymbolProvider {
    operator fun get(name: String): List<Decl>?
    operator fun get(type: TypeNode): List<Decl>? {
        return get(type.toString())
    }
}

object EmptySymbolProvider : SymbolProvider {
    override fun get(name: String): List<Decl>? = null
}

data class ComposedSymbolProvider(val current: SymbolProvider, val parent: SymbolProvider) : SymbolProvider {
    override fun get(name: String): List<Decl>? = current[name] ?: parent[name]
}

operator fun SymbolProvider.plus(other: SymbolProvider): SymbolProvider =
    ComposedSymbolProvider(other, this)

class SymbolContainer(
    val symbolsByName: LinkedHashMap<String, ArrayList<Decl>>
) : SymbolProvider {
    override fun get(name: String): List<Decl> = symbolsByName.getOrPut(name) { arrayListOf() }
}

interface ResolutionContext {
}

object DummyResolutionContext : ResolutionContext {
}