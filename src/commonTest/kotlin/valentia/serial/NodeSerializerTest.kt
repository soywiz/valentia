package valentia.serial

import valentia.ast.DeclBuilder
import valentia.ast.ExprOrStm
import valentia.ast.NodeBuilder
import valentia.ast.StmBuilder
import kotlin.test.Test

@OptIn(ExperimentalStdlibApi::class)
class NodeSerializerTest : NodeBuilder, DeclBuilder, StmBuilder {
    fun exprToByteArray(expr: ExprOrStm?): ByteArray = NodeSerializer().writeExprOrStm(expr).toByteArray()
    fun byteArrayToExpr(bytes: ByteArray): ExprOrStm? = NodeDeserializer(bytes).readExprOrStm()
    fun testExpr(expr: ExprOrStm?): ExprOrStm? = byteArrayToExpr(exprToByteArray(expr))

    @Test
    fun testSerialize() {
        println(exprToByteArray(1.lit + 2.lit).toHexString())
        println(exprToByteArray(RETURN('c'.lit)).toHexString())
        println(exprToByteArray(RETURN(true.lit)).toHexString())
        println(exprToByteArray(THROW(true.lit)).toHexString())
        println(exprToByteArray(IF(true.lit) { 1.lit } ELSE { 2.lit }).toHexString())
    }

    @Test
    fun test() {
        println(testExpr(1.lit + 2.lit))
        println(testExpr(100.lit))
        println(testExpr(100L.lit))
        println(testExpr(RETURN('c'.lit)))
        println(testExpr(RETURN(true.lit)))
        println(testExpr(THROW(true.lit)))
        println(testExpr(IF(true.lit) { STM(1.lit) } ELSE { STM(2.lit) }))
        //println(testExpr(RETURN("hello".lit)))
    }
}