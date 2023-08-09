package valentia

import valentia.parser.ValentiaParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTest {
    @Test
    fun test() {
        //assertEquals('1', ValentiaParser("1 + 2").context.peekChar())
        println(ValentiaParser("1 + 2").expression())
    }
}