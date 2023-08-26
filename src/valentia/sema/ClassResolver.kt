package valentia.sema

import valentia.ast.*

fun INode.getAscendantClassByName(id: String? = null): ClassLikeDecl? =
    currentDecl!!.getAscendantClassByName(id)

fun IDecl.getAscendantClassByName(id: String? = null): ClassLikeDecl? {
    if (this is ClassLikeDecl) {
        if (id != null) {
            if (this.declName == id) return this
        } else {
            return this
        }
    }
    return parentDecl?.getAscendantClassByName(id)
}
