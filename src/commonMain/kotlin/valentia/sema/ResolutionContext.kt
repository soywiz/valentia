package valentia.sema

import valentia.ast.*
import valentia.util.Extra

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
    fun resolve(type: Type): Decl? = resolve().findMatch(type)
    override fun toString(): String = if (addThis) "this.$id" else id
}

inline class DeclCollection(val declsNull: List<Decl>?) {
    val decls: List<Decl> get() = declsNull ?: emptyList()
    fun findMatch(type: Type): Decl? {
        val items = declsNull ?: return null
        for (item in items) {
            // @TODO: Check constructors
            if (item is ClassOrObjectDecl) {
                for (constructor in item.constructors) {
                    val constructorFuncType = constructor.getType(DummyResolutionContext)
                    if (TypeMatching.canAssignTo(type, constructorFuncType)) {
                        return constructor
                    }
                }
                if (type is FuncType) {
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

interface ResolutionContext {
    val node: Node? get() = null
    operator fun get(name: String): DeclCollection = resolve(name)
    operator fun get(type: Type): DeclCollection = resolve(type.toString())
    fun resolve(id: String): DeclCollection
    fun getCurrentClass(id: String? = null): ClassOrObjectDecl? = null
}

operator fun ResolutionContext.plus(other: ResolutionContext): ResolutionContext {
    return ParentResolutionContext(this, other)
    //return when {
    //    this is ListResolutionContext && other is ListResolutionContext -> ListResolutionContext(this.resolutions + other.resolutions)
    //    this is ListResolutionContext -> ListResolutionContext(this.resolutions + other)
    //    other is ListResolutionContext -> ListResolutionContext(listOf(this) + other.resolutions)
    //    else -> ListResolutionContext(this, other)
    //}
}

open class ParentResolutionContext(val base: ResolutionContext, val parent: ResolutionContext) : ResolutionContext {
    override fun resolve(id: String): DeclCollection = DeclCollection(base.resolve(id).decls + parent.resolve(id).decls)
    override fun getCurrentClass(id: String?): ClassOrObjectDecl? = base.getCurrentClass(id) ?: parent.getCurrentClass(id)
}

//open class ListResolutionContext(val resolutions: List<ResolutionContext>) : ResolutionContext {
//    override val node: Node? get() = resolutions.getOrNull(0)?.node
//
//    constructor(vararg resolutions: ResolutionContext) : this(resolutions.toList())
//    override fun resolve(id: String): DeclCollection = DeclCollection(resolutions.flatMap { it.resolve(id).decls })
//}

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

open class ModuleResolutionContext(val module: Module) : ResolutionContext {
    override val node: Node? get() = module
    override fun resolve(id: String): DeclCollection {
        return DeclCollection(emptyList())
    }
}

/** Resolves imports and private declarations */
open class FileResolutionContext(val file: FileNode) : ResolutionContext {
    val importsById = file.importsById
    override val node: Node? get() = file
    override fun resolve(id: String): DeclCollection {
        return DeclCollection(file.topDeclsByName[id] ?: emptyList())
    }
}

open class ProgramResolutionContext(val program: Program) : ResolutionContext {
    override val node: Node? get() = program
    override fun resolve(id: String): DeclCollection {
        return DeclCollection(emptyList())
    }
}

val ClassOrObjectDecl.classMembersById: Map<String, List<Decl>> by Extra.PropertyThis {
    bodyAll.groupBy { it.declName }
}
open class ClassResolutionContext(val clazz: ClassOrObjectDecl) : ResolutionContext {
    val classMembersById = clazz.classMembersById
    override val node: Node? get() = clazz
    override fun resolve(id: String): DeclCollection = DeclCollection(classMembersById[id] ?: emptyList())
    override fun getCurrentClass(id: String?): ClassOrObjectDecl? {
        if (id == null) return clazz
        if (id === clazz.name) return clazz
        return null
    }
}

open class FunResolutionContext(val func: FunDecl) : ResolutionContext {
    override val node: Node? get() = func
    override fun resolve(id: String): DeclCollection {
        //DeclCollection(classMembersById[id] ?: emptyList())
        //println("@TODO: FunResolutionContext.resolve: $func")
        return DeclCollection(emptyList())
    }
}

val Decl.resolver: ResolutionContext by Extra.PropertyThis {
    when (this) {
        is FunDecl -> FunResolutionContext(this)
        is ClassOrObjectDecl -> ClassResolutionContext(this)
        is Package -> PackageResolutionContext(this)
        is Module -> ModuleResolutionContext(this)
        is FileNode -> FileResolutionContext(this)
        is Program -> ProgramResolutionContext(this)
        else -> TODO("this=$this")
    }
}

fun Node.resolve(id: String): Sequence<Decl> = sequence<Decl> {
    val node = this@resolve
    println("Resolving $node")
    val decl = currentDecl ?: return@sequence
    val parentDecl = decl.parentDecl
    println("    - $decl -> parentDecl=$parentDecl")
    //yieldAll(decl.resolver.resolve(id).decls)
    when (decl) {
        is Program -> {
        }
        is Module -> {
        }
        is Package -> {
            println("  PACKAGE")
            decl.allDeclsByName[id]?.let { yieldAll(it) }
        }
        is FileNode -> {
            println("  FILE")
            //yieldAll(decl.topLevelDecls.filter { it.declName == id })
            //decl?.pack?.allDeclsByName?.values[]
            decl.pack?.let { yieldAll(it.resolve(id)) }
        }
        is ClassOrObjectDecl -> {
            println("  CLASS")
            for (subType in decl.directResolvedSubTypes.distinct()) {
                println("Resolving.subType=$subType")
                yieldAll(subType.resolve(id))
            }
        }
        is FunDecl -> {
            println("  FUNC")
            parentDecl?.resolve(id)?.let { yieldAll(it) }
        }
        else -> TODO("$decl")
    }
    val parent = decl.parentNode
    if (parent != null) {
        println("Resolving.parent=$parent")
        yieldAll(parent.resolve(id))
    }
}

//fun Decl.resolve(id: String): DeclCollection {
//}
val ClassOrObjectDecl.directResolvedSubTypes: List<Decl> by Extra.PropertyThis {
    val subtypes = (this.subTypes ?: emptyList()).map { it.type }
    println(subtypes)
    val decls: List<Decl> = subtypes.mapNotNull { resolve(it.toString()).firstOrNull() }
    decls.distinct()
}

val SubTypeInfo.resolvedDecl: Decl? by Extra.PropertyThis {
    resolve(this.type.toString()).firstOrNull()
    //var resolvedDecl: Decl? = null
}