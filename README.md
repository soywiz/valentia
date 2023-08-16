# Valentia Compiler

Valentia (spelled Valentía in Spanish) is a Kotlin Compiler fully written in Kotlin,
that aims to generate JS and in the future C++/WASM code, being able to compile itself.

It is designed to generate dependency-less JS Deno/Browser code and in the future C++ or WASM code too.
It can be embedded as library and also allows compiling code in the browser.

* [x] Step 1: Parsing
* [x] Use a tokenizer to make it faster and simpler
* [ ] `[WIP]` Step 2: Compilation into modern JS: classes, generators for coroutines, etc.
* [ ] Step 2a: Be able to compile itself and to run in Deno, Node.JS and the Browser
* [ ] Step 3: Compilation into WASM or C++? If WASM 2 C already exists we could skip C++
* [ ] Step 4: Recovery, code completion, LSP?
* [ ] Step 5: New features: static if, Numeric coercion, etc.
* [ ] Extra: Support for source-code-based plugins for AST transformation and code generation
* [ ] Extra: API support for passing the AST to functions like C# LINQ

It can work on the browser, Deno and Node.JS,
It skips the JVM, Android and Kotlin/Native targets
and focuses on JS and eventually other source-code level targets.
