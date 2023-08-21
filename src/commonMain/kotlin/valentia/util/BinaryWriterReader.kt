package valentia.util

import kotlin.math.max
import kotlin.math.min

fun buildByteArray(initialCapacity: Int = 74, block: BinaryWriter.() -> Unit): ByteArray =
    BinaryWriter(initialCapacity).also(block).toByteArray()

open class BinaryWriter(initialCapacity: Int = 74) {
    var buffer = ByteArray(initialCapacity)
    val capacity: Int get() = buffer.size
    var length: Int = 0
        private set
    private val availableWrite get() = capacity - length
    private fun ensure(len: Int): Int {
        if (len > availableWrite) {
            buffer = buffer.copyOf(max(length + availableWrite + 7, (capacity * 2.5).toInt()))
        }
        return length.also {
            length += len
        }
    }
    fun writeBytes(data: ByteArray) = writeBytes(data, 0, data.size)
    fun writeBytes(data: ByteArray, offset: Int, len: Int) {
        data.copyInto(this.buffer, ensure(len), offset, offset + len)
    }

    fun writeByte(data: Byte) {
        buffer[ensure(1)] = data
    }

    fun writeBytes(d1: Byte, d2: Byte) {
        val p = ensure(2)
        buffer[p + 0] = d1
        buffer[p + 1] = d2
    }

    fun writeBytes(d1: Byte, d2: Byte, d3: Byte, d4: Byte) {
        val p = ensure(4)
        buffer[p + 0] = d1
        buffer[p + 1] = d2
        buffer[p + 2] = d3
        buffer[p + 3] = d4
    }

    fun writeIntVLQ(v: Int) {
        var v = v
        while (true) {
            var payload = v and 0x7F
            v = v ushr 7
            if (v != 0) payload = payload or 0x80
            writeByte(payload.toByte())
            if (v == 0) break
        }
    }

    fun writeStringNullable(str: String?) {
        if (str == null) {
            writeIntVLQ(0)
            return
        } else {
            writeString(str)
        }
    }

    fun writeString(str: String) {
        val bytes = str.encodeToByteArray()
        writeIntVLQ(1 + bytes.size)
        writeBytes(bytes)
    }
    fun writeStringz(str: String) {
        writeBytes(str.encodeToByteArray())
        writeByte(0)
    }
    fun writeFixedSizeString(str: String) {
        writeBytes(str.encodeToByteArray())
    }

    fun toByteArray(): ByteArray = buffer.copyOf(length)
}

open class BinaryReader(val data: ByteArray, var position: Int = 0, val length: Int  = data.size) {
    val eof: Boolean get() = position >= length
    val hasMore: Boolean get() = !eof

    fun readStringNullable(): String? {
        val len = readIntVLQ() - 1
        if (len < 0) return null
        return readBytes(len).decodeToString()
    }
    fun readString(): String = readStringNullable()!!

    fun readIntVLQ(): Int {
        var out = 0
        var offset = 0
        while (true) {
            val v = readByte()
            out = out or ((v and 0x7F) shl offset)
            offset += 7
            if ((v and 0x80) == 0) break
        }
        return out
    }
    fun readByte(): Int = data[position++].toInt() and 0xFF
    fun readBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        val len = readBytes(bytes)
        return if (len == bytes.size) bytes else bytes.copyOf(len)
    }
    fun readBytes(out: ByteArray): Int = readBytes(out, 0, out.size)
    fun readBytes(out: ByteArray, offset: Int, len: Int): Int {
        val available = length - position
        val rlen = min(available, len)
        this.data.copyInto(out, offset, this.position, this.position + rlen)
        this.position += rlen
        return rlen
    }
    fun readFixedSizeString(len: Int): String {
        return readBytes(len).decodeToString()
    }

    fun readStringz(): String {
        val out = BinaryWriter(64)
        while (hasMore) {
            val b = readByte()
            if (b == 0) break
            out.writeByte(b.toByte())
        }
        return out.toByteArray().decodeToString()
    }
}

fun ByteArray.binaryReader(): BinaryReader = BinaryReader(this)
