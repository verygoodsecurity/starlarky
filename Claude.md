# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Starlarky is VGS's fork of Bazel's Java Starlark interpreter, used to run untrusted user-submitted scripts. Maven multi-module repo (Java 21):

- `libstarlark/` — the Starlark lexer/parser/resolver/evaluator (`net.starlark.java.*`), periodically synced from bazelbuild via `bin/update-starlark.py`, which replaces `src/{main,test}/java/net/` with upstream's tree (rsync, deleting extra files). VGS's own libstarlark code lives in separate source roots (same packages, added by build-helper), which the sync never touches: `src/main/vgs/` / `src/test/vgs/` for changes to the language and runtime (bytes/bytearray, int limits, Python string and `%`/`format()` behaviour, `ScriptFilesTest`), and `src/main/bc/` / `src/test/bc/` for the bytecode compiler and VMs. Put new VGS classes there, not in `src/main/java`, and keep edits to upstream files to small hooks that call into them, so syncs merge cleanly. New VGS files carry a `Copyright <year> Very Good Security Authors` Apache 2.0 header. `bin/*-differences-libstarlark-*.patch` records the remaining delta against an upstream commit; after a sync (or any edit to an upstream file), regenerate it with `bin/regenerate-differences-patch.sh`, which diffs against the `.tmp/bazel` checkout and checks that upstream + patch rebuilds our tree (`-r REF` diffs a commit instead of the working tree).
- `larky/` — VGS additions (`com.verygood.security.larky.*`): JSR223 engine, Python-compat object model (`objects/`, `modules/types/`), native Java modules (`modules/`), and a Starlark stdlib written in `.star` (`src/main/resources/stdlib`, `vendor`, `vgs`).
- `runlarky/` — Quarkus/GraalVM native CLI (`larky-runner`). Adding or moving a native module in `larky/modules` requires updating `runlarky/src/main/resources/reflect-config.json`.
- `larky-api/`, `pylarky/` — Java API and pip wrapper around the runner (not in the root reactor).

## Commands

```bash
mvn clean install -DskipTests                      # build everything; larky depends on libstarlark 1.0.0-SNAPSHOT from ~/.m2
mvn test -pl libstarlark                           # libstarlark tests (tree-walker)
mvn test -pl libstarlark -Dstarlark.bytecode=true -Dstarlark.bytecode.strict=true  # same suite on the bytecode VM, failing instead of falling back
mvn test -pl libstarlark,larky -Dstarlark.bytecode=true -Dstarlark.bytecode.strict=true -Dstarlark.bytecode.vm=starlark-go  # pick the VM: interpreter (default), starlark-go, starlark-rust, buck, jvm
mvn test -pl libstarlark,larky -Dstarlark.bytecode=true -Dstarlark.bytecode.strict=true -Dstarlark.bytecode.vm=jvm -Dstarlark.jit.threshold=0  # jvm compiles a chunk after 50 runs by default; 0 makes the suites run compiled code
mvn test -pl libstarlark -Dtest=EvaluationTest#testExec -Dstarlark.bytecode=true   # single test
mvn test -pl larky -Dtest=StdLibTests -Dlarky.stdlib_test=test_bytes.star           # one larky stdlib .star test
```

- After changing `libstarlark`, run `mvn install -pl libstarlark -DskipTests` before testing `larky`, or use `-pl larky -am`.
- Surefire reports: `<module>/target/surefire-reports/*.txt`. Check file timestamps; a failed Maven run (e.g. `-o` plugin-resolution failure) leaves the previous run's report in place.
- Some interrupt tests can leave Maven hanging after tests finish; wrap long runs in `timeout`.
- Debug flags (system properties): `-Ddebug.bytecode=true` (dump chunks + trace each instruction), `-Ddebug.globals=true`.
- `.star` test locations: libstarlark `src/test/java/net/starlark/java/eval/testdata/` (upstream, plus VGS's `bytes.star`), `src/test/vgs/net/starlark/java/eval/testdata/` (VGS) and `src/test/bc/net/starlark/java/eval/testdata/` (bytecode regressions) — each file is a JUnit case of `ScriptFilesTest` (runs `ScriptTest.runFile` on a thread with a 512k stack; `json.star`'s nesting-depth cases depend on hitting `StackOverflowError`). Upstream files VGS can't run as-is get a text substitution in `ScriptFilesTest.VGS_EDITS` rather than an edit. larky `src/test/resources/{stdlib_tests,vendor_tests,vgs_tests,quick_tests}` (run by `StdLibTests`, `VendorLibTests`, `VGSLibTests`, `LarkyQuickTests`).
- CI parity: `docker-compose run local bash /src/build-and-test-java.sh`.

## Bytecode execution path (libstarlark)

An alternative to the tree-walking `Eval`, enabled only when `-Dstarlark.bytecode=true`:

1. `syntax/Program` compiles the body with `BytecodeCompiler.compileFunction` when the property is set (`compileBytecode`). On compile failure it prints a warning and **silently falls back** to the tree-walker unless `-Dstarlark.bytecode.strict=true` is set — always use strict when testing bytecode. `Program.compileFile(file, env, enableBytecode)` forces compilation explicitly. The compiler must throw on syntax it doesn't know (the statement and expression switches have throwing defaults), so new upstream syntax fails loudly in strict runs.
2. `Starlark.execFileProgram()` keeps upstream's code and only replaces its final call with `BytecodeVms.execFile`, which runs the VM when `prog.hasBytecode()`. A typed program (one with a type table) also runs on the VM: `BytecodeGlobals` carries the type table, `BcOps.makeFunction` types each function and checks its defaults, `BytecodeFunction` checks arguments and return values as `StarlarkFunction` does, and `TYPE_ALIAS` binds `type X = ...` aliases. Top-level code runs as a `BytecodeToplevel` callable via `Starlark.positionalOnlyCall`, which pushes the frame and wraps unchecked exceptions. The VM's namespace (`BytecodeGlobals`) is a live view of the `Module`'s globals (functions see later assignments, e.g. from another file run in the same module, as in a REPL); PREDECLARED/UNIVERSAL names are read with `LOAD_BUILTIN`, so file-level bindings shadow builtins as in the tree-walker. Top-level frames are `BytecodeToplevel`, which, like `BytecodeFunction`, reports its module to `Module.ofInnermostEnclosingStarlarkFunction`.
3. `eval/compiler/`: `BytecodeCompiler` (AST → `BytecodeChunk`: instructions + `ConstantPool` + locals/line/column metadata), `Opcode`, `Instruction`. `def` bodies compile to nested chunks wrapped at runtime as `BytecodeFunction` (must report `type()` as `"function"` and participate in recursion detection like `StarlarkFunction`).
4. VMs: every instruction that does more than move a value is implemented once, in `BcOps` (static helpers taking a `BcFrame`). `AbstractBytecodeVM`'s dispatch loop calls them; `BytecodeInterpreter` (default), `StarlarkGoInterpreter`, `StarlarkRustInterpreter` and `BuckStyleInterpreter` are subclasses that only choose storage (`push`/`pop`/`getLocal`...: separate stack list, unified growable slot array, ...) and optional hooks (`binaryOp` specialization, `getAttr` caching, stats). `jvm` (`JvmBytecodeCompiler`) compiles each chunk to a JVM method (ASM, hidden class, cached on the chunk) whose Starlark stack slots are JVM locals (the stack depth at each instruction must be static) and which calls the same `BcOps` helpers; a chunk too large for a JVM method (64KB) stays interpreted. Fix semantics in `BcOps`, never in a VM. `-Dstarlark.bytecode.vm` (read once by `BytecodeTarget.configuredVm()`) selects the VM for top-level code and function bodies via `BytecodeVms`.

Invariants that have caused bugs:
- `Instruction.create(Opcode, int operand, int offset)` — operand before offset.
- Labels/jump targets are **instruction indices**, not byte offsets; nested functions must `patchJumps()` before their chunk is built; labels must be unique across nested if/else.
- `FOR_ITER` pops the iterator itself on exhaustion — no extra `POP` after a loop. Iteration must call `EvalUtils.addIterator/removeIterator` so mutation-during-iteration errors fire.
- Comprehensions: each `for` clause adds one iterator to the stack; `LIST_APPEND`/`DICT_ADD` depth operands must account for it.
- `InterruptedException` must propagate unwrapped; the main loop must count steps / honor `thread` interrupt and expiry. Other runtime exceptions propagate too (no catch-all); `Starlark.positionalOnlyCall` / `callViaArgumentProcessor` wrap them as `UncheckedEvalException`.
- Calls follow `Eval.evalCall`: positional-only calls use `Starlark.positionalOnlyCall`, others add arguments in source order (positional < keyword < `*` < `**`) to the callee's `ArgumentProcessor` and call `Starlark.callViaArgumentProcessor`; duplicate/unexpected-keyword errors come from the callee.
- Local stores go through `storeLocal`, which also updates the frame's locals array; the debugger (`StarlarkThread.Frame.getLocals`) reads that array.
- Top-level unindented assignments and defs emit `POST_ASSIGN` so the thread's post-assign hook (Bazel "export") sees them, as in `Eval.execStatements`.
- Each error-raising instruction is emitted with the tree-walker's error location (`emitAt(loc, ...)`: operator, dot, lbracket, `=`, for-clause start). Iteration locks are released in `run()`'s `finally` for loops exited by `return`/exception.
- `ConstantPool` dedups by class + value (floats by bits): `1 == 1.0` in Starlark, but they must stay distinct constants.
- Error text and locations must match the tree-walker exactly (`EvalException.withLocation`); `ErrorConsistencyTest` and `EvaluationTest` compare them.

## Larky layer

- `ModuleSupplier` defines what scripts can see: `CORE_MODULES`/`CORE_ENVIRONMENT` (globals such as `LarkyGlobals`, Python builtins, `classmethod`), `STD_MODULES`, `VGS_MODULES`, `TEST_MODULES`. Native modules are `@StarlarkBuiltin`-annotated classes; `.star` stdlib modules wrap them (e.g. `stdlib/re.star` over `RegexModule`).
- `LarkySemantics` holds Larky-specific `StarlarkSemantics` flags.
- `ProgramCache` keeps the compiled `Program` of Larky's own resource modules (stdlib/vendor/vgs) process-wide; each evaluation still executes it into a fresh `Module`. A cached program is reused only while the names it resolved as PREDECLARED/UNIVERSAL still resolve that way. `-Dlarky.programCache.disable=true` turns it off.
- Code that special-cases user functions must test `UserDefinedFunction` (implemented by both `StarlarkFunction` and `BytecodeFunction`), not `StarlarkFunction` — e.g. `LarkyProvidedTypeClass` wraps class members as `LarkyFunction` descriptors to bind `self`.
- JSR223 (`jsr223/LarkyCompiledScript`): `compile()` resolves leniently (bindings arrive at eval) and caches the compiled program; `eval()` always goes through the Larky interpreter (on the bytecode VM when `-Dstarlark.bytecode=true`). Never open `context.getReader()` unless there is no compiled source — the default reader wraps `System.in`, and closing it kills the surefire fork.

## Rules (from `.cursorrules`)

- Scripts are untrusted: no file/OS/process/network access, reflection, dynamic class loading, or JNI escape hatches from builtins; no unseeded randomness or wall-clock/timezone access; don't leak Java stack traces in Starlark errors.
- Keep Starlark semantics (not Python): respect freezing/mutability, don't add Python-only features. Return Starlark values from builtins and validate arguments strictly.
- Error message form: ``fn(arg=…) expected `<type>`; got <type>``.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
