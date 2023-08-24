package valentia.sema

import valentia.ast.FuncType
import valentia.ast.Type
import valentia.ast.UnknownType

object TypeMatching {
    fun canAssignTo(src: Type?, dst: Type?): Boolean {
        if (src is FuncType && dst is FuncType) {
            val srcParams2 = src.params
            val dstParams = dst.params
            // @TODO: This is for lambdas
            val srcParams = if (srcParams2.isEmpty() && dstParams.size == 1) listOf(dstParams[0]) else srcParams2

            if (srcParams.size != dstParams.size) return false
            for ((srcP, dstP) in srcParams.zip(dstParams)) {
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