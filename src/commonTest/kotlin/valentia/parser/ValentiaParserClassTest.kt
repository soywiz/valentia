package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValentiaParserClassTest : DeclBuilder, StmBuilder {

    @Test
    fun testAnnotatedClass() {
        assertEquals(
            ClassDecl(kind = ClassKind.CLASS, name = "Demo"),
            ValentiaParser.topLevelDecl("@Test class Demo")
        )
    }

    @Test
    fun testAnnotationClass() {
        assertEquals(
            ClassDecl(kind = ClassKind.CLASS, name = "Demo",
                primaryConstructor = PrimaryConstructorDecl(
                    ClassParameter("a", IntType, valOrVar = "val"),
                    ClassParameter("b", StringType, valOrVar = "val"),
                )
            ),
            ValentiaParser.topLevelDecl("annotation class Demo(val a: Int, val b: String)")
        )
    }

    @Test
    fun testClass() {
        val res = ValentiaParser.topLevelDecl("class Demo(val a: Int, val b: String)")
        assertTrue(res is ClassDecl)
        assertEquals(ClassKind.CLASS, res.kind)
        assertEquals("Demo", res.name)
    }

    @Test
    fun testSimpleClass() {
        assertEquals(
            ClassDecl(kind = ClassKind.CLASS, name = "Test", body = listOf(
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
            ClassDecl(
                kind = ClassKind.CLASS,
                name = "Hello",
                subTypes = listOf(SubTypeInfo("World".type)),
            ),
            ValentiaParser.topLevelDecl("""class Hello : World""")
        )
    }

    @Test
    fun testConstructorDelegation() {
        assertEquals(
            ClassDecl(
                kind = ClassKind.CLASS, name = "Hello", body = listOf(
                    ConstructorDecl(
                        constructorDelegationCall = ConstructorDelegationCall(DelegationCallKind.THIS, listOf(1.lit, 2.lit)),
                    ),
                )
            ),
            ValentiaParser.topLevelDecl("""
                class Hello {
                    constructor() : this(1, 2)
                }
            """.trimIndent())
        )
    }

    @Test
    fun testInterfaceWithEmptyMethod() {
        val file = ValentiaParser.file("""
            package demo

            interface Indenter {
                class Impl : Indenter { }
                fun indent()
            }
        """.trimIndent())

        //assertTrue(file is FileNode)
        assertEquals(Identifier("demo"), file._package)

        assertEquals(
            FILE(
                _package = Identifier("demo"),
            ) {
                INTERFACE("Indenter") {
                    CLASS("Impl", SubTypeInfo("Indenter".type)) {
                    }
                    FUN("indent")
                }
            },
            file
        )

    }
}