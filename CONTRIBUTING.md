# Contributing to kaml

Thank you for helping to build kaml. This document explains how a change gets in. The binding rules for code,
tests and the build are in [AGENTS.md](AGENTS.md); read it before your first change.

## Code of Conduct

Everyone who takes part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Open an issue for anything larger than a small fix, so the approach can be agreed on first. Security problems
   are never reported as an issue; see [SECURITY.md](SECURITY.md).
2. Branch from `develop` and keep the change to one concern.
3. Run `./gradlew check` until it is green. It needs JDK 25 and network access.
4. Open a pull request against `develop` and describe what changed and why.

## Development Standards

### One command before you push

```bash
./gradlew check
```

It compiles with warnings as errors, runs Detekt, the tests on the JVM and on Wasm, coverage, the API check, the
license header check, the changelog check, Trivy and the Kreate assurance gate. A pull request is only merged when it
is green.

### Security first

Every input of kaml may come from an attacker: YAML bytes, JSONPath expressions, JSON Pointers, patches and the
values handed to the serializer. A change that reads input checks it against `YamlLimits` first, charges every
budget while it works, bounds every recursion by depth, escapes every untrusted value it shows through `SafeText` of
Kore, never puts a sensitive value into a message and fails closed. It ships with tests that feed hostile input
through every new sink and that run every new limit up to its bound and past it.

### The public API is a gate

The `api` folder of every module records its binary API, for the JVM and for the klib. A deliberate change is
recorded with `./gradlew kreateApiDump`, gets an entry in [CHANGELOG.md](CHANGELOG.md) and, when it breaks
compatibility, waits for the next major version.

### Documentation

Every public declaration has KDoc with `@since`. Internal and private declarations have none, and there are no
other comments.

## Changing a Dependency

Every dependency, the classpath of `build-logic` included, is locked and verified. After changing
`gradle/libs.versions.toml`, refresh both and review what changed:

```bash
./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
```

```bash
./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
```

A new dependency other than a library of JetBrains or of Kore needs the approval of a maintainer. Kore and Kreate
are used in released versions only, never as snapshots.

## Following a Standard

kaml implements YAML 1.2.2, JSON Pointer, JSON Patch, JSON Merge Patch, JSONPath and I-Regexp, among others. Every
standard it follows is declared in `build-logic/src/main/kotlin/kaml.standards.gradle.kts`, and `standards.lock`
records what its body published when a maintainer last reviewed it. The assurance gate fails when a declared standard
was never reviewed, when its body publishes a new version, errata or a replacement, and when a file names a standard
that is not declared. Read what changed against kaml, fix and test what it breaks, and record the review:

```bash
./gradlew kreateStandardsReview --standard=rfc:9535 --note="Erratum 8343 does not change how kaml parses."
```

## Snapshots and Releases

`gradle.properties` holds the version in development. Every push to `develop` publishes it as a `-SNAPSHOT` to
`https://repo.davils.com/snapshots`. A release is the tag of that version without a `v`, such as `1.0.0`, pushed
on a commit of `main`; the pipeline signs it, publishes it to `https://repo.davils.com/releases` and attests it.
`kreateReleaseGuard` refuses a tag that does not equal the version, a tag on a commit outside `main` and a build with
snapshot tooling, such as a Kreate snapshot. Protect the tags `[0-9]*` with a GitHub tag ruleset so that only
maintainers can push them. Before the tag, the version gets its section in [CHANGELOG.md](CHANGELOG.md).

## Commit Messages

Conventional Commits in English with the module as scope, such as `feat(core): parse flow mappings` or
`fix(document): bound the descendant segment of JSONPath`. A breaking change is marked with `!` and a
`BREAKING CHANGE:` footer.
