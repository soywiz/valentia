package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserLambdaLiteralExprTest : StmBuilder {
    @Test
    fun testAnonymousFunctionLiteral1() {
        assertEquals(
            DeclStm(decl= VariableDecl(id="a", expr=AnonymousFunctionExpr(decl=FunDecl(name="", params=listOf(FuncValueParam(id="a", type=IntType)), body=ReturnStm(10.lit))))),
            ValentiaParser.statement("val a = fun(a: Int) = 10") as Any
        )
    }
}