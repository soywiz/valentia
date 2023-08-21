package valentia.serial

import kotlin.test.Test
import kotlin.test.assertEquals

class StringPoolTest {
    @Test
    fun test() {
        assertEquals(2, StringPool().toByteArray().size) // null is encoded by default in index 0
    }

    @Test
    fun test2() {
        val pool = StringPool()
        val index1 = pool["hello"]
        val index2 = pool["world"]
        val index1b = pool["hello"]
        assertEquals(1, index1)
        assertEquals(1, index1b)
        assertEquals(2, index2)
        assertEquals(14, pool.toByteArray().size)
        val pool2 = StringPool.fromByteArray(pool.toByteArray())
        assertEquals(pool, pool2)
    }
}