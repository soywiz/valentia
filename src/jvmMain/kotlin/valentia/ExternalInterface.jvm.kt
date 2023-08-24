package valentia

import java.io.File

actual object ExternalInterface : ExternalInterfaceBase() {
    override val ENVS: Map<String, String> = System.getenv()

    override fun exec(vararg params: String, cwd: String): ExecResult {
        val process = ProcessBuilder(params.toList())
            .directory(File(cwd))
            .start()
        val stdout = process.inputStream.readBytes().decodeToString()
        val stderr = process.errorStream.readBytes().decodeToString()
        val exitCode = process.waitFor()
        return ExecResult(exitCode, stdout, stderr)
    }

    override fun fileMkdir(path: String): Boolean = File(path).mkdirs()
    override fun fileExists(path: String): Boolean = File(path).exists()
    override fun fileWrite(path: String, content: ByteArray) = File(path).writeBytes(content)
    override fun fileRead(path: String): ByteArray = File(path).readBytes()
    override fun fileList(path: String): List<String> = (File(path).listFiles() ?: emptyArray()).map { it.name }

    override fun canonical(path: String): String = File(path).canonicalPath

    override fun fileStat(path: String): StatResult? {
        val file = File(path).takeIf { it.exists() } ?: return null
        return StatResult(file.name, file.length(), file.isDirectory)
    }

    override fun makeExecutable(path: String) {
        File(path).setExecutable(true)
    }
}