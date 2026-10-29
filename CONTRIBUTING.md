# Contributing to Swish

Swish is maintained by ArsDevs. File reproducible bugs or focused feature ideas
in this repository. For large changes, discuss the design before implementation.
Keep private data out of logs and screenshots; use the private reporting route
in SECURITY.md for vulnerabilities.

## Ways to help

| Contribution | Start here |
| --- | --- |
| Report a bug | [Open an issue](https://github.com/realarsalanahmad/swish/issues/new/choose) with steps, app version and device details |
| Suggest an improvement | Explain the problem and the experience you want in a feature request |
| Improve code or documentation | Fork the repository, make a focused change and open a pull request |

A fork is your own copy of Swish. A pull request proposes changes back to this
repository. ArsDevs reviews the proposal, may request changes, and decides whether
to merge it. Opening a pull request does not give access to edit this repository
or publish an APK. Contributions are voluntary; no payment is required.

## Develop

1. Fork this repository and create a branch from its current main branch.
2. Install JDK 21, Android SDK platform 37.0 and Build Tools 36.1.0.
3. Copy local.properties.example to local.properties and set sdk.dir.
4. Build a flavor with `./gradlew :app:assembleGithubDebug` or the FOSS equivalent.
   Windows PowerShell uses `gradlew.bat`.

Use Kotlin, Compose Material 3 and existing shared components. Keep network,
database and expensive work off the main thread. Honor lifecycle and cache
invalidation. Preserve playback performance and disabled-feature privacy.
Read AGENTS.md for architecture, formatting and testing rules. Internal legacy
identifiers are retained where renaming would break stored data or integrations.

## Validate a change

```bash
./gradlew ktlintCheck
./gradlew :app:testGithubDebugUnitTest :app:testFossDebugUnitTest
./gradlew :app:assembleGithubDebug :app:assembleFossDebug
```

Run the affected UI on a device/emulator. Recommendation changes also require
the existing video/music benchmark tests. Do not change database schemas or bump
versions without an explicit maintainer decision. Add user-facing strings to
the default resources and tests where behavior needs verification.

Explain the problem, resulting behavior and verification in the pull request.
Contributions must be compatible with GPL-3.0; preserve inherited and dependency
notices. Do not upload APKs, caches, credentials or personal data with source.
