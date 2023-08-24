package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

open class ValentiaParserVarStmTest : StmBuilder {
    @Test
    fun testVarDelegatedStatement() {
        assertEquals(
            STM(VariableDecl("a", expr = 1.lit, delegation = true, kind = VariableKind.VAR)),
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
            STM(VAR("a", expr = "lazy".id(lambdaArg = LAMBDA { STM(1.lit) }), delegation = true)),
            ValentiaParser.statement("var a by lazy { 1 }") as Any
        )
    }

    @Test
    fun testVarStatement() {
        assertEquals(
            STM(VariableDecl("a", kind = VariableKind.VAR)),
            ValentiaParser.statement("var a") as Any
        )
    }

    @Test
    fun testMultiVarStatement() {
        assertEquals(
            STM(MultiVariableDecl(VariableDecl("a", kind = VariableKind.VAR), VariableDecl("b", kind = VariableKind.VAR), kind = VariableKind.VAR)),
            ValentiaParser.statement("var (a, b)") as Any
        )
    }

    @Test
    fun testVarTypeStatement() {
        assertEquals(
            STM(VariableDecl("a", IntType, kind = VariableKind.VAR)),
            ValentiaParser.statement("var a: Int") as Any
        )
    }

    @Test
    fun testVarWithAssignmentStatement() {
        assertEquals(
            STM(VariableDecl("a", expr = 10.lit, kind = VariableKind.VAR)),
            ValentiaParser.statement("var a = 10") as Any
        )
    }

    @Test
    fun testVarTypeWithAssignmentStatement() {
        assertEquals(
            STM(VariableDecl("a", IntType, expr = 10.lit, kind = VariableKind.VAR)),
            ValentiaParser.statement("var a: Int = 10") as Any
        )
    }

    @Test
    fun testDestructuring() {
        assertEquals(
            STM(
                MultiVariableDecl(VariableDecl("a", kind = VariableKind.VAR), VariableDecl("b", IntType, kind = VariableKind.VAR), expr = 10.lit.infix("to", 20.lit), kind = VariableKind.VAR)
            ),
            ValentiaParser.statement("var (a, b: Int) = 10 to 20") as? Any?
        )
    }

    @Test
    fun testDelegated1() {
        assertEquals(
            FILE {
                 CLASS("NumberToken", SubTypeInfo("Token".type, "number".id), data = true, primaryConstructorDecl = PrimaryConstructorDecl(
                     ClassParameter.VAL("number" to StringType),
                 ), modifiers = Modifiers(Modifier.DATA),
                 ) {
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

    @Test
    fun test3() {
        assertEquals(
            VariableDecl(
                "lit", type="BoolLiteralExpr".type,
                getter = FunDecl("get", body = ReturnStm("BoolLiteralExpr".id(THIS)))
            ),
            ValentiaParser.topLevelDecl("""
                val Boolean.lit: BoolLiteralExpr get() = BoolLiteralExpr(this)
            """.trimIndent()) as? Any?
        )
    }

    @Test
    fun test4() {
        assertEquals(
            VariableDecl("prop", expr = "WeakProperty".id(lambdaArg = LAMBDA { STM(0.lit) }), delegation = true, receiver = "C".type,
                modifiers = Modifiers(VisibilityModifier.PRIVATE), kind = VariableKind.VAR
            ),
            ValentiaParser.topLevelDecl("private var C.prop by WeakProperty { 0 }") as? Any?
        )
    }
}