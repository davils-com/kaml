# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0

The first version of kaml: the YAML library of Davils as an open source library under the Apache License 2.0, for
Kotlin on the JVM and on `wasmJs`, built with Kreate 5.2.0 on Kore 2.1.0 and published to `https://repo.davils.com`.
The code comes from the YAML stack of an internal Davils project and is hardened for use with untrusted input on the
way.

### Added

- **The build** of kaml with Kreate 5.2.0: one convention plugin per concern, strict dependency locking and
  verification, Detekt with every rule, Kover at 97 %, the API and semver check, Trivy and the assurance gate.
