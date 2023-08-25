package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValentiaParserEnumClassTest : DeclBuilder, StmBuilder {

    @Test
    fun testEnumClass() {
        val enumClass = ValentiaParser.topLevelDecl("""
            enum class ClassModifier(override val id: String) : Modifier {
                ENUM("enum"), SEALED("sealed"), ANNOTATION("annotation"), DATA("data"), INNER("inner"), VALUE("value");
                companion object {
                    val BY_ID = entries.associateBy { it.id }
                }
            }
        """.trimIndent())
        assertTrue(enumClass is ClassDecl)
    }

}