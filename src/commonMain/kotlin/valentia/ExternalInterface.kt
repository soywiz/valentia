package valentia

open class ExternalInterfaceBase {
    open fun exec(vararg params: String, cwd: String = "."): ExecResult = TODO()
    open val ENVS: Map<String, String> = emptyMap()

    val ENVS_UPPERCASED by lazy { ENVS.mapKeys { it.key.uppercase() } }
    val TEMP get() = env("tmp") ?: env("temp") ?: "/tmp"
    fun env(key: String): String? = ENVS_UPPERCASED[key.uppercase()]

    open fun fileMkdir(path: String): Boolean = TODO()
    open fun fileExists(path: String): Boolean = TODO()
    open fun fileWrite(path: String, content: ByteArray): Unit = TODO()
    open fun fileRead(path: String): ByteArray = TODO()

    fun fileWriteString(path: String, content: String): Unit = fileWrite(path, content.encodeToByteArray())
    fun fileReadString(path: String): String = fileRead(path).decodeToString()
}

expect object ExternalInterface : ExternalInterfaceBase
data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)

