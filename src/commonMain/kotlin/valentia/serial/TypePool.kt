package valentia.serial

import valentia.ast.*
import valentia.util.BinaryReader
import valentia.util.BinaryWriter
import valentia.util.binaryReader
import valentia.util.buildByteArray

class TypePool(val stringPool: StringPool = StringPool()) : Pool<Type?>() {
    val typePool = this

    init {
        this[null]
    }

    fun toByteArray(): ByteArray = buildByteArray {
        val stringPoolBytes = stringPool.toByteArray()
        writeBytesWithLength(stringPoolBytes)
        writeIntVLQ(items.size)
        for (type in items) {
            writeType(type)
        }
    }

    override fun beforeAddingEntry(item: Type?) {
        val type = item
        when (type) {
            null -> Unit
            is SimpleType -> stringPool[type.fullName]
            is NullableType -> this[type.type]
            is GenericType -> {
                this[type.base]
                for (gen in type.generics) this[gen]
            }
            is FuncType -> {
                this[type.receiver]
                this[type.ret]
                for (param in type.params) {
                    stringPool[param.name]
                    this[param.type]
                }
            }
            else -> TODO("item=$type")
        }
    }

    private fun getStringIndex(str: String?): Int {
        return stringPool.getOrThrow(str)
    }

    private fun getTypeIndex(type: Type?, currentIndex: Int): Int {
        return typePool.getOrThrow(type).also { check(it < currentIndex) { "$it < $currentIndex" } }
    }

    fun BinaryWriter.writeType(type: Type?) {
        val index = typePool.getOrThrow(type)
        when (type) {
            null -> writeIntVLQ(0)
            is SimpleType -> {
                writeIntVLQ(SimpleType.ID)
                writeIntVLQ(getStringIndex(type.fullName))
            }
            is NullableType -> {
                writeIntVLQ(NullableType.ID)
                writeIntVLQ(getTypeIndex(type.type, index))
            }
            is GenericType -> {
                writeIntVLQ(GenericType.ID)
                writeIntVLQ(getTypeIndex(type.base, index))
                writeIntVLQ(type.generics.size)
                for (generic in type.generics) writeIntVLQ(getTypeIndex(generic, index))
            }
            is FuncType -> {
                writeIntVLQ(FuncType.ID)
                writeIntVLQ(type.flags)
                writeIntVLQ(getTypeIndex(type.receiver, index))
                writeIntVLQ(getTypeIndex(type.ret, index))
                for (param in type.params) {
                    writeIntVLQ(getStringIndex(param.name))
                    writeIntVLQ(getTypeIndex(param.type, index))
                }
            }
            else -> TODO("type=$type")
        }
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): TypePool {
            val reader = bytes.binaryReader()
            val pool = TypePool(StringPool.fromByteArray(reader.readBytesWithLength()))
            for (n in 0 until reader.readIntVLQ()) {
                reader.readType(pool).also { pool[it] }
            }
            return pool
        }

        fun BinaryReader.readType(typePool: TypePool): Type? {
            val type = readIntVLQ()
            return when (type) {
                0 -> null
                SimpleType.ID -> SimpleType(typePool.stringPool[readIntVLQ()]!!)
                NullableType.ID -> NullableType(typePool[readIntVLQ()]!!)
                GenericType.ID -> GenericType(typePool[readIntVLQ()]!!, (0 until readIntVLQ()).map { typePool[readIntVLQ()]!! })
                else -> TODO("type=$type")
            }
        }
    }
}
