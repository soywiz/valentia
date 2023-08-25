package valentia.serial

import valentia.ast.GenericType
import valentia.ast.SimpleType
import valentia.ast.nullable
import kotlin.test.Test
import kotlin.test.assertEquals

class TypePoolTest {
    @Test
    fun test() {
        val pool = TypePool()
        val index0 = pool.getOrThrow(null)
        val index1 = pool[SimpleType("Hello")]
        val index1b = pool[SimpleType("Hello")]
        val index4 = pool[GenericType(SimpleType("Hello"), SimpleType("Hello").nullable(), SimpleType("Int"))]
        val index2 = pool[SimpleType("Hello").nullable()]
        val index3 = pool[SimpleType("Int")]
        assertEquals(0, index0)
        assertEquals(1, index1)
        assertEquals(1, index1b)
        assertEquals(2, index2)
        assertEquals(3, index3)
        assertEquals(4, index4)
        val pool2 = TypePool.fromByteArray(pool.toByteArray())
        assertEquals(pool, pool2)
        assertEquals(26, pool2.toByteArray().size)
    }
}