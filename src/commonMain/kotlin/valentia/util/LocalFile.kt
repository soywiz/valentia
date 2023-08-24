package valentia.util

import valentia.ExternalInterface
import valentia.StatResult

data class LocalFile(val path: String) {
    override fun toString(): String = "LocalFile($path)"
    fun statOpt(): StatResult? = ExternalInterface.fileStat(path)
    fun stat(): StatResult = statOpt() ?: error("Can't find file '$path'")
    fun length(): Long = stat().size
    fun exists(): Boolean = ExternalInterface.fileExists(path)
    fun readString(): String = ExternalInterface.fileReadString(path)
    fun readBytes(): ByteArray = ExternalInterface.fileRead(path)
    fun writeBytes(content: ByteArray) = ExternalInterface.fileWrite(path, content)
    fun writeString(content: String) = ExternalInterface.fileWriteString(path, content)
}