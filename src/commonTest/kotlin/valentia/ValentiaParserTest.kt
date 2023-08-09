package valentia

import valentia.ast.*
import valentia.parser.ValentiaParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTest {
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
            WhileLoopStm(IntLiteralExpr(1), EmptyStm()),
            ValentiaParser("while (1) ;").whileStatement()
        )
    }

    @Test
    fun testSimplestFor() {
        assertEquals(
            ForLoopStm(IntLiteralExpr(1), null, null),
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
        val expr = ValentiaParser("1_000 * 2_0_0").expression()

        assertEquals(
            OpSeparatedExprs(listOf("*"), listOf(IntLiteralExpr(1000), IntLiteralExpr(200))),
            ValentiaParser("1_000 * 2_0_0").expression()
        )
    }

    @Test
    fun testExpr() {
        assertEquals(
            OpSeparatedExprs(listOf("*"), listOf(IntLiteralExpr(2),
                OpSeparatedExprs(listOf("+"), listOf(IntLiteralExpr(3), IntLiteralExpr(4)))
            )),
            ValentiaParser("2 * (3 + 4)").expression()
        )
    }

    @Test
    fun testInfix() {
        assertEquals(
            OpSeparatedExprs(listOf("shl"), listOf(IntLiteralExpr(1), IntLiteralExpr(5))),
            ValentiaParser("1 shl 5").expression()
        )
    }

    @Test
    fun testIn() {
        assertEquals(
            RangeTestExpr(IntLiteralExpr(1), "in", IntLiteralExpr(5)),
            ValentiaParser("1 in 5").expression()
        )
    }

    @Test
    fun testNotIn() {
        assertEquals(
            RangeTestExpr(IntLiteralExpr(1), "!in", IntLiteralExpr(5)),
            ValentiaParser("1 !in 5").expression()
        )
    }

    @Test
    fun testIs() {
        assertEquals(
            TypeTestExpr(IntLiteralExpr(1), "is", SimpleType("Int")),
            ValentiaParser("1 is Int").expression()
        )
    }

    @Test
    fun testNotIs() {
        assertEquals(
            TypeTestExpr(IntLiteralExpr(1), "!is", SimpleType("Int")),
            ValentiaParser("1 !is Int").expression()
        )
    }
}