package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserTest : StmBuilder {
    @Test
    fun testInheritance() {
        assertEquals(
            ClassDecl(kind = "class", name = "Hello", subTypes = listOf(BasicSubTypeInfo(UserType("World".type)))),
            ValentiaParser.topLevelDecl("""class Hello : World""")
        )
    }

    @Test
    fun testObjectLiteral() {
        assertEquals(
            ObjectLiteralExpr(body = emptyList()),
            ValentiaParser.expression("""object { }""")
        )
        assertEquals(
            ObjectLiteralExpr(body = listOf(FunDecl(name="test", params=emptyList(), body=Stms()))),
            ValentiaParser.expression("""object { fun test() { } }""")
        )
    }

    @Test
    fun testCollectionLiteral() {
        assertEquals(
            CollectionLiteralExpr(listOf(1.lit, 2.lit, 3.lit)),
            ValentiaParser.expression("[1, 2, 3]")
        )
    }

    @Test
    fun testTryCatch() {
        assertEquals(
            TryExpr(body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(1)))), catches=listOf(TryExpr.Catch(local="e", type="Throwable".userType, body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(2)))))), finally=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(3))))),
            ValentiaParser.expression("try { 1 } catch (e: Throwable) { 2 } finally { 3 }")
        )
        assertEquals(
            TryExpr(body=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(1)))), finally=Stms(stms=listOf(ExprStm(expr=IntLiteralExpr(2))))),
            ValentiaParser.expression("try { 1 } finally { 2 }")
        )
    }

    @Test
    fun testIfElseExpression() {
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody = EmptyStm()),
            ValentiaParser.expression("if (true) ;")
        )
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody = ExprStm(expr=IntLiteralExpr(1))),
            ValentiaParser.expression("if (true) 1")
        )
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody=ExprStm(expr=IntLiteralExpr(1)), falseBody=ExprStm(expr=IntLiteralExpr(2))),
            ValentiaParser.expression("""
                if (true) 1 else 2
            """) as? Any?
        )
        assertEquals(
            IfExpr(cond=BoolLiteralExpr(value=true), trueBody=ExprStm(expr=IntLiteralExpr(1)), falseBody=ExprStm(expr=IntLiteralExpr(2))),
            ValentiaParser.expression("""
                if (true) 1; else 2;
            """) as? Any?
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
    fun testSimpleClass() {
        assertEquals(
            ClassDecl(kind = "class", name = "Test"),
            ValentiaParser("""
                class Test {
                    fun demo() { println("1") }
                }
            """.trimIndent()).topLevelObject()
        )
    }

    @Test
    fun testReturn() {
        assertEquals(
            RETURN(1.lit),
            ValentiaParser.expression("return 1")
        )
    }

    @Test
    fun testReturnAt() {
        assertEquals(
            RETURN(1.lit, label = "test"),
            ValentiaParser.expression("return@test 1")
        )
    }

    @Test
    fun testSimpleFunction() {
        assertEquals(
            //"println".id("Hello World!".lit),
            FUN("sum", IntType, "a" to IntType, "b" to IntType) {
                RETURN("a".id + "b".id)
            },
            ValentiaParser.topLevelDecl("fun sum(a: Int, b: Int): Int { return a + b }")
        )
    }

    @Test
    fun testPrintHelloWorld() {
        assertEquals(
            "println".id("Hello World!".lit),
            ValentiaParser.expression("println(\"Hello World!\")")
        )
    }

    @Test
    fun testSimplestFunctionCall() {
        assertEquals(
            "exit".id(),
            ValentiaParser.expression("exit()")
        )
    }

    @Test
    fun testSimplerFunctionCall() {
        assertEquals(
            "exit".id(-(1).lit),
            ValentiaParser.expression("exit(-1)")
        )
    }

    @Test
    fun testSimpleFunctionCall() {
        assertEquals(
            "max".id(12.lit, 34.lit),
            ValentiaParser.expression("max(12, 34)")
        )
    }

    @Test
    fun testShebang() {
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser.file("#!/bin/sh -x\n"))
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser.file("#!/bin/sh -x"))
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

    @Test
    fun testSimplestWhile() {
        assertEquals(
            WHILE(1.lit) { },
            ValentiaParser.statement("while (1) ;")
        )
    }

    @Test
    fun testSimplestFor() {
        assertEquals(
            ForLoopStm(IntLiteralExpr(1), vardecl = VariableDecls(VariableDecl(id = "n", type = null)), body = null),
            ValentiaParser.statement("for (n in 1) ;")
        )
    }
}