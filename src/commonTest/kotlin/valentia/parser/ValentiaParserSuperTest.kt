package valentia.parser

import valentia.ast.NodeBuilder.Companion.type
import valentia.ast.SuperExpr
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserSuperTest {
    @Test
    fun testSuper() {
        assertEquals(
            SuperExpr(),
            ValentiaParser.expression("super")
        )
    }

    @Test
    fun testSuperLabel() {
        assertEquals(
            SuperExpr(label = "test"),
            ValentiaParser.expression("super@test")
        )
    }


    @Test
    fun testSuperTyped() {
        assertEquals(
            SuperExpr(type = "Test".type),
            ValentiaParser.expression("super<Test>") as? Any?
        )
    }

    @Test
    fun testSuperTypedLabel() {
        assertEquals(
            SuperExpr(label = "test", type = "Test".type),
            ValentiaParser.expression("super<Test>@test")
        )
    }
}