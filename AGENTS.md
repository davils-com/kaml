# AGENTS.md

Binding rules for every agent and every person working in this repository. kaml is the YAML library of Davils: a
strict, lossless YAML 1.2 parser, emitter and document toolkit for Kotlin on the JVM and on WebAssembly (`wasmJs`),
built on [Kore](https://github.com/davils-com/kore). It is published under the Apache License 2.0 and is built to the
standard it is used for, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kaml-core` | `com.davils:kaml-core` | The limits and diagnostics, strict decoding of YAML sources, the parser and its lossless syntax tree with incremental reparsing, the event stream, the node model, the failsafe, JSON, core and YAML 1.1 schemas, the composer, the emitter, the `Yaml` facade, the typed view of the syntax tree, the editor and the formatter |
| `kaml-document` | `com.davils:kaml-document` | JSON Pointer, JSONPath with a linear-time I-Regexp engine, JSON Patch and JSON Merge Patch, layered merges, diffs and fail-closed redaction of YAML documents |
| `kaml-serialization` | `com.davils:kaml-serialization` | `YamlFormat` for kotlinx.serialization: decoding, encoding, polymorphism and the interop with JSON |
| `build-logic` | - | One convention plugin per concern, composed by `kaml.kotlin-library` and `kaml.serialization-library` for the modules and `kaml.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The modules are ported from the YAML stack of an internal Davils project and arrive one by one; until a module
exists, its row describes where its code goes. `kaml-core` builds on `com.davils:kore-core`, `kaml-document` and
`kaml-serialization` build on `kaml-core`, and `kaml-serialization` on kotlinx.serialization as well.

The root package of a module is `com.davils.kaml.<module without kaml->`, such as `com.davils.kaml.core`, and its
Java module has the same name. Every module declares it in `src/jvmMain/java/module-info.java` and exports only
packages with public API, never an `internal` package.

## Targets

Every module is a Kotlin Multiplatform library with two targets: `jvm` and `wasmJs`. The Wasm tests run under Node.js;
the published klib runs in the browser as well.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM can offer, such as
  `java.math.BigInteger`, files, paths, threads and the other types of the JDK, lives in `src/jvmMain/kotlin`; a member
  of a common class that needs a JDK type becomes an extension function there.
- The parser, the composer, the emitter and everything else that reads or writes YAML is common code. The JVM-only
  replacements of the original code, such as `java.nio.charset`, `IdentityHashMap` and `Regex`, are written in common
  code instead, so both targets behave the same.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`, and a parity test proves that both targets give the same result. kaml declares no `expect` or
  `actual` class: they are a Beta language feature.
- JavaScript interop is written with `js("...")` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and
extra warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and
`allRules`, the tests on the JVM and on Wasm, Kover verification at 97 % of lines, branches and instructions, the API
and semver check, the license header check, the changelog check, every Trivy scan and the Kreate assurance gate,
which blocks on every finding. The gate reads OSV.dev and deps.dev and Trivy downloads its database, so `check` needs
the network. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it and toolchains are
never downloaded.

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without
a baseline. Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **One top-level type per file**, named after it; a file of functions is named after its role.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead, and when a subpackage shrinks to one file, that file moves up and the
  subpackage goes away. Exceptions are the root package of a module and an `internal` package, which may hold a
  single file.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `YamlNode`, never
  `KamlYamlNode` or `KamlNode`. The package says where a declaration comes from. Where a name meets a type of the same
  name, the foreign type is imported with an alias, such as `import kotlinx.serialization.json.JsonElement as
  KotlinxJsonElement`, and a KDoc link names it in full. kaml uses the annotations of Kore, such as `DavilsInternal`,
  `DavilsExperimental` and `DavilsDsl`, and declares no annotation of that kind itself. The ids of the Gradle plugins in
  `build-logic` keep their `kaml.` prefix.
- **`@DavilsInternal`** marks API shared between Davils libraries that consumers must not use; it fails the build of
  every caller that does not opt in. **`@DavilsExperimental`** marks API that may still change.
- **Immutable values.** Public types are immutable; public collections are built with `immutableList()`,
  `buildList` and the like, never stored from `toList()` or `+`, so a cast cannot change them. Whatever a caller
  configures, such as the comments to emit, is built with a validating builder and frozen when it is built.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString`; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent. Atomics come from `kotlin.concurrent.atomics`, through
  a local `@OptIn(ExperimentalAtomicApi::class)`.
  Experimental Kotlin API is allowed only through a local `@OptIn` and never appears in a public signature. Internal
  API of kotlinx.serialization, such as `InternalSerializationApi`, is avoided; where nothing else works, it stays
  in one file with a test that pins the version of kotlinx.serialization.
- **Nothing global.** No mutable singletons, no static configuration, no global caches, no threads or timers of the
  library's own. Detekt does not enforce this rule; reviews do.

## Security rules

Every input of kaml may come from an attacker: the bytes of a YAML document, JSONPath expressions, JSON Pointers,
JSON Patches and Merge Patches, the layers of a merge, the targets of a redaction, the values handed to the
serializer and the comments, tags and anchors a caller asks the emitter to write. kaml is built so that a mistake of
a caller fails closed.

- **Limits before work.** Every entry point checks its input against `YamlLimits` before it does work that grows
  with the input, such as the length of the source, of a scalar and of a number, and refuses what is too large with
  a diagnostic or an exception. Nothing is truncated silently. A new limit goes into `YamlLimits`, never into a
  private constant, and has a hard upper bound: no preset, the most relaxed one included, is unbounded, and no
  entry point, the syntax tree and its incremental reparse included, bypasses a limit.
- **Budgets apply while the work is done.** The budgets of nodes, scalars, anchors, aliases and diagnostics are
  charged while events and nodes are built, not checked afterwards, so an oversized document stops at the first
  excess instead of after it is in memory.
- **Aliases and anchors have a budget.** Every alias that is resolved, expanded, detached, copied or merged through
  `<<` charges an expansion budget with the nodes it stands for, so a billion laughs document fails at its budget
  and never at the heap. Exceeding the anchor limit stops the work; it is never only reported.
- **Depth bounds every recursion.** Every recursive walk, such as the parser, the composer, `equals`, `hashCode`
  and `toString` of nodes, the emitter, the syntax tree, JSONPath, patches, merges, diffs, redaction and the
  serializer, checks its depth against a limit before it descends. A `YamlNode` knows its depth and refuses one
  beyond the hard limit, also when it is built in code. A walk that can be iterative, such as the descendant segment
  `..` of JSONPath, is iterative and has a step and a result budget.
- **No input decides the complexity.** Regular expressions of a JSONPath run on the I-Regexp engine of
  `kaml-document` in O(n·m); input never reaches `Regex`. Numbers are converted in linear time within
  `maxNumberLength`, merges by key use a hash index, and a scalar that is too long is never parsed as a number.
- **Escape untrusted text exactly once, at the sink.** Whatever shows text that may come from input, such as an
  exception message, a diagnostic, a `toString` or a report, writes it through `SafeText` of Kore, which escapes
  control, format and separator characters, bidirectional overrides and lone surrogates and bounds the length.
  Canonical values with a restricted alphabet, such as a JSON Pointer that was validated, are shown as they are.
- **Emitted YAML cannot be injected.** The emitter validates what it writes: a comment is split into lines and
  refuses control characters, a tag is percent-encoded, an anchor is limited to a restricted alphabet and a scalar
  that cannot be written plain is quoted, so no value can start a node, a document or a directive. What kaml emits
  parses back to the same graph, and the emitter has a depth and an output limit.
- **Secrets never leak.** A sensitive value, such as the target of a redaction, never appears in a message, a
  reason, a cause, a `toString` or a report. The only mask is `SafeText.REDACTED`; it reveals neither length nor
  count.
- **No raw input in exceptions.** Messages are constant text plus escaped, bounded fields; a thrown exception of a
  callback that may carry input is replaced by a redacted placeholder. Only the exceptions a contract names are
  caught, never an `Error`.
- **Redaction fails closed.** A document with a parse error is refused, never redacted in part. A target reached
  through an alias or a merge key `<<` redacts its anchor and every reference to it, every occurrence of a duplicate
  key is redacted, the replacement is literal, and the trailing comment of a redacted value goes with it. When a
  redaction cannot decide, it refuses.
- **Fail closed.** Malformed input is an error and never repaired silently: invalid UTF-8, UTF-16 or UTF-32, a
  lone surrogate, also from an escape, a code point above U+10FFFF and U+FFFE or U+FFFF are refused, never replaced
  by U+FFFD. A non-finite number is refused where a finite one is expected. Duplicate keys resolve by the same rule
  in every API.
- **Tags never construct types.** A tag or a type name selects only a serializer the application registered;
  kaml never instantiates a class by its name.
- **Hide what Java must not reach.** Kotlin's `internal` is `public` in the bytecode, so a Java caller on the module
  path reaches every internal class of an exported package. A class whose misuse weakens a guarantee, such as the
  green tree of the parser or the budgets of the composer, lives in the `internal` subpackage of its package, for
  example `com.davils.kaml.core.syntax.internal`, and `module-info.java` never exports an `internal` package.
- **No randomness.** Reading and writing YAML needs none. No `java.util.Random`, `kotlin.random.Random` or
  `ThreadLocalRandom`; if randomness is ever needed, it comes from Kore.
- **Every sink has an injection test.** A renderer is tested with text built from `HostileFragments` of
  `kore-testing`, combined by a Kotest generator, and its output must leave `unsafeCodePointsIn` empty; a sensitive
  value has a property test that it appears nowhere; every limit and budget has a test at the limit and one past it.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from `kore-testing` or a `fixture`.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test,
  even when that package holds a single test. A test that needs the JVM, through reflection, threads, files, Java
  serialization, a JDK type or resources on the class path, lives in `src/jvmUnitTest/kotlin`; a test of common code
  stays common wherever it can. A fixture lives in the `fixture` folder of the only subpackage that uses it, or in the
  shared `fixture` folder of the area; a `fixture` folder holds at least two files, so a lone fixture lives next to the
  tests that use it. `src/test` is rejected by the build.
- The YAML test suite lives once, in `kaml-core/src/jvmUnitTest/resources`, and runs on the JVM; selected cases run
  inline in `src/commonUnitTest/kotlin` on both targets.
- Property tests with adversarial generators for every parser and every security invariant: YAML, JSONPath, JSON
  Pointer and patches; emitting and parsing again gives the same graph; an incremental reparse gives the same tree as
  a full parse; random bytes never throw an unexpected exception.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("kaml.kotlin-library")`, or `id("kaml.serialization-library")` for
  `kaml-serialization`, and declares nothing but its description and its dependencies, through the configurations
  of the source sets such as `commonMainApi`. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured
  only in `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one concern and applies `kaml.base`:

  | Plugin | Configures |
  |---|---|
  | `kaml.base` | The Kreate plugin, the group and the version |
  | `kaml.dependency-locking` | Strict locking of every configuration |
  | `kaml.git-hooks` | The Git hooks |
  | `kaml.trivy` | Every Trivy scan |
  | `kaml.assurance` | The assurance gate and its `ignore` rules |
  | `kaml.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `kaml.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `kaml.testing` | Kotest and the test suites |
  | `kaml.detekt` | Detekt and its reports |
  | `kaml.coverage` | Kover and its verification |
  | `kaml.api-validation` | The API dump and the semver check |
  | `kaml.docs` | Dokka |
  | `kaml.publish` | The POM, the release guard and the Davils repository |
  | `kaml.archives` | The legal files and the manifest of every archive |
  | `kaml.tool-classpaths` | The version floors on the class paths of the build tools |
  | `kaml.changelog` | The changelog check |
  | `kaml.license-header` | The license header check |
  | `kaml.serialization` | The kotlinx.serialization compiler plugin |

  `kaml.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `kaml.serialization-library` adds `kaml.serialization` to it; `kaml.root` composes them for the root project. A new
  concern gets a plugin of its own, never a block in a composing plugin.
- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains and of Kore may be added when they
  are needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, Kore and Kreate included, through `exclusiveContent`.
  Everything else comes from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- kaml builds only with released versions of Kreate and Kore. `gradle/verification-metadata.xml` trusts `com.davils`
  artifacts only by the signature of the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063`; a snapshot of
  Kreate or Kore is never put into the catalog, and no trust rule for unsigned snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/kaml.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every publication,
refuses a build whose resolved version differs from it, so no environment variable can change what is published.
Every build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`, that equals that
version, sits on a commit of `main` and is built without any snapshot tooling; the guard refuses every other
combination before anything is published. Snapshots go to `https://repo.davils.com/snapshots`, releases to
`https://repo.davils.com/releases`, and never to Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in `DAVILS_MAVEN_NAME`
and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in
`ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`; a release without a signing key fails.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(core): ...`, `fix(document): ...` or `feat(serialization): ...`. Small commits, one concern each. Commit only
when asked. No `Co-Authored-By` trailer and no other attribution to tools or AI assistants.
