# neon-util

[![Test](https://github.com/NeonMika/neon-util/actions/workflows/test.yml/badge.svg)](https://github.com/NeonMika/neon-util/actions/workflows/test.yml)
[![JitPack](https://jitpack.io/v/NeonMika/neon-util.svg)](https://jitpack.io/#NeonMika/neon-util)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL_v2.1-blue.svg)](LICENSE)

A small Kotlin/JVM library that answers **"Which JVM am I *actually* running on, and how is it configured?"** with a single call.

```kotlin
import neon.jvm.printFullJavaInformation

fun main() {
    printFullJavaInformation()
}
```

It prints a detailed report of the running JVM to `stdout`: runtime and VM details, the effective HotSpot flags, memory and GC
setup, threads, OS resources, encodings, environment and more. It is meant for debugging those "works on my machine" problems,
such as an unexpected JDK being picked up by the IDE or Gradle, a container limiting memory or CPUs, a different garbage
collector, or a wrong default charset.

Each part of the report is also available on its own, e.g. `printHotSpotVmFlags()` or `printMemoryInformation()`
(see [What is reported](#what-is-reported)).

## Installation

The library is published via [JitPack](https://jitpack.io/#NeonMika/neon-util).

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.NeonMika:neon-util:<version>")
}
```

**Gradle (Groovy DSL)**

```groovy
repositories {
    mavenCentral()
    maven { url = 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.NeonMika:neon-util:<version>'
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.NeonMika</groupId>
    <artifactId>neon-util</artifactId>
    <version>VERSION</version>
</dependency>
```

Replace the version placeholder with a release tag (e.g. `v0.1.1`), a commit hash, or `main-SNAPSHOT`.

### Requirements

- **Java 25** or newer (the library is compiled with a Java 25 toolchain).
- A **full JDK** is recommended: on a JRE, the Java compiler and JDI sections report that they are not available.

## Usage

### As a standalone jar

The jar is executable. Build it and run it with the JVM you want to inspect:

```shell
./gradlew build
java -jar build/libs/neon-util-<version>.jar
```

`build/libs` contains the jar together with its runtime dependencies (`kotlin-stdlib`, `annotations`), which are referenced via the
manifest's `Class-Path`. To use it elsewhere, copy the whole `build/libs` folder, e.g. onto a server or into a container, and run
`java -jar` there.

### From Kotlin

All functions are top-level functions in the package `neon.jvm`:

```kotlin
import neon.jvm.*

printFullJavaInformation()       // the complete report, without environment variables
printFullJavaInformation(printEnvironment = true)  // the complete report, including environment variables

printRuntimeInformation()        // or only the parts you are interested in
printGarbageCollectorInformation()
printHotSpotVmFlags()
```

### From Java

Top-level Kotlin functions are compiled into a class named after the file:

```java
import neon.jvm.JVMUtilKt;

public class Main {
    public static void main(String[] args) {
        JVMUtilKt.printFullJavaInformation();      // without environment variables
        JVMUtilKt.printFullJavaInformation(true);  // including environment variables
        JVMUtilKt.printHotSpotVmFlags();
    }
}
```

### In a test

A common use is a test that prints the environment of a CI runner or of the IDE's test runner:

```kotlin
class EnvironmentTest {
    @Test
    fun printJvmInformation() = printFullJavaInformation()
}
```

## What is reported

`printFullJavaInformation()` prints all of the following sections in this order, except for the environment variables. Those are
only included with `printFullJavaInformation(printEnvironment = true)`, since they often contain sensitive values. Each section
can also be printed on its own with the function in the first column.

| Function | Source | Contents |
|---|---|---|
| `printRuntimeInformation()` | `RuntimeMXBean` | VM name, vendor and version, VM info (execution mode, CDS), PID, input arguments, system properties, class/library/boot path, spec versions, start time, uptime |
| `printOperatingSystemInformation()` | `OperatingSystemMXBean` | OS name, version, architecture, processors, load average, CPU load, physical/swap/virtual memory, open file descriptors |
| `printMemoryInformation()` | `MemoryMXBean` | Heap and non-heap usage (init, used, committed, max) |
| `printMemoryPoolInformation()` | `MemoryPoolMXBean` | Every pool (Eden, Survivor, Old Gen, Metaspace, CodeCache, …) with type, usage, peak usage and managing collectors |
| `printGarbageCollectorInformation()` | `GarbageCollectorMXBean` | Active collectors with collection count and time |
| `printMemoryManagerInformation()` | `MemoryManagerMXBean` | All memory managers and the pools they manage |
| `printThreadInformation()` | `ThreadMXBean` | Live/daemon/peak/started counts, deadlocks, monitoring capabilities, list of live threads |
| `printClassLoadingInformation()` | `ClassLoadingMXBean` | Loaded, total loaded and unloaded classes |
| `printJitCompilationInformation()` | `CompilationMXBean` | JIT compiler name and total compilation time |
| `printBufferPoolInformation()` | `BufferPoolMXBean` | Direct and mapped buffers: count, memory used, capacity |
| `printHotSpotVmFlags()` | `HotSpotDiagnosticMXBean` | Effective values and origins of key flags (GC selection, heap sizes, compressed oops, container support, …) and all non-default manageable flags |
| `printFlightRecorderInformation()` | `FlightRecorderMXBean` | Availability and active recordings |
| `printRuntimeResourceInformation()` | `Runtime` | Parsed version (feature/interim/update/patch/build), container-aware processor count and memory limits |
| `printProcessInformation()` | `ProcessHandle` | PID, parent PID, user, executable, command line, start instant, CPU time |
| `printLocalizationInformation()` | `Charset`, `Locale`, `ZoneId` | Default charset and encodings (`file.encoding`, `native.encoding`, `stdout.encoding`, `sun.jnu.encoding`), locales, time zone, line separator |
| `printModuleInformation()` | `ModuleLayer` | Modules resolved in the boot layer |
| `printEnvironmentInformation()` | `System.getenv()` | *Only with `printEnvironment = true`.* All environment variables, `PATH` split into entries. Values whose names look like secrets (`TOKEN`, `SECRET`, `PASSWORD`, `KEY`, `AUTH`, …) are masked. |
| `printJavaCompilerInformation()` | `javax.tools.ToolProvider` | System Java compiler and supported source versions |
| `printJdiInformation()` | `com.sun.jdi.Bootstrap` | JDI interface version and the VM version of a debuggee launched through the default connector |

### Example output (excerpt)

```text
==================================
Garbage collector information:

G1 Young Generation:
  Valid: true
  Collection count: 0
  Collection time: PT0S
  Memory pools: G1 Eden Space, G1 Survivor Space, G1 Old Gen
...
==================================
JIT compilation information:

Name:
  HotSpot 64-Bit Tiered Compilers
Compilation time monitoring supported:
  true
Total compilation time:
  PT0.806S
==================================
HotSpot VM flags:

  - UseG1GC = true (origin: ERGONOMIC)
  - MaxHeapSize = 536870912 (origin: VM_CREATION)
  - UseCompressedOops = true (origin: ERGONOMIC)
  - UseContainerSupport = true (origin: DEFAULT)
  ...
```

> **Note:** The report contains system properties, paths and the command line, and optionally environment variables. Even with
> masking of secret-looking environment variables, review the output before posting it publicly.

## Upgrading

Version 0.1.0 renamed `printJavaInformation()` to `printFullJavaInformation()` (in Java: `JVMUtilKt.printFullJavaInformation()`).
The report now starts with a "Java runtime information" section header, like all other sections.

Since 0.1.1, `printFullJavaInformation()` no longer prints the environment variables by default. Pass `printEnvironment = true`
(in Java: `JVMUtilKt.printFullJavaInformation(true)`) to include them.

## Building

The project uses the Gradle wrapper. You don't need a local Gradle installation. If no JDK 25 is installed, the
[Foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) downloads one automatically.

```shell
./gradlew build                # compile, test and package (incl. sources and javadoc jars, plus runtime libraries in build/libs)
./gradlew test                 # run the tests
./gradlew publishToMavenLocal  # install into ~/.m2 for local use
```

The build prints which Java is used to run Gradle, to compile and to test, since these can differ.

Gradle does not show the test output (the JVM report) on the console. You find it in the HTML test report at
`build/reports/tests/test/index.html`. Alternatively, run `./gradlew test --info`.

On every push and every published release, CI runs the tests on Linux, Windows and macOS (see
[`.github/workflows/test.yml`](.github/workflows/test.yml)). The test reports of each platform are uploaded as workflow
artifacts.

After every published release, the [Test JitPack](.github/workflows/jitpack.yml) workflow checks that the release can actually
be used from JitPack. A minimal Gradle consumer project ([`jitpack-test/`](jitpack-test)) declares a dependency on the new
version and runs code from it. JitPack builds new versions on their first request, so the check retries once a minute, up to
10 attempts. It can also be started manually for any tag or commit from 0.1.0 on:

```shell
gh workflow run jitpack.yml -f version=v0.1.0
./gradlew -p jitpack-test run -PneonUtilVersion=v0.1.0   # the same check, locally
```

## License

[GNU Lesser General Public License v2.1](LICENSE)
