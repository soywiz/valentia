package valentia.parser

import valentia.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValentiaParserShebangTest : StmBuilder {
    @Test
    fun testShebang() {
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser.file("#!/bin/sh -x\n"))
        assertEquals(FileNode(shebang = "#!/bin/sh -x"), ValentiaParser.file("#!/bin/sh -x"))
    }
}