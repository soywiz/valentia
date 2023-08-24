package valentia.sema

import valentia.ast.*
import valentia.compiler.FileWithContents
import valentia.compiler.ValentiaCompiler
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeResolverTest {
    @Test
    fun testDirectImport() {
        testBase("""
            package demo
            import demo2.BaseTest
            class Test : BaseTest()
        """.trimIndent())
    }

    @Test
    fun testAsteriskImport() {
        testBase("""
            package demo
            import demo2.*
            class Test : BaseTest()
        """.trimIndent())
    }

    @Test
    fun testSamePackage() {
        testBase("""
            package demo2
            class Test : BaseTest()
        """.trimIndent(), pack = "demo2")
    }

    @Test
    fun testSameFile() {
        testBase("""
            package demo
            class Test : BaseTest()
            class BaseTest
        """.trimIndent())
    }

    @Test
    fun testInnerClass() {
        testBase("""
            package demo
            class Test : BaseTest() {
                class BaseTest
            }
        """.trimIndent())
    }

    fun testBase(str: String, pack: String = "demo") {
        val result = ValentiaCompiler.compile(listOf(
            FileWithContents(str),
            FileWithContents("""
                package demo2
                class BaseTest
            """.trimIndent()),
        ))
        val module = result.program.getModule(null)
        val test = module.getPackage(Identifier(pack)).symbols["Test"]?.first() as ClassDecl
        val parentType = test.subTypes!!.first().type
        val resolvedDecl = (parentType as SimpleType).resolveSimpleType()
        assertEquals("BaseTest", resolvedDecl.declName)
    }
}
