# kaml

kaml is the YAML library of Davils: a strict, lossless YAML 1.2 and 1.1 parser, emitter and document toolkit for
Kotlin on the JVM and on WebAssembly (`wasmJs`), built on [Kore](https://github.com/davils-com/kore) and safe to feed
with untrusted input. It is published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kaml-core` | `com.davils:kaml-core` | Strict decoding of UTF-8, UTF-16 and UTF-32, a lossless parser, the event stream, the node graph, the failsafe, JSON, core and YAML 1.1 schemas, the emitter, the `Yaml` facade, the lossless syntax tree with incremental reparsing, the editor that keeps comments and the formatter |
| `kaml-document` | `com.davils:kaml-document` | JSON Pointer, JSONPath on a linear-time I-Regexp engine, JSON Patch and JSON Merge Patch on the graph and in place on the text, layered merges with origins, diffs and redaction that fails closed |
| `kaml-serialization` | `com.davils:kaml-serialization` | `YamlFormat` for kotlinx.serialization with positioned diagnostics, a closed registry for polymorphism and the conversion to and from `JsonElement` |

kaml is a Kotlin Multiplatform library for two targets, `jvm` and `wasmJs`, written for Kotlin 2.4 or newer. On the
JVM it requires JDK 25, and every module is a named Java module: `com.davils.kaml.core`, `com.davils.kaml.document`
and `com.davils.kaml.serialization`. On Wasm it is tested under Node.js and runs in the browser as well. Everything
that reads or writes YAML is common code, so both targets give the same results.

## Installation

kaml is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only, so no
other repository can ever serve a Davils artifact:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:kaml-core:1.0.0")
    implementation("com.davils:kaml-document:1.0.0")
    implementation("com.davils:kaml-serialization:1.0.0")
}
```

Gradle picks the JVM or the Wasm variant from these coordinates. A Maven build names the JVM artifacts directly,
such as `com.davils:kaml-core-jvm`.

## A short tour

### Reading and writing YAML

```kotlin
when (val result = Yaml.Default.parse(text)) {
    is YamlParseResult.Parsed -> {
        val root = result.stream.single?.root
        val port = root["server"]["port"].int()
    }
    is YamlParseResult.Failed -> result.diagnostics.forEach { diagnostic -> println(diagnostic) }
}

val text = Yaml.Default.emit(yamlMapping { "name" to "kaml"; "port" to 8080 })
```

Parsing never throws for malformed input: the result carries diagnostics with a code, a range and an escaped
message. Every input is bounded by `YamlLimits`, which `Yaml.Default` sets for untrusted sources: the length of the
source, the depth, the nodes, the expansion of aliases and merge keys, anchors, scalars, collections, documents,
numbers, diagnostics and the output. A billion laughs document stops at its budget, not at the heap. For large
documents from a trusted source, `yaml { limits = YamlLimits.Relaxed }` raises every limit, but no limit is ever
unbounded.

### Editing text and keeping its comments

```kotlin
val tree = YamlSyntaxTree.parse(text)
val result = YamlEditor(tree).set(YamlPath.ROOT.key("server").key("port"), yamlScalar(9090))
val edited = (result as? YamlEditResult.Edited)?.tree
```

The syntax tree keeps every character of the source and reparses only the documents an edit touches, always to the
same tree as a full parse. The editor writes values through the emitter and validates comments, so nothing it writes
can change the structure of the document around it.

### Querying, patching, merging and redacting

```kotlin
val names = JsonPath.parse("$.users[?@.active == true].name").query(root)
val redaction = yamlRedaction {
    path("$..password")
    key { name -> name.endsWith("token") }
}
val redacted = redaction.redact(tree)
```

JSONPath runs regular expressions on its own I-Regexp engine in linear time, and every query, patch, merge and diff
is bounded by steps and results. JSON Patch and JSON Merge Patch apply to the node graph and in place to the text,
keeping its comments. Redaction refuses what it cannot decide: a document with a parse error, or a target it cannot
follow through an alias, a merge key or a duplicate key.

### kotlinx.serialization

```kotlin
@Serializable
class Server(val host: String, val port: Int)

val server = YamlFormat.Default.decodeFromString<Server>(text)
val yaml = YamlFormat.Default.encodeToString(server)

when (val decoding = YamlFormat.Default.decodeOrFailure<Server>(text)) {
    is YamlDecoding.Decoded -> decoding.value
    is YamlDecoding.Failed -> decoding.diagnostics
}
```

A type name in a document selects only a serializer the application registered or a sealed subclass; kaml never
instantiates a class by its name. Messages never echo the input or the message of a serializer.

## Security

kaml treats every input as hostile: the bytes of a document, JSONPath expressions, JSON Pointers and patches, the
layers of a merge, the targets of a redaction, and the values, comments, tags and anchors a caller asks it to write.
[SECURITY.md](SECURITY.md) describes the threat model, the limits, the known limitations and how to report a
vulnerability.

## Building

```bash
./gradlew check
```

`check` compiles with warnings as errors, runs Detekt, the tests on the JVM and on Wasm under Node.js, Kover at 97 %,
the API and semver check, the license header and changelog checks, Trivy and the Kreate assurance gate. It needs
JDK 25 and the network. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
