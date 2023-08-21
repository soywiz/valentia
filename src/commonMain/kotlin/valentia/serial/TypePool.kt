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

    private fun getTypeIndex(type: Type, currentIndex: Int): Int {
        return typePool.getOrThrow(type).also { check(it < currentIndex) { "$it < $currentIndex" } }
    }

    fun BinaryWriter.writeType(type: Type?) {
        val index = typePool.getOrThrow(type)
        when (type) {
            null -> writeIntVLQ(0)
            is SimpleType -> {
                writeIntVLQ(SimpleType.ID)
                writeIntVLQ(stringPool.getOrThrow(type.fullName))
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
