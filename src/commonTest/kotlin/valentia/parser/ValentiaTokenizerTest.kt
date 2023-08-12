package valentia.parser

import kotlin.test.Test

class ValentiaTokenizerTest {
    @Test
    fun test() {
        println(ValentiaTokenizer("1 == 2").tokenize())
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