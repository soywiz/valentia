package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserEnumClassTest : DeclBuilder, StmBuilder {

    @Test
    fun testEnumClass() {
        assertEquals(
            null,
            ValentiaParser.topLevelDecl("""
                enum class ClassModifier(override val id: String) : Modifier {
                    ENUM("enum"), SEALED("sealed"), ANNOTATION("annotation"), DATA("data"), INNER("inner"), VALUE("value");
                    companion object {
                        val BY_ID = entries.associateBy { it.id }
                    }
                }
            """.trimIndent()) as? Any?
        )
    }

}