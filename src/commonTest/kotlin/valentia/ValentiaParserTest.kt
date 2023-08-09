package valentia

import valentia.ast.*
import valentia.ast.NodeBuilder.Companion.id
import valentia.ast.NodeBuilder.Companion.lit
import valentia.parser.ValentiaParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTest : NodeBuilder, StmBuilder {
    @Test
    fun testSimpleClass() {
        assertEquals(
            null,
            ValentiaParser("""
                class Test {
                    fun demo() { println("1") }
                }
            """.trimIndent()).topLevelObject() as? Any?
        )
    }

    @Test
    fun testReturn() {
        assertEquals(
            RETURN(1.lit),
            ValentiaParser("return 1").expression() as? Any?
        )
    }

    @Test
    fun testReturnAt() {
        assertEquals(
            RETURN(1.lit, label = "test"),
            ValentiaParser("return@test 1").expression() as? Any?
        )
    }

    @Test
    fun testSimpleFunction() {
        assertEquals(
            //"println".id("Hello World!".lit),
            FUN("sum", IntType, "a" to IntType, "b" to IntType) {
                RETURN("a".id + "b".id)
            },
            ValentiaParser("fun sum(a: Int, b: Int): Int { return a + b }").functionDeclaration() as? Any?
        )
    }

    @Test
    fun testPrintHelloWorld() {
        assertEquals(
            "println".id("Hello World!".lit),
            ValentiaParser("println(\"Hello World!\")").expression()
        )
    }

    @Test
    fun testSimplestFunctionCall() {
        assertEquals(
            "exit".id(),
            ValentiaParser("exit()").expression()
        )
    }

    @Test
    fun testSimplerFunctionCall() {
        assertEquals(
            "exit".id(-(1).lit),
            ValentiaParser("exit(-1)").expression()
        )
    }

    @Test
    fun testSimpleFunctionCall() {
        assertEquals(
            "max".id(12.lit, 34.lit),
            ValentiaParser("max(12, 34)").expression()
        )
    }

    @Test
    fun testShebang() {
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser("#!/bin/sh -x\n").valentiaFile())
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser("#!/bin/sh -x").valentiaFile())
    }

    @Test
    fun testPackage() {
        assertEquals(
            FileNode(_package = Identifier("hello.world")),
            ValentiaParser("  \npackage hello.world").valentiaFile()
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

    @Test
    fun testSimplestWhile() {
        assertEquals(
            WHILE(1.lit) { },
            ValentiaParser("while (1) ;").whileStatement()
        )
    }

    @Test
    fun testSimplestFor() {
        assertEquals(
            ForLoopStm(IntLiteralExpr(1), vardecl = VariableDecls(VariableDecl(id = "n", type = null)), body = null),
            ValentiaParser("for (n in 1) ;").forStatement()
        )
    }

    @Test
    fun testSimplestExpr() {
        assertEquals(
            OpSeparatedExprs(listOf("+"), listOf(IntLiteralExpr(1), IntLiteralExpr(5))),
            ValentiaParser("1 \n + 5").expression()
        )
    }

    @Test
    fun testSimpleExpr() {
        assertEquals(
            OpSeparatedExprs(listOf("*"), listOf(IntLiteralExpr(1000), IntLiteralExpr(200))),
            ValentiaParser("1_000 * 2_0_0").expression()
        )
    }

    @Test
    fun testExpr() {
        assertEquals(
            2.lit * (3.lit + 4.lit),
            ValentiaParser("2 * (3 + 4)").expression()
        )
    }

    @Test
    fun testInfix() {
        assertEquals(
            1.lit shl 5.lit,
            ValentiaParser("1 shl 5").expression()
        )
    }

    @Test
    fun testIn() {
        assertEquals(
            1.lit _in 5.lit,
            ValentiaParser("1 in 5").expression()
        )
    }

    @Test
    fun testNotIn() {
        assertEquals(
            1.lit _notIn 5.lit,
            ValentiaParser("1 !in 5").expression()
        )
    }

    @Test
    fun testIs() {
        assertEquals(
            1.lit _is "Int".type,
            ValentiaParser("1 is Int").expression()
        )
    }

    @Test
    fun testNotIs() {
        assertEquals(
            1.lit _notIs "Int".type,
            ValentiaParser("1 !is Int").expression()
        )
    }

    @Test
    fun testAs() {
        assertEquals(
            1.lit.safeCastTo(SimpleType("Float")),
            ValentiaParser("1 as? Float").expression()
        )
        assertEquals(
            1.lit.castTo(SimpleType("Float")),
            ValentiaParser("1 as Float").expression()
        )
    }
}