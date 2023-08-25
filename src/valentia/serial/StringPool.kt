package valentia.serial

import valentia.util.BinaryReader
import valentia.util.buildByteArray

class StringPool : Pool<String?>() {
    init {
        this[null]
    }

    fun toByteArray(): ByteArray = buildByteArray(7 + items.sumOf { (it ?: "").length * 2 }) {
        writeIntVLQ(items.size)
        for (str in items) writeStringNullable(str)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): StringPool {
            val reader = BinaryReader(bytes)
            val count = reader.readIntVLQ()
            val pool = StringPool()
            for (n in 0 until count) { pool[reader.readStringNullable()] }
            return pool
        }
    }
}

// @TODO: This could be used for example for FqNames that will tend to have lots of repeated prefixes

//class PrefixedStringPool {
//    data class PrefixedString(val prefix: String, val suffix: String) {
//        val string: String = "$prefix$suffix"
//    }
//    val prefixes = arrayListOf<Int>()
//    val strings = arrayListOf<String>()
//    val stringsToIndex = LinkedHashMap<String, Int>()
//
//    fun get(prefixed: PrefixedString): Int {
//        stringsToIndex[prefixed.string]?.let { return it }
//        val base = stringsToIndex[prefixed.prefix] ?: -1
//        prefixes += base
//        strings.add(prefixed)
//    }
//}
