package valentia

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLTextAreaElement
import valentia.cli.ValentiaCli
import valentia.compiler.FileWithContents
import valentia.compiler.ValentiaCompiler

actual fun main(args: Array<String>) {
    val button = document.getElementById("compile-button") as? HTMLButtonElement?
    if (button != null) {
        button.onclick = {
            val inputField = document.querySelector("#compile-text-kotlin") as? HTMLTextAreaElement?
            val outputField = document.querySelector("#compile-text-js") as? HTMLTextAreaElement?
            inputField?.value?.let {
                val result = ValentiaCompiler.compile(listOf(FileWithContents(it)))
                outputField?.value = result.jsString
            }
            Unit
        }
    } else {
        ValentiaCli.main(args)
    }
}