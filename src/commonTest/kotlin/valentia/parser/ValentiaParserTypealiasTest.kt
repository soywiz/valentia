package valentia.parser

import valentia.ast.NodeBuilder.Companion.generic
import valentia.ast.NodeBuilder.Companion.type
import valentia.ast.TypeAliasDecl
import valentia.ast.TypeParameter
import valentia.ast.VisibilityModifier
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserTypealiasTest {
    @Test
    fun testTypealias() {
        assertEquals(
            TypeAliasDecl("Example", "Int".type),
            ValentiaParser.topLevelDecl("typealias Example = Int") as Any?
        )
    }

    @Test
    fun testTypealiasModifier() {
        assertEquals(
            TypeAliasDecl(
                id = "Example",
                type = "Int".type,
                types = null,
                modifiers = listOf(VisibilityModifier.PRIVATE)
            ),
            ValentiaParser.topLevelDecl("private typealias Example = Int") as Any?
        )
    }

    @Test
    fun testTypealiasFull() {
        assertEquals(
            TypeAliasDecl(
                id = "Example",
                type = "Map".type.generic("Int".type, "T".type),
                types = listOf(TypeParameter(id = "T", type = null)),
                modifiers = listOf(VisibilityModifier.PRIVATE)
            ),
            ValentiaParser.topLevelDecl("private typealias Example<T> = Map<Int, T>") as Any?
        )
    }
}
