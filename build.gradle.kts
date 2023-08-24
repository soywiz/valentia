plugins {
    kotlin("multiplatform") version "1.9.10"
    application
}

group = "com.soywiz.valentia"
version = "0.1-SNAPSHOT"

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
        //nodejs {
        //    testTask {
        //        //environment("key", "value")
        //    }
        //}
        browser {
        //    commonWebpackConfig {
        //        cssSupport {
        //            enabled.set(true)
        //        }
        //    }
        }
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
                api("org.antlr:antlr4-runtime:4.13.0")
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
        val jsTest by getting
    }
}

application {
    mainClass.set("valentia.MainKt")
    //println(System.getProperty("user.dir"))
}

val run = tasks.getByName<JavaExec>("run")
run.workingDir = File(System.getProperty("user.dir"))
//println(tasks.getByName("run")!!::class)

//println(System.getProperty("user.dir"))

//tasks.named<Copy>("jvmProcessResources") {
//    val jsBrowserDistribution = tasks.named("jsBrowserDistribution")
//    from(jsBrowserDistribution)
//}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        //freeCompilerArgs += listOf("-Xreport-perf", "-Xprofile=")
        //freeCompilerArgs += listOf("-Xreport-perf")
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = application.mainClass
    }
    from({ configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) } })
    with(tasks["jar"] as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}