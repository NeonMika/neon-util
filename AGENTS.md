# AGENTS.md

Notes for AI coding agents (and humans) working on **neon-util**: a small Kotlin/JVM library that prints detailed information
about the running JVM. Usage and features are in [README.md](README.md). This file covers how to work on the project and how
releases are done, including the pitfalls already hit once.

## Project layout

| Path | Purpose |
|---|---|
| `src/main/kotlin/neon/jvm/JVMUtil.kt` | The whole library: `printFullJavaInformation()` plus one public `printXxx()` function per report section |
| `src/main/kotlin/neon/jvm/Main.kt` | `main()` for running the jar directly (`java -jar`) |
| `src/test/kotlin/JVMUtil.kt` | JUnit 4 tests |
| `jitpack-test/` | Standalone Gradle consumer project that uses a released version from JitPack (not part of the main build) |
| `.github/workflows/test.yml` | Tests on Linux, Windows and macOS on every push and release |
| `.github/workflows/jitpack.yml` | After each release: checks the version is usable from JitPack (retries up to 10 × 1 min) |
| `.claude/skills/release/` | `/release` skill that runs the release process below |

## Tech stack

- Gradle wrapper **9.9.2**, Groovy DSL (`build.gradle`, `settings.gradle`)
- Kotlin **2.4.21**, Java toolchain **25** (latest LTS). The Foojay resolver in `settings.gradle` downloads the JDK if missing.
- JUnit **4.13.2**
- Published via **JitPack** as `com.github.NeonMika:neon-util:<tag>`. The Gradle `group`/`version` (`neon` / `x.y.z`) are
  overridden by JitPack.

## Building and verifying

```shell
./gradlew clean build                                     # compile, test, jars, runtime libs in build/libs
(cd build/libs && java -jar neon-util-<version>.jar)      # run the jar directly
./gradlew clean build --rerun-tasks --configuration-cache # configuration cache compatibility
./gradlew clean -Pgroup=com.github.NeonMika -Pversion=vX.Y.Z -xtest assemble publishToMavenLocal   # what JitPack runs
./gradlew -p jitpack-test run -PneonUtilVersion=<tag-or-commit>   # consume a published version from JitPack
```

Gradle hides test stdout. Read it with `./gradlew test --rerun -i` or in `build/reports/tests/test/index.html`.

## Conventions

### Code
- **New report section**: add a public `fun printXxxInformation()` in `JVMUtil.kt` that starts with `printSectionHeader("…")`
  and has KDoc. Then:
  1. call it from `printFullJavaInformation()`,
  2. add it to the `sections` list and add a test in `src/test/kotlin/JVMUtil.kt`,
  3. add a row to the function table in the README.
- Use the existing helpers `printValue`, `formatBytes`, `MemoryUsage.toListString()`, `joinToListString()`.
- Sections must not crash when something is unavailable (JRE without compiler/JDI, non-HotSpot VM, missing JFR). Print
  "not available" instead.
- **Environment variables are sensitive**: `printFullJavaInformation(printEnvironment = false)` leaves them out by default,
  and `printEnvironmentInformation()` masks secret-looking names. Keep both behaviors.
- Java callers use `neon.jvm.JVMUtilKt.<function>()`. Public functions with default parameters need `@JvmOverloads`.

### Build script
- `copyRuntimeLibrariesToLib` copies runtime dependencies into `build/libs` so the manifest `Class-Path` resolves when running
  the jar directly. Because it shares `build/libs` with the jars, **every `Jar` task depends on it**. Without that, publishing
  (and therefore JitPack) fails with an implicit-dependency error.
- Keep the build **configuration-cache compatible**: don't use `project` or call build-script methods inside `doFirst`/`doLast`.
  Capture values (providers) at configuration time. Test with `--rerun-tasks`, since up-to-date tasks don't run their actions
  and hide these problems.
- The build prints which Java runs Gradle, compiles and tests. Keep that output.

### Tests
- Tests run on Windows too: `println` writes `\r\n` there. Normalize with `replace(System.lineSeparator(), "\n")` before
  matching multi-line output.
- Test output is also echoed to stdout, because the printed report from CI runners is useful.

### Git
- One commit per logical change, with a descriptive message. Commits made by an agent end with its `Co-Authored-By` line.
- Changes to code, the build or workflows go through a branch and a PR, so CI runs before merging (see below).
  Documentation-only changes (README, AGENTS.md, skills) may be committed to `main` directly.

## Versioning

- Tags are `vX.Y.Z`. The version in `build.gradle` is `X.Y.Z`.
- While below 1.0: breaking API changes bump the **minor** version (0.0.7 → 0.1.0). Everything else bumps the **patch** version.
  The maintainer decides when unsure (e.g. a changed default output went out as 0.1.1).
- On a version bump, update all occurrences:
  - `build.gradle` `version`
  - the README example tag ("e.g. `vX.Y.Z`")
  - the README JitPack command examples
  - "Since X.Y.Z" notes in the README's "Upgrading" section, if the release changes behavior

## Release process

Run it with the `/release` skill. The steps and the reasons behind them:

1. **Prepare on a branch** `release/vX.Y.Z` from an up-to-date `main`: version bump and README updates.
2. **Verify locally**: `clean build`, the JitPack command, the configuration-cache build, and `java -jar`.
3. **Push and open a PR**. Write the PR description by hand: summary, changes, tests. Mark breaking changes clearly.
4. **Wait for CI yourself.** Auto-merge is disabled in the repository and `main` has no required checks, so
   `gh pr merge --auto` doesn't wait for anything. Watch the `Test` run **for the PR's head commit**
   (`gh run list --workflow test.yml --commit <sha>`) and merge only if all three OS jobs pass. If one fails, fix it on the
   branch and push again. Never merge a red build.
5. **Merge with a merge commit** (`gh pr merge N --merge --delete-branch`), so the individual commits stay in history.
6. **Create the GitHub release** on the merge commit:
   `gh release create vX.Y.Z --target <merge-sha> --title "Version X.Y.Z" --notes "…"`. Notes are a short hand-written bullet
   list of user-visible changes, breaking changes first in a `> [!IMPORTANT]` block, ending with `Full changes: #<PR>`
   (GitHub links `#N` automatically).
7. **Watch the release-triggered workflows**: `Test` and `Test JitPack`. JitPack builds a version lazily on first request,
   and `Test JitPack` triggers and waits for that build.

### Pitfalls from past releases
- **Don't request a tag from JitPack before it exists on GitHub.** JitPack caches the "tag not found" answer for a while
  (happened with v0.0.7). Let `Test JitPack` do the first request, since it runs after the tag exists.
- **A broken release stays broken.** Don't move or re-tag a published version. Fix it in a new patch release and add a
  `> [!WARNING]` note to the broken release pointing to the fix (v0.0.6 → v0.0.7).
- The JitPack build log is at `https://jitpack.io/com/github/NeonMika/neon-util/<tag>/build.log`, and the status at
  `https://jitpack.io/api/builds/com.github.NeonMika/neon-util/<tag>`.
- `jitpack-test` calls `printFullJavaInformation()`, so it only works for versions ≥ 0.1.0.
