package valentia.parser

import valentia.ast.MultiVariableDecl
import valentia.ast.StmBuilder
import valentia.ast.VariableDecl
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserVarStmTest : StmBuilder {
    @Test
    fun testVarDelegatedStatement() {
        assertEquals(
            STM(VariableDecl("a", expr = 1.lit, delegation = true)),
            ValentiaParser.statement("var a by 1") as Any
        )
    }

    //@Test
    //fun testVarLazyDelegatedStatement() {
    //    assertEquals(
    //        STM(VariableDecl("a", expr = 1.lit, delegation = true)),
    //        ValentiaParser.statement("var a by lazy { 1 }") as Any
    //    )
    //}

    @Test
    fun testVarStatement() {
        assertEquals(
            STM(VariableDecl("a")),
            ValentiaParser.statement("var a") as Any
        )
    }

    @Test
    fun testMultiVarStatement() {
        assertEquals(
            STM(MultiVariableDecl(VariableDecl("a"), VariableDecl("b"))),
            ValentiaParser.statement("var (a, b)") as Any
        )
    }

    @Test
    fun testVarTypeStatement() {
        assertEquals(
            STM(VariableDecl("a", IntType)),
            ValentiaParser.statement("var a: Int") as Any
        )
    }

    @Test
    fun testVarWithAssignmentStatement() {
        assertEquals(
            STM(VariableDecl("a", expr = 10.lit)),
            ValentiaParser.statement("var a = 10") as Any
        )
    }

    @Test
    fun testVarTypeWithAssignmentStatement() {
        assertEquals(
            STM(VariableDecl("a", IntType, expr = 10.lit)),
            ValentiaParser.statement("var a: Int = 10") as Any
        )
    }

    @Test
    fun testDestructuring() {
        assertEquals(
            null,
            ValentiaParser.statement("var (a, b: Int) = 10 to 20") as? Any?
        )
    }
}