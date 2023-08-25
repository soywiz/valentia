package valentia.ast.cfg

import valentia.ast.AnyType
import valentia.ast.StmBuilder
import valentia.ast.StringType
import kotlin.test.Test

class BasicBlockTest : StmBuilder {
    @Test
    fun testSmartCastIf() {
        val stms = buildStmList {
            VAL_STM("x", "My string".lit, AnyType)
            IF_STM("x".id _is StringType) {
                RETURN_STM("x".lit["length"])
            } ELSE {
                STM("test".id(1.lit))
                //RETURN(3.lit)
            }
            WHILE(1.lit) {
                STM("inside_loop".id(1.lit))
            }
        }

        val builder = BasicBlockBuilder()
        println(builder.build(stms))
        println(builder.finalize())
    }
}