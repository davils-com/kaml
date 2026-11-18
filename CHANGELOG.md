# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0

The first version of kaml: the YAML library of Davils as an open source library under the Apache License 2.0, for
Kotlin on the JVM and on `wasmJs`, built with Kreate 5.2.0 on Kore 2.1.1 and published to `https://repo.davils.com`.
The code comes from the YAML stack of an internal Davils project and is hardened for use with untrusted input on the
way.

### Added

- **The build** of kaml with Kreate 5.2.0: one convention plugin per concern, strict dependency locking and
  verification, Detekt with every rule, Kover at 97 %, the API and semver check, Trivy and the assurance gate.
- **The YAML model.** Tags, anchors that refuse names YAML does not allow, versions, tag directives and the styles
  of scalars and collections.
- **The node model.** Immutable scalars, sequences, mappings, documents and streams whose depth is bounded, mappings
  without duplicate keys, typed accessors that return `null` on overflow, builders and, on the JVM, `BigInteger`
  accessors.
- **Tag resolution** with the failsafe, JSON, core and YAML 1.1 schemas and custom schemas, by hand-written linear
  scanners instead of regular expressions.
- **The event stream** of the YAML specification.
- **The emitter** for nodes and events, with comments, whose output parses back to the same graph and cannot be
  injected, within an output limit.
- **The `Yaml` facade** that reads YAML 1.2 and 1.1 from strictly decoded UTF-8, UTF-16 or UTF-32 through a
  lossless, stack-safe parser and a composer that charges every budget while it works, so a billion laughs document
  fails at its budget.
- **The lossless syntax tree** with incremental reparsing and a typed view, which no limit can be bypassed through.
- **The editor**, which changes values, keys and comments of a document as text edits that keep the rest of it and
  cannot inject YAML.
- **The formatter**, which refuses every change that would change the meaning of a document.
- **`YamlFormat` for kotlinx.serialization** with its configuration and DSL, naming strategies, polymorphism by
  property, local tag or wrapper through the public structure protocol and a closed registry of serializers, the
  annotations `YamlComment`, `YamlFlow`, `YamlNames` and `YamlStyle`, and positioned diagnostics through
  `decodeOrFailure` and `decodeAll`. Decoding applies `YamlLimits` to every node graph, also one built in code,
  refuses names that collide and keys given twice, checks every number against the range of its type, and never
  echoes input or the message of a serializer: a refused value is shown in a message only when `isValueQuoted` asks
  for it.
- **Conversion between `YamlNode` and `JsonElement`** of kotlinx.serialization.json, within `YamlLimits`, refusing
  keys that are not strings or are given twice.
- **JSON Pointer** (RFC 6901) on the node graph and on the syntax tree, where a duplicate key resolves to
  its first occurrence.
- **A linear-time I-Regexp engine** (RFC 9485) for `match` and `search` of JSONPath, with bounded patterns,
  programs and steps.
- **JSONPath** (RFC 9535) with nesting, step and result budgets; a query beyond a budget fails instead of
  answering in part.
- **JSON Patch** (RFC 6902), **JSON Merge Patch** (RFC 7386) and diffs on the node graph and in place on the
  syntax tree, within an operation limit and the node budget.
- **Layered merges** with the origin of every value, merging sequences by key through a hash index.
- **Redaction that fails closed**: broken documents are refused, targets behind aliases, merge keys and
  duplicates are redacted, the replacement is literal and no secret reaches a message.

- **Limits and diagnostics.** `YamlLimits` bounds the input, the depth, the nodes, the alias expansion, the anchors,
  scalars, collections, documents, numbers, diagnostics and the output, each with a hard ceiling; `YamlDiagnostic`
  carries a code, a range, a severity and an escaped message.