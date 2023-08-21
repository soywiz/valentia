package valentia.parser

import valentia.ast.FuncType
import valentia.ast.GenericType
import valentia.ast.NodeBuilder.Companion.generic
import valentia.ast.NodeBuilder.Companion.type
import valentia.ast.SimpleType
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

    @Test
    fun testTypeFunc() {
        assertEquals(
            FuncType(ret = SimpleType(name = "T"), params = listOf(FuncType.Item("Int".type))),
            ValentiaParser.type("(Int) -> T") as Any?
        )
    }

    @Test
    fun testTypeGenerics() {
        assertEquals(
            GenericType(
                "LinkedHashMap".type,
                generics = listOf("String".type, GenericType("ArrayList".type, generics = listOf("Decl".type)))
            ),
            ValentiaParser.type("LinkedHashMap<String, ArrayList<Decl>>") as Any?
        )
    }

    @Test
    fun testFun1() {
        assertEquals(
            FuncType("T".type, listOf(), receiver = "ValentiaParser".type),
            ValentiaParser.type("ValentiaParser.() -> T") as? Any?
        )
    }
}