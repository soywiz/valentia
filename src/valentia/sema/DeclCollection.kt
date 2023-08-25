package valentia.sema

import valentia.ast.ClassLikeDecl
import valentia.ast.Decl
import valentia.ast.FuncType
import valentia.ast.Type

inline class DeclCollection(val declsNull: List<Decl>?) {
    val decls: List<Decl> get() = declsNull ?: emptyList()

    fun findMatch(type: Type): Decl? {
        val items = declsNull ?: return null
        for (item in items) {
            // @TODO: Check constructors
            if (item is ClassLikeDecl) {
                for (constructor in item.constructors) {
                    val constructorFuncType = constructor.getNodeType()
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
            if (TypeMatching.canAssignTo(type, item.getNodeType())) {
                return item
            }
        }
        return null
    }
}
