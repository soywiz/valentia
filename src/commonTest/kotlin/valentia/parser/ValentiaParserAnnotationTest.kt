package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserAnnotationTest : StmBuilder {

    @Test
    fun testEmptyAnnotation() {
        assertEquals(
            AnnotationNodes(AnnotationNode("Test".type)),
            ValentiaParser("@Test").annotation()
        )
    }

    @Test
    fun testEmptyArgsAnnotation() {
        assertEquals(
            AnnotationNodes(AnnotationNode("Test".type, emptyList())),
            ValentiaParser("@Test()").annotation()
        )
    }

    @Test
    fun testWithArgsAnnotation() {
        assertEquals(
            AnnotationNodes(AnnotationNode("Test".type, listOf(1.lit, "2".lit, collection(1.lit, 2.lit, 3.lit)))),
            ValentiaParser("@Test(1, \"2\", [1, 2, 3])").annotation()
        )
    }

}