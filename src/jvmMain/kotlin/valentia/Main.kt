package valentia

import valentia.cli.ValentiaCli

actual fun main(args: Array<String>) {
    try {
        ValentiaCli.main(args)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}