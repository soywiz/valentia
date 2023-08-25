package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserLambdaLiteralExprTest : StmBuilder {
    @Test
    fun testAnonymousFunctionLiteral1() {
        assertEquals(
            DeclStm(decl= VariableDecl(id="a", expr=AnonymousFunctionExpr(decl=FunDecl(name="", params=listOf(FuncValueParam(id="a", type=IntType)), body=FunctionBody(ReturnStm(10.lit)))))),
            ValentiaParser.statement("val a = fun(a: Int) = 10") as Any
        )
    }

    @Test
    fun testLambdaNoArgs() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA { STM(1.lit) })),
            ValentiaParser.statement("val a = { 1 }") as? Any?
        )
    }

    @Test
    fun testLambdaNoArgsEmpty() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA { })),
            ValentiaParser.statement("val a = { }") as? Any?
        )
    }

    @Test
    fun testLambdaArgs() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA(VariableDecl("a"), VariableDecl("b")) { STM(1.lit) })),
            ValentiaParser.statement("val a = { a, b -> 1 }") as? Any?
        )
    }

    @Test
    fun testLambdaArgsEmpty() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA(VariableDecl("a"), VariableDecl("b")) {  })),
            ValentiaParser.statement("val a = { a, b -> }") as? Any?
        )
    }

    @Test
    fun testLambdaDestructuring() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA(MultiVariableDecl(VariableDecl("a"), VariableDecl("b")), MultiVariableDecl(VariableDecl("c"), VariableDecl("d"))) { STM(1.lit) })),
            ValentiaParser.statement("val a = { (a, b), (c, d) -> 1 }") as? Any?
        )
    }

    @Test
    fun testLambdaDestructuringEmpty() {
        assertEquals(
            DeclStm(decl=VariableDecl(id="a", expr = LAMBDA(MultiVariableDecl(VariableDecl("a"), VariableDecl("b")), MultiVariableDecl(VariableDecl("c"), VariableDecl("d"))) { })),
            ValentiaParser.statement("val a = { (a, b), (c, d) -> }") as? Any?
        )
    }
}