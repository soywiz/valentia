package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTest : StmBuilder {

    @Test
    fun testCollectionLiteral() {
        assertEquals(
            CollectionLiteralExpr(listOf(1.lit, 2.lit, 3.lit)),
            ValentiaParser.expression("[1, 2, 3]")
        )
    }

    @Test
    fun test3() {
        assertEquals(
            null,
            ValentiaParser.statements("""
                fun test(a: Int, b: Int): Int {
                    return when (a + b) {
                        1 -> "a"
                        2 -> 'b' 
                    }
                }
            """) as? Any?
        )
    }

    @Test
    fun testStatements() {
        assertEquals(
            listOf(
                WHILE(true.lit) { STM("println".id(2.lit)) }
            ),
            ValentiaParser.statements("""
                //for (n in 0 until 10) println(1)
                while (true) {
                    println(2)
                }
        """)
        )
    }

    @Test
    fun testSimpleFile() {
        val file = ValentiaParser.file(
            """
            package test
            
            import a.b.Test3
            import a.b.c.*
            import a.b.Test4 as MyTest
            
            fun test(a: Int, b: Int) { 1 + 2; return 3 }
            class Test
            class Demo {
                companion object {
                    fun demo() {
                    }
                }
            }
            class Test2
            object Test5
            // test
            /*
            /**/
            fun test() { }
            fun demo() = 10
            val value = 10
            val value2: Int = 10
            */
            data class Demo2(val a: Int, val b: String)
        """.trimIndent())
    }

    @Test
    fun testPackage() {
        assertEquals(
            FileNode(_package = Identifier("hello.world")),
            ValentiaParser.file("  \npackage hello.world")
        )
    }

    @Test
    fun testImports() {
        assertEquals(
            listOf(
                ImportNode(Identifier("a.b")),
                ImportNode(Identifier("c.d")),
            ),
            ValentiaParser("import a.b\nimport c.d").importList()
        )
    }

    @Test
    fun testImportAll() {
        assertEquals(
            listOf(
                ImportNode(Identifier("a.b"), all = true),
                ImportNode(Identifier("c.d")),
            ),
            ValentiaParser("import a.b.*\nimport c.d").importList()
        )
    }

    @Test
    fun testImportAlias() {
        assertEquals(
            listOf(
                ImportNode(Identifier("a.b.Test"), all = false, alias = "demo"),
            ),
            ValentiaParser("import a.b.Test as demo").importList()
        )
    }

}