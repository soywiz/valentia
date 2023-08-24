package valentia.util

import valentia.ExternalInterface
import valentia.StatResult

data class LocalFile(val fullPath: String) {
    val path: String get() = fullPath.substringBeforeLast('/', "")
    val baseName: String get() = fullPath.substringAfterLast('/')

    val parent: LocalFile get() = LocalFile(path)
    val cannonical: LocalFile get() = LocalFile(ExternalInterface.canonical(path))
    operator fun get(child: String): LocalFile = LocalFile("$fullPath/$child")

    override fun toString(): String = "LocalFile($fullPath)"
    fun statOpt(): StatResult? = ExternalInterface.fileStat(fullPath)
    fun stat(): StatResult = statOpt() ?: error("Can't find file '$fullPath'")
    fun length(): Long = stat().size
    val isDirectory: Boolean get() = stat().isDir
    fun exists(): Boolean = ExternalInterface.fileExists(fullPath)
    fun readString(): String = ExternalInterface.fileReadString(fullPath)
    fun readBytes(): ByteArray = ExternalInterface.fileRead(fullPath)
    fun writeBytes(content: ByteArray) = ExternalInterface.fileWrite(fullPath, content)
    fun writeString(content: String) = ExternalInterface.fileWriteString(fullPath, content)
    fun mkdirs() = ExternalInterface.fileMkdir(fullPath)
    fun list(): List<LocalFile> = ExternalInterface.fileList(fullPath).map { LocalFile("$fullPath/$it") }
    fun listRecursively(): Sequence<LocalFile> = sequence {
        for (item in list()) {
            yield(item)
            if (item.isDirectory) {
                yieldAll(item.listRecursively())
            }
        }
    }
    //fun delete() = ExternalInterface.filerm(path)
}