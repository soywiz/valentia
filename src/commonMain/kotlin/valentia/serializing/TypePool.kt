package valentia.serializing

import valentia.ast.GenericType
import valentia.ast.SimpleType
import valentia.ast.TypeNode
import valentia.util.BinaryReader
import valentia.util.BinaryWriter
import valentia.util.binaryReader
import valentia.util.buildByteArray

class TypePool(val stringPool: StringPool = StringPool()) : Pool<TypeNode>() {
    fun toByteArray(): ByteArray = buildByteArray {
        writeIntVLQ(items.size)
        for (type in items) {
            writeType(stringPool, type)
        }
    }

    companion object {
        fun fromByteArray(bytes: ByteArray, stringPool: StringPool): TypePool {
            val pool = TypePool(stringPool)
            val reader = bytes.binaryReader()
            val types = (0 until reader.readIntVLQ()).map { reader.readType(stringPool) }
            for (type in types) pool[type]
            return pool
        }

        fun BinaryWriter.writeType(stringPool: StringPool, type: TypeNode) {
            when (type) {
                is SimpleType -> {
                    writeIntVLQ(SimpleType.ID)
                    writeIntVLQ(stringPool[type.fqname ?: type.name])
                }
                is GenericType -> {
                    writeIntVLQ(GenericType.ID)
                    writeType(stringPool, type.base)
                    writeIntVLQ(type.generics.size)
                    for (gen in type.generics) writeType(stringPool, gen)
                }
                else -> TODO("type=$type")
            }
        }

        fun BinaryReader.readType(stringPool: StringPool): TypeNode {
            val type = readIntVLQ()
            return when (type) {
                SimpleType.ID -> SimpleType(stringPool[readIntVLQ()])
                GenericType.ID -> GenericType(readType(stringPool), (0 until readIntVLQ()).map { readType(stringPool) })
                else -> TODO("type=$type")
            }
        }
    }
}
