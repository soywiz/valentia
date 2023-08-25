package valentia.cli

import valentia.ExternalInterface
import valentia.Valentia
import valentia.compiler.ValentiaCompiler
import valentia.util.LocalFile

object ValentiaCli {
    fun main(args: Array<String>) {
        val items = args.toMutableList()
        if (items.isEmpty()) {
            items.add("--help")
        }
        loop@while (items.isNotEmpty()) {
            val item = items.removeFirst()
            when (item) {
                "version", "-v", "-version", "--version" -> {
                    println("Valentia ${Valentia.VERSION}")
                    return
                }
                "help", "-h", "-help", "--help" -> {
                    println("Valentia ${Valentia.VERSION} - soywiz - 2023")
                    println("")
                    println("valentia <command> <args>")
                    println("")
                    println("  [source] : <file.kt>/<folder>/<valentia.toml>")
                    println("")
                    println("  version  - Displays version")
                    println("  help     - Shows this help")
                    println("  run       [source]   - Compiles & runs generated code with Deno")
                    println("  compile   [source] > [output.js]  - Generates JS code")
                    println("  gradle    [source]   - Creates a gradle project to use existing tools for editing")
                    println("  edit      [source]   - Creates a gradle project and open `idea` to use existing tools for editing")
                    println("  new       [source]   - Creates a new valentia project")
                    println("")
                    println("Example:")
                    println("")
                    println("  valentia run file.kt")
                    println("  valentia compile file.kt > output.js")
                    println("")
                    return
                }
                "run", "-r", "-run", "--run" -> {
                    // valentia.toml
                    // .kt files
                    // .vlm VaLentia Module
                    val files = items.toList().takeIf { it.isNotEmpty() } ?: listOf(".")
                    items.clear()
                    val finalFiles = files.flatMap {
                        val file = LocalFile(it)
                        if (file.isDirectory) {
                            file.listRecursively().filter { it.baseName.endsWith(".kt") }.toList()
                        } else {
                            listOf(file)
                        }
                    }
                    ValentiaCompiler.compileAndRun(finalFiles.map { it.fullPath })
                }
                "compile", "-c", "-compile", "--compile" -> {
                    val files = items.toList()
                    items.clear()
                    if (files.isEmpty()) error("Missing file/s to compile")
                    println(ValentiaCompiler.compile(files))
                }
                "new" -> {
                    val folder = LocalFile(items.removeFirstOrNull() ?: error("Folder not specified"))
                    val stat = folder.statOpt()
                    if (stat != null && !stat.isDir) error("Exists and not a directory '$folder'")
                    if (stat != null && folder.list().isNotEmpty()) error("Folder is not empty '$folder'")
                    folder.mkdirs()
                    folder[".gitignore"].writeString("""
                        .idea/
                        build/
                        .gradle/
                        gradle/
                        build.gradle.kts
                        settings.gradle.kts
                    """.trimIndent())
                    folder["valentia.toml"].writeString("""
                        name = "${folder.cannonical.baseName}"
                        authors = ["dummy"]

                        [dependencies]
                    """.trimIndent())
                    folder["src"].mkdirs()
                    folder["test"].mkdirs()
                    folder["src"]["main.kt"].writeString("""
                        fun main() {
                            println("Hello World!")
                        }
                    """.trimIndent())
                }
                "gradle" -> {
                    val folder = LocalFile(items.removeFirstOrNull() ?: ".")
                    createGradleProject(folder)

                }
                "edit", "-e", "-edit", "--edit" -> {
                    val folder = LocalFile(items.removeFirstOrNull() ?: ".")
                    createGradleProject(folder)
                    ExternalInterface.exec("idea", folder.fullPath)
                }
                else -> {
                    error("Unknown command $item")
                }
            }
        }
    }

    private fun createGradleProject(folder: LocalFile) {
        folder["settings.gradle.kts"].writeString("""
            //rootProject.name = "${folder.cannonical.baseName}"
        """.trimIndent())
        folder["build.gradle.kts"].writeString("""
            plugins {
                kotlin("multiplatform") version "1.9.10"
                application
            }
                                    
            repositories {
                mavenCentral()
            }
            
            java {
                targetCompatibility = JavaVersion.VERSION_1_8
                sourceCompatibility = JavaVersion.VERSION_1_8
            }
            
            kotlin {
                jvm {
                    compilations.all {
                        kotlinOptions.jvmTarget = "1.8"
                    }
                    withJava()
                    testRuns["test"].executionTask.configure {
                        useJUnitPlatform()
                    }
                }
                js(IR) {
                    binaries.executable()
                    browser { }
                }
                sourceSets {
                    val commonMain by getting
                    val commonTest by getting {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }
                    val jvmMain by getting {
                        dependencies {
                        }
                    }
                    val jvmTest by getting {
                        dependencies {
                        }
                    }
                    val jsMain by getting {
                        dependencies {
                        }
                    }
                    val jsTest by getting
                    
                    sourceSets {
                        this.maybeCreate("commonMain").kotlin.setSrcDirs(listOf("src"))
                        this.maybeCreate("commonMain").resources.setSrcDirs(listOf("resources"))
                        this.maybeCreate("commonTest").kotlin.setSrcDirs(listOf("test"))
                        this.maybeCreate("commonTest").resources.setSrcDirs(listOf("testresources"))
                    }
                }
            }
        """.trimIndent())
    }
}