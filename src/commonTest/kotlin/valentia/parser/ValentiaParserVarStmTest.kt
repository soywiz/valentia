package valentia.parser

import valentia.ast.*
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

    @Test
    fun testVarLazyDelegatedDecl() {
        assertEquals(
            VAR("a", expr = "lazy".id(lambdaArg = LAMBDA { STM(1.lit) }), delegation = true),
            ValentiaParser.topLevelDecl("var a by lazy { 1 }") as Any
        )
    }

    @Test
    fun testVarLazyDelegatedStatement() {
        assertEquals(
            STM(VAR("a", expr = CallExpr("lazy".id, lambdaArg = LAMBDA { STM(1.lit) }), delegation = true)),
            ValentiaParser.statement("var a by lazy { 1 }") as Any
        )
    }

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
            STM(
                MultiVariableDecl(VariableDecl("a"), VariableDecl("b", IntType), expr = 10.lit.infix("to", 20.lit))
            ),
            ValentiaParser.statement("var (a, b: Int) = 10 to 20") as? Any?
        )
    }

    @Test
    fun testDelegated1() {
        assertEquals(
            FILE {
                 CLASS("NumberToken", ConstructorInvocation("Token".type, "number".id), data = true) {
                     VAL("numberCleanedUp", type = StringType, expr = "lazy".id(lambdaArg = LAMBDA { STM("".lit) }), delegation = true)
                 }
            },
            ValentiaParser.file("""
                data class NumberToken(val number: String) : Token(number) {
                    val numberCleanedUp: String by lazy { "" }
                }
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun test2() {
        //assertEquals(
        //    STM(VAL("falseBody", IF("expectOpt".id("else".lit)) {
        //
        //    })),
        //    ValentiaParser.statement("""
        //        val falseBody = if (expectOpt("else")) {
        //            opt { controlStructureBody() }.also {
        //                NLs()
        //                opt { SEMICOLON() }
        //                NLs()
        //            }
        //        } else {
        //            null
        //        }
        //    """.trimIndent()) as? Any?
        //)

        ValentiaParser.statement("""
            val falseBody = if (expectOpt("else")) {
                opt { controlStructureBody() }.also {
                    NLs()
                    opt { SEMICOLON() }
                    NLs()
                }
            } else {
                null
            }
        """.trimIndent())
    }
}