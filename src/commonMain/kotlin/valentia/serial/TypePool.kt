package valentia.serial

import valentia.ast.*
import valentia.util.BinaryReader
import valentia.util.BinaryWriter
import valentia.util.binaryReader
import valentia.util.buildByteArray

class TypePool(val stringPool: StringPool = StringPool()) : Pool<TypeNode>() {
    val typePool = this

    fun toByteArray(): ByteArray = buildByteArray {
        writeIntVLQ(items.size)
        for (type in items) {
            writeType(type)
        }
    }

    override fun beforeAddingEntry(item: TypeNode) {
        val type = item
        when (type) {
            is SimpleType -> stringPool[type.fullName]
            is NullableType -> this[type.type]
            is GenericType -> {
                this[type.base]
                for (gen in type.generics) this[gen]
            }
            else -> TODO("item=$type")
        }
    }

    private fun getTypeIndex(type: TypeNode, currentIndex: Int = Int.MAX_VALUE): Int {
        return typePool.getOrThrow(type).also { check(it < currentIndex) { "$it < $currentIndex" } }
    }

    fun BinaryWriter.writeType(type: TypeNode) {
        val index = typePool.getOrThrow(type)
        when (type) {
            is SimpleType -> {
                writeIntVLQ(SimpleType.ID)
                writeIntVLQ(stringPool.getOrThrow(type.fullName))
            }
            is NullableType -> {
                writeIntVLQ(NullableType.ID)
                writeIntVLQ(getTypeIndex(type.type))
            }
            is GenericType -> {
                writeIntVLQ(GenericType.ID)
                writeIntVLQ(getTypeIndex(type.base))
                writeIntVLQ(type.generics.size)
                for (generic in type.generics) writeIntVLQ(getTypeIndex(generic))
            }
            else -> TODO("type=$type")
        }
    }

    companion object {
        fun fromByteArray(bytes: ByteArray, stringPool: StringPool): TypePool {
            val pool = TypePool(stringPool)
            val reader = bytes.binaryReader()
            for (n in 0 until reader.readIntVLQ()) {
                reader.readType(pool).also { pool[it] }
            }
            return pool
        }

        fun BinaryReader.readType(typePool: TypePool): TypeNode {
            val type = readIntVLQ()
            return when (type) {
                SimpleType.ID -> SimpleType(typePool.stringPool[readIntVLQ()])
                NullableType.ID -> NullableType(typePool[readIntVLQ()])
                GenericType.ID -> GenericType(typePool[readIntVLQ()], (0 until readIntVLQ()).map { typePool[readIntVLQ()] })
                else -> TODO("type=$type")
            }
        }
    }
}
