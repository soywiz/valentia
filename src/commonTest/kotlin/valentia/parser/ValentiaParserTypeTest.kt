package valentia.parser

import valentia.ast.NodeBuilder.Companion.generic
import valentia.ast.NodeBuilder.Companion.type
import valentia.ast.NodeBuilder.Companion.multi
import valentia.ast.NodeBuilder.Companion.multiType
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserTypeTest {
    @Test
    fun testType() {
        assertEquals(
            "Int".type,
            ValentiaParser.type("Int") as Any?
        )
    }

    @Test
    fun testTypeGeneric() {
        assertEquals(
            "Map".type.generic("Int".type, "String".type),
            ValentiaParser.type("Map<Int, String>") as Any?
        )
    }
}