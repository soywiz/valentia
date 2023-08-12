package valentia.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaTokenizerTest {
    @Test
    fun test() {
        println(ValentiaTokenizer("1 == 2").tokenize())
    }
    @Test
    fun test2() {
        fun chunks(str: String): List<String> = ValentiaTokenizer(str).tokenize().map { it.str }
        assertEquals(listOf("0x10", "+"), chunks("0x10+"))
        assertEquals(listOf("0o10", "+"), chunks("0o10+"))
        assertEquals(listOf("0b10", "+"), chunks("0b10+"))
        assertEquals(listOf("010", "+"), chunks("010+"))
        assertEquals(listOf("1l", "+"), chunks("1l+"))
        assertEquals(listOf("1ul", "+"), chunks("1ul+"))
        assertEquals(listOf("1f", "+"), chunks("1f+"))
        assertEquals(listOf("1.0f", "+"), chunks("1.0f+"))
        assertEquals(listOf("1e10", "+"), chunks("1e10+"))
        assertEquals(listOf("1e-10", "+"), chunks("1e-10+"))
        assertEquals(listOf("1e+10", "+"), chunks("1e+10+"))
        assertEquals(listOf("1.999", "+"), chunks("1.999+"))
        assertEquals(listOf("1", ".", "f", "+"), chunks("1.f+"))
        assertEquals(listOf("1", ".", "+"), chunks("1.+"))
        assertEquals(listOf("1.0", "+"), chunks("1.0+"))
        assertEquals(listOf("1", ".", "a", "+"), chunks("1.a+"))
    }

    @Test
    fun testInterpolationString() {
        println(ValentiaTokenizer("\"\"").tokenize())
        println(ValentiaTokenizer("\"hello \$world\"").tokenize())
    }

    @Test
    fun testInterpolationTripleString() {
        println(ValentiaTokenizer("${TRIPLE}HELLO\\n\${test}WORLD${TRIPLE}").tokenize())
    }

    @Test
    fun testComments() {
        println(ValentiaTokenizer("""
            package hello.world
            
            // this is a comment
            @Hello()
            import a.b.*
            
            /* /* */ */
        """.trimIndent()).tokenize())
    }

    val TRIPLE = "\"\"\""
}