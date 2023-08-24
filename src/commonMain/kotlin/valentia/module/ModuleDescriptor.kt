package valentia.module

// valentia.toml
// [dependencies]
// korlibs.korge.kds = "4.0.0"

class ModuleDescriptor(
    val name: String,
    val license: String,
    val authors: List<String>,
    val dependencies: ModuleDependency
)

class ModuleDependency(val str: String)
