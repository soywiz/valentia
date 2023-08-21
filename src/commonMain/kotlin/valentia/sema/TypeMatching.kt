package valentia.sema

import valentia.ast.FuncType
import valentia.ast.Type
import valentia.ast.UnknownType

object TypeMatching {
    fun canAssignTo(src: Type?, dst: Type?): Boolean {
        if (src is FuncType && dst is FuncType) {
            if (src.params.size != dst.params.size) return false
            for ((srcP, dstP) in src.params.zip(dst.params)) {
                if (!canAssignTo(srcP.type, dstP.type)) return false
            }
            //src.params.zip()
            //TODO("$src -> $dst")
            return true
        }
        if (src == UnknownType) return true
        if (dst == UnknownType) return true
        if (src == dst) {
            return true
        }
        //TODO("src=$src == dst=$dst")
        return src == dst
    }
}