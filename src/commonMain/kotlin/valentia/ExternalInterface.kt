package valentia

open class ExternalInterfaceBase {
    open fun exec(vararg params: String, cwd: String = "."): ExecResult = TODO()
    open val ENVS: Map<String, String> = emptyMap()

    val ENVS_UPPERCASED by lazy { ENVS.mapKeys { it.key.uppercase() } }
    val TEMP get() = env("tmp") ?: env("temp") ?: "/tmp"
    fun env(key: String): String? = ENVS_UPPERCASED[key.uppercase()]

    open fun fileMkdir(path: String): Boolean = TODO("fileMkdir")
    open fun fileStat(path: String): StatResult? = TODO("fileStat")
    open fun fileWrite(path: String, content: ByteArray): Unit = TODO("fileWrite")
    open fun fileRead(path: String): ByteArray = TODO("fileRead")
    open fun fileList(path: String): List<String> = TODO("fileList")
    open fun fileListStat(path: String): List<StatResult> = TODO("fileListStat")

    open fun fileExists(path: String): Boolean = fileStat(path) != null
    fun fileWriteString(path: String, content: String): Unit = fileWrite(path, content.encodeToByteArray())
    fun fileReadString(path: String): String = fileRead(path).decodeToString()
    open fun makeExecutable(path: String): Unit = Unit
}

expect object ExternalInterface : ExternalInterfaceBase
data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)
data class StatResult(val name: String, val size: Long, val isDir: Boolean) {
    val isFile: Boolean get() = !isDir
}
