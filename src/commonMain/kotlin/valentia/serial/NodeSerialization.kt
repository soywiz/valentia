package valentia.serial

import valentia.util.BinaryReader
import valentia.util.BinaryWriter

interface NodeSerializationCommon {
    companion object {
        val MAGIC = "ValentiA"
        const val VERSION = 1
    }
}

open class NodeSerializer : BinaryWriter(), NodeSerializationCommon {
    val stringPool = StringPool()
    val typePool = TypePool(stringPool)


    fun writeHeader() {
        writeFixedSizeString(NodeSerializationCommon.MAGIC)
        writeIntVLQ(NodeSerializationCommon.VERSION)
    }

    fun writeStringPool() {

    }
}

open class NodeDeserializer(data: ByteArray) : BinaryReader(data), NodeSerializationCommon {
    fun readHeader() {
        val magic = readFixedSizeString(NodeSerializationCommon.MAGIC.length)
        check(magic == NodeSerializationCommon.MAGIC) { "Header '$magic'" }
        val version = readIntVLQ()
        check(version == NodeSerializationCommon.VERSION) { "Header version '$version'" }
    }
}

enum class NodeKind {
    MODULE, PACKAGE, FILE, CLASS,
}
