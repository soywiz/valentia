package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserClassTest : StmBuilder {

    @Test
    fun testAnnotatedClass() {
        assertEquals(
            ClassDecl(kind = "class", name = "Demo"),
            ValentiaParser.topLevelDecl("@Test class Demo")
        )
    }

    @Test
    fun testAnnotationClass() {
        assertEquals(
            ClassDecl(kind = "class", name = "Demo"),
            ValentiaParser.topLevelDecl("annotation class Demo(val a: Int, val b: String)")
        )
    }

    @Test
    fun testClass() {
        assertEquals(
            ClassDecl(kind = "class", name = "Demo"),
            ValentiaParser.topLevelDecl("class Demo(val a: Int, val b: String)")
        )
    }

    @Test
    fun testSimpleClass() {
        assertEquals(
            ClassDecl(kind = "class", name = "Test", body = listOf(
                FUN("demo", null) {
                    STM("println".id("1".lit))
                }
            )),
            ValentiaParser.topLevelDecl("""
                class Test {
                    fun demo() { println("1") }
                }
            """.trimIndent())
        )
    }

    @Test
    fun testInheritance() {
        assertEquals(
            ClassDecl(kind = "class", name = "Hello", subTypes = listOf(BasicSubTypeInfo("World".type))),
            ValentiaParser.topLevelDecl("""class Hello : World""")
        )
    }

    @Test
    fun testConstructorDelegation() {
        assertEquals(
            ClassDecl(kind = "class", name = "Hello", body = listOf(
                ConstructorDecl(
                    constructorDelegationCall = ConstructorDelegationCall("this", listOf(1.lit, 2.lit))
                ),
            )),
            ValentiaParser.topLevelDecl("""
                class Hello {
                    constructor() : this(1, 2)
                }
            """.trimIndent())
        )
    }
}