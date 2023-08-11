package valentia

@JsName("require")
external fun jsRequire(str: String): dynamic

@JsName("Object")
external object JsObject {
    fun keys(obj: dynamic): Array<String>?
}

fun jsObjectToMap(obj: dynamic): Map<String, dynamic> {
    val out = LinkedHashMap<String, dynamic>()
    val keys = JsObject.keys(obj) ?: return emptyMap()
    for (key in keys) out[key] = obj[key]
    return out
}

fun jsObject(vararg params: Pair<Any?, Any?>): dynamic {
    val out = js("({})")
    for ((key, value) in params) out[key] = value
    return out
}

actual object ExternalInterface : ExternalInterfaceBase() {
    override val ENVS: Map<String, String> by lazy {
        jsObjectToMap(js("(process.env)"))
    }

    override fun exec(vararg params: String, cwd: String): ExecResult {
        val result = jsRequire("child_process").spawnSync(
            params.first(), params.drop(1).toTypedArray(),
            jsObject(
                "cwd" to cwd,
                "encoding" to "utf-8",
            )
        )
        val exitCode = result.status ?: -1
        return ExecResult(exitCode, result.stdout.toString(), result.stderr.toString())
    }

    override fun fileMkdir(path: String): Boolean {
        jsRequire("fs").mkdirSync(path)
        return true
    }

    override fun fileExists(path: String): Boolean {
        return jsRequire("fs").existsSync(path)
    }

    override fun fileWrite(path: String, content: ByteArray) {
        jsRequire("fs").writeFileSync(path, content)
    }

    override fun fileRead(path: String): ByteArray {
        return jsRequire("fs").readFileSync(path)
    }
}