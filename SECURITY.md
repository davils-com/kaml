# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kaml/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected kaml version, module, target (JVM or Wasm) and JDK or JavaScript host,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal YAML document and a Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Threat Model

kaml is a library: it runs with the privileges of the application that uses it. It assumes that an attacker
controls:

- the **bytes of every YAML document** kaml reads, including their encoding, byte order mark, directives, tags,
  anchors, aliases, merge keys, comments and nesting,
- every **JSONPath expression**, including the regular expressions of `match` and `search`, every **JSON Pointer**,
  every **JSON Patch** and **JSON Merge Patch** and every layer of a merge,
- the **paths and documents of a redaction**, except the decision which paths are sensitive,
- the **values** an application hands to `kaml-serialization` and the **comments, tags and anchors** it asks the
  emitter to write.

The sinks that must stay safe are logs, terminals and reports that show messages, diagnostics and `toString`
output, and every YAML text kaml writes, which may be parsed again by kaml or by any other YAML parser.

kaml defends against this attacker with limits that are checked before any work and charged while the work is done,
budgets for anchors and alias expansion, a depth limit on every recursion, a linear-time regular expression engine,
strict decoding, escaping at every sink and fail-closed redaction. The bounds are the properties of `YamlLimits`;
their KDoc lists every default and every hard upper bound.

## Scope

The following are **in scope**:

- input within the configured limits that makes kaml use unbounded memory or CPU, superlinear time in the size of
  the input, a stack overflow, or crash with an unexpected exception, on the JVM or on Wasm,
- a document whose aliases or merge keys expand beyond the alias budget without being refused,
- an entry point that does work beyond a limit of `YamlLimits`, or a preset whose limits are unbounded,
- untrusted text that reaches a message, a diagnostic, a report or a `toString` unescaped, so it can forge log lines
  or control a terminal,
- emitted YAML in which a comment, tag, anchor, key or scalar starts a node, a document or a directive of its own, or
  that parses back to a different graph,
- a redaction that leaves a sensitive value readable, through an alias, a merge key, a duplicate key, a comment or a
  document with parse errors, or that reveals the length or the count of what it hid,
- a tag or a type name that makes `kaml-serialization` choose a serializer the application did not register,
- malformed input, such as invalid UTF-8, UTF-16 or UTF-32 or a lone surrogate, that is accepted or silently
  replaced instead of refused.

The following are **not** vulnerabilities in kaml:

- a vulnerability in the JDK, the JavaScript host, the Kotlin standard library, kotlinx.serialization or Kore —
  report those to the respective project,
- an application that raises the limits of `YamlLimits` to their hard upper bounds and then runs out of memory
  within them,
- an application that does not mark a path as sensitive, or logs a value it read,
- the behaviour of another YAML parser on YAML that kaml emitted correctly.

## Known Limitations

- `@DavilsInternal` is a compile-time opt-in, not an access boundary. Kotlin `internal` is public in JVM bytecode:
  Java code and reflection reach internal declarations through their mangled names. On the module path the
  `module-info` of every module exports only packages with public API, and implementation details live in packages
  it does not export; on the class path every class is reachable. kaml therefore validates in every constructor.
- Messages and diagnostics escape input as Kore's `SafeText` does: escapes are written as `\u{XXXX}`, `\n`, `\r`
  and `\t`. A literal backslash in free text is shown as it is, so input can display text that looks like an escape,
  but never a real control character. Right-to-left letters are shown as they are; a viewer may reorder the neutral
  characters around them.

## Verifying a Release

Releases are published to `https://repo.davils.com/releases` and signed with the release key of Davils,
`36F578E8AD2D4391E0FA6C18154C625FAD370063`, which a consumer can pin as a `trusted-key` in its own
`gradle/verification-metadata.xml`. A release also carries a signed build provenance attestation made by the GitHub
Actions run that built it:

```bash
gh attestation verify kaml-core-jvm-1.0.0.jar --repo davils-com/kaml --signer-repo davils-com/kotlin-workflow
```

Snapshots in `https://repo.davils.com/snapshots` are neither signed nor attested and are not meant for production.
