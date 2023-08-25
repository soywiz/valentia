package valentia.sema

import valentia.ast.ClassLikeDecl
import valentia.ast.Decl
import valentia.ast.Node

fun Node.getAscendantClassByName(id: String? = null): ClassLikeDecl? =
    currentDecl!!.getAscendantClassByName(id)

fun Decl.getAscendantClassByName(id: String? = null): ClassLikeDecl? {
    if (this is ClassLikeDecl) {
        if (id != null) {
            if (this.declName == id) return this
        } else {
            return this
        }
    }
    return parentDecl?.getAscendantClassByName(id)
}
