package valentia.sema

import valentia.ast.*
import valentia.util.Extra
import valentia.util.associateByList
/*
fun interface SymbolProvider : ResolutionContext {
    operator fun get(name: String): List<Decl>?
    operator fun get(type: TypeNode): List<Decl>? = get(type.toString())
    override fun resolve(id: String): List<Decl> = get(id) ?: emptyList()
}

object EmptySymbolProvider : SymbolProvider {
    override fun get(name: String): List<Decl>? = null
}

data class ComposedSymbolProvider(val current: SymbolProvider, val parent: SymbolProvider) : SymbolProvider {
    override fun get(name: String): List<Decl>? = current[name] ?: parent[name]
}

operator fun SymbolProvider.plus(other: SymbolProvider): SymbolProvider =
    ComposedSymbolProvider(other, this)

 lass SymbolContainer(
    val symbolsByName: LinkedHashMap<String, ArrayList<Decl>>
) : SymbolProvider {
    override fun get(name: String): List<Decl> = symbolsByName.getOrPut(name) { arrayListOf() }
}

*/

data class IdWithContext(val id: String, val context: ResolutionContext, val addThis: Boolean) {
    fun resolve(): DeclCollection = context[id]
    fun resolve(type: TypeNode): Decl? = resolve().findMatch(type)
    override fun toString(): String = if (addThis) "this.$id" else id
}

inline class DeclCollection(val decls: List<Decl>) {
    fun findMatch(type: TypeNode): Decl? {
        val items = decls
        for (item in items) {
            // @TODO: Check constructors
            if (item is ClassOrObjectDecl) {
                for (constructor in item.constructors) {
                    val constructorFuncType = constructor.getType(DummyResolutionContext)
                    if (TypeMatching.canAssignTo(type, constructorFuncType)) {
                        return constructor
                    }
                }
                if (type is FuncTypeNode) {
                    if (type.params.isEmpty()) {
                        return item
                    }
                }
                TODO("Can't find constructor for $type")
            }
            // @TODO: Compat
            if (TypeMatching.canAssignTo(type, item.getType(DummyResolutionContext))) {
                return item
            }
        }
        return null
    }
}

fun interface ResolutionContext {
    val node: Node? get() = null
    operator fun get(name: String): DeclCollection = resolve(name)
    operator fun get(type: TypeNode): DeclCollection = resolve(type.toString())
    fun resolve(id: String): DeclCollection
}

operator fun ResolutionContext.plus(other: ResolutionContext): ResolutionContext = when {
    this is ListResolutionContext && other is ListResolutionContext -> ListResolutionContext(this.resolutions + other.resolutions)
    this is ListResolutionContext -> ListResolutionContext(this.resolutions + other)
    other is ListResolutionContext -> ListResolutionContext(listOf(this) + other.resolutions)
    else -> ListResolutionContext(this, other)
}

open class ListResolutionContext(val resolutions: List<ResolutionContext>) : ResolutionContext {
    override val node: Node? get() = resolutions.getOrNull(0)?.node

    constructor(vararg resolutions: ResolutionContext) : this(resolutions.toList())
    override fun resolve(id: String): DeclCollection = DeclCollection(resolutions.flatMap { it.resolve(id).decls })
}

object DummyResolutionContext : ResolutionContext {
    override fun resolve(id: String): DeclCollection = DeclCollection(emptyList())
}

private val Package.allDeclsByName: LinkedHashMap<String, ArrayList<Decl>> by Extra.PropertyThis {
    LinkedHashMap<String, ArrayList<Decl>>().also { out ->
        for (file in files) for (decl in file.topLevelDecls) {
            val list = out.getOrPut(decl.declName) { arrayListOf() }
            list += decl
        }
    }
}

/** Resolves all public declarations in this package */
open class PackageResolutionContext(val pack: Package) : ResolutionContext {
    val allDeclsByName = pack.allDeclsByName
    override val node: Node? get() = pack

    override fun resolve(id: String): DeclCollection {
        return DeclCollection(pack.allDeclsByName[id] ?: emptyList())
    }
}

private val FileNode.topDeclsByName by Extra.PropertyThis {
    LinkedHashMap<String, ArrayList<Decl>>().also { out ->
        for (decl in topLevelDecls) {
            val list = out.getOrPut(decl.declName) { arrayListOf() }
            list += decl
        }
    }
}
val FileNode.importsById by Extra.PropertyThis { imports.associateBy { it.identifier.lastPart } }

/** Resolves imports and private declarations */
open class FileResolutionContext(val file: FileNode) : ResolutionContext {
    val importsById = file.importsById
    override val node: Node? get() = file
    override fun resolve(id: String): DeclCollection {
        return DeclCollection(file.topDeclsByName[id] ?: emptyList())
    }
}

val ClassOrObjectDecl.classMembersById: Map<String, List<Decl>> by Extra.PropertyThis {
    bodyAll.associateByList { it.declName }
}
open class ClassResolutionContext(val clazz: ClassOrObjectDecl) : ResolutionContext {
    val classMembersById = clazz.classMembersById
    override val node: Node? get() = clazz
    override fun resolve(id: String): DeclCollection = DeclCollection(classMembersById[id] ?: emptyList())
}

open class FunResolutionContext(val func: FunDecl) : ResolutionContext {
    override val node: Node? get() = func
    override fun resolve(id: String): DeclCollection {
        //DeclCollection(classMembersById[id] ?: emptyList())
        println("@TODO: FunResolutionContext.resolve: $func")
        return DeclCollection(emptyList())
    }
}