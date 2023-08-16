# Valentia Compiler

Valentia (spelled Valentía in Spanish) is a Kotlin Compiler fully written in Kotlin,
that aims to generate JS and in the future C++/WASM code, being able to compile itself.

It is designed to generate dependency-less JS Deno/Browser code and in the future C++ or WASM code too.
The compiler can be embedded as library and also allows compiling code in the browser.

* [x] Step 1: Parsing
* [x] Use a tokenizer to make it faster and simpler
* [ ] `[WIP]` Step 2: Compilation into modern JS: classes, generators for coroutines, etc.
* [ ] Step 2a: Be able to compile itself and to run in Deno, Node.JS and the Browser
* [ ] Optimization: Add all binary operators on a single call to reduce recursion, then re-sort based on operator precedence
* [ ] Step 3: Compilation into WASM or C++? If WASM 2 C already exists we could skip C++
* [ ] Step 4: Recovery, code completion, LSP?
* [ ] Step 5: New features: static if, Numeric coercion, etc.
* [ ] Extra: Support for source-code-based plugins for AST transformation and code generation
* [ ] Extra: API support for passing the AST to functions like C# LINQ
* [ ] Extra: Reflection support: annotation, types, etc. for annotated elements.
* [ ] Project descriptor: Create project descriptor using TOML. For example cargo.toml
* [ ] No multiple folders for targets. Use annotations, static ifs, etc.. Just `src`, `test`, `resources` and `testresources` by default. For Kotlin compatibility it can use `src/commonMain` and `src/jsMain`
* [ ] Resources are copied along the generated `.js`/executable file.
* [ ] Support loading WASM modules in JS and C++/WASM by using wat2c? For example for image decoding: stb_image, minimp3, ffmpeg, etc.
* [ ] `valentia edit` creates a gradle project file setting up all the dependencies to edit the valentia project using existing tools

It can work on the browser, Deno and Node.JS,
It skips the JVM, Android and Kotlin/Native targets
and focuses on JS and eventually other source-code level targets like C++.

## Expected new scenarios

* Being able to compile on the browser, simplifying and allowing people to try and modify snippets directly in the browser without requiring a backend.
* Faster or equiparable compilation times while reducing the number of targets. Much faster on CI since it won't include K/N or Android targets.
* Not depending on the JVM.
* Simpler compiler plugins.
* Simplified tooling for projects.
* Hugely reduced download times: not gradle, not all the JVM, not all those libraries. Just a single Deno executable plus likely around 1MB or compiler and runtime.
* Reflection on all the targets, allowing to implement some scenarios without extra plugins.
* Hot reloading on JS.
* Being able to use the compiler as a Library, allowing Kotlin to be used as scripting on JS.
* Allow to use existing Kotlin/JS libraries from source.

## Sandbox ideas

### WASM interop

```kotlin
@WasmClass("sum.wasm")
external class MyModule {
    val memory: ByteArray
    fun sum(a: Int, b: Int): Int
}
```

```typescript
// AssemblyScript
// sudo npm -g install assemblyscript
// asc sum.asc.ts --outFile sum.wasm --optimize

export function sum(a: i32, b: i32): i32 {
  return a + b;
}
```

```typescript
const wasmCode = Deno.readFileSync("sum.wasm");
const wasmModule = new WebAssembly.Module(wasmCode);
const wasmInstance = new WebAssembly.Instance(wasmModule);
const sum = wasmInstance.exports.sum as CallableFunction;
console.log(sum(1, 2));
```

```js
class MyModule {
    // Alternatively embed here as base64
    #wasmFile = "sum.wasm";
    #wasmInstance = new WebAssembly.Instance(new WebAssembly.Module(Deno.readFileSync(this.#wasmFile)));
    #exports = this.#wasmInstance.exports;
    memory = this.#wasmInstance.memory;

    constructor() {
    }
    
    sum(a, b) {
        return this.#exports.sum(a, b);
    }
}
```

for example:

```js
function base64ToArrayBuffer(base64) {
    var binaryString = atob(base64);
    var bytes = new Uint8Array(binaryString.length);
    for (var i = 0; i < binaryString.length; i++) bytes[i] = binaryString.charCodeAt(i);
    return bytes.buffer;
}

class MyModule {
    #wasmContent = base64ToArrayBuffer("AGFzbQEAAAABBwFgAn9/AX8DAgEABAUBcAEBAQUDAQAABhQDfwBBCAt/AUGIgAILfwBBiIACCwcQAgNzdW0AAAZtZW1vcnkCAAkGAQBBAQsACgoBCAAgACABag8L");
    #wasmInstance = new WebAssembly.Instance(new WebAssembly.Module(this.#wasmContent));
    #exports = this.#wasmInstance.exports;
    memory = this.#wasmInstance.memory;
    
    sum(a, b) {
        return this.#exports.sum(a, b);
    }
}

console.log(new MyModule().sum(1, 2));
```

### Deno FFI

```typescript
#!/usr/bin/env -S deno run -A --allow-ffi --unstable

const SDL_QUIT           = 0x100;
const SDL_KEYDOWN        = 0x300;
const SDL_WINDOW_SHOWN = 0x00000004;
const SDL_MOUSEBUTTONDOWN = 0x401;

const libName = `/opt/homebrew/Cellar/sdl2/2.28.1/lib/libSDL2.dylib`;
const SDL = Deno.dlopen(
  libName,
  {
    "SDL_InitSubSystem": { parameters: ["i32"], result: "i32" },
    'SDL_QuitSubSystem': { parameters: ["i32"], result: "i32" },
    'SDL_CreateWindow': { result: 'pointer', parameters: [ 'buffer', 'i32', 'i32', 'i32', 'i32', 'i32' ] },
    'SDL_Delay': { parameters: ["i32"], result: "i32" },
    'SDL_PollEvent': { result: 'i32', parameters: [ 'buffer' ] },
    } as const,
).symbols;

console.log(SDL.SDL_InitSubSystem(0x00000020));
const window = SDL.SDL_CreateWindow(null, 100, 100, 600, 600, 0);
console.log(window);

let e = new Int32Array(64);
let quit = false;
while (!quit){
  while (SDL.SDL_PollEvent(e)){
    e.type = e[0];
    console.log(e.type);
        if (e.type == SDL_QUIT){
            quit = true;
        }
        if (e.type == SDL_KEYDOWN){
            quit = true;
        }
        if (e.type == SDL_MOUSEBUTTONDOWN){
            quit = true;
        }
    }
}
console.log('closing...');

SDL.SDL_QuitSubSystem(0x00000020);
```

### Possible project file

```toml
# https://doc.rust-lang.org/cargo/reference/manifest.html
[dependencies]
# github: org.repo = "tag/commithash"
# github: org.repo.folder = "tag/commithash"
korlibs.korge.kds = "4.0.4"
```
