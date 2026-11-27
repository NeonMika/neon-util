# neon-util

[![Test](https://github.com/NeonMika/neon-util/actions/workflows/test.yml/badge.svg)](https://github.com/NeonMika/neon-util/actions/workflows/test.yml)
[![JitPack](https://jitpack.io/v/NeonMika/neon-util.svg)](https://jitpack.io/#NeonMika/neon-util)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL_v2.1-blue.svg)](LICENSE)

A small Kotlin/JVM library that answers **"Which JVM am I *actually* running on, and how is it configured?"** with a single call.

```kotlin
import neon.jvm.printJavaInformation

fun main() {
    printJavaInformation()
}
```

It prints a detailed report of the running JVM to `stdout`: runtime and VM details, the effective HotSpot flags, memory and GC
setup, threads, OS resources, encodings, environment and more. It is meant for debugging those "works on my machine" problems,
such as an unexpected JDK being picked up by the IDE or Gradle, a container limiting memory or CPUs, a different garbage
collector, or a wrong default charset.

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

Replace the version placeholder with a release tag (e.g. `v0.0.5`), a commit hash, or `main-SNAPSHOT`.

### Requirements

- **Java 25** or newer (the library is compiled with a Java 25 toolchain).
- A **full JDK**, not just a JRE: the report uses the `java.compiler` (`javax.tools`) and `jdk.jdi` modules.

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

```kotlin
import neon.jvm.printJavaInformation

printJavaInformation()
```

### From Java

Top-level Kotlin functions are compiled into a class named after the file:

```java
import neon.jvm.JVMUtilKt;

public class Main {
    public static void main(String[] args) {
        JVMUtilKt.printJavaInformation();
    }
}
```

### In a test

A common use is a test that prints the environment of a CI runner or of the IDE's test runner:

```kotlin
class EnvironmentTest {
    @Test
    fun printEnvironment() = printJavaInformation()
}
```

## What is reported

| Section | Source | Contents |
|---|---|---|
| Java runtime | `RuntimeMXBean` | VM name, vendor and version, VM info (execution mode, CDS), PID, input arguments, system properties, class/library/boot path, spec versions, start time, uptime |
| Operating system | `OperatingSystemMXBean` | OS name, version, architecture, processors, load average, CPU load, physical/swap/virtual memory, open file descriptors |
| Memory | `MemoryMXBean` | Heap and non-heap usage (init, used, committed, max) |
| Memory pools | `MemoryPoolMXBean` | Every pool (Eden, Survivor, Old Gen, Metaspace, CodeCache, …) with type, usage, peak usage and managing collectors |
| Garbage collectors | `GarbageCollectorMXBean` | Active collectors with collection count and time |
| Memory managers | `MemoryManagerMXBean` | All memory managers and the pools they manage |
| Threads | `ThreadMXBean` | Live/daemon/peak/started counts, deadlocks, monitoring capabilities, list of live threads |
| Class loading | `ClassLoadingMXBean` | Loaded, total loaded and unloaded classes |
| JIT compilation | `CompilationMXBean` | JIT compiler name and total compilation time |
| Buffer pools | `BufferPoolMXBean` | Direct and mapped buffers: count, memory used, capacity |
| HotSpot VM flags | `HotSpotDiagnosticMXBean` | Effective values and origins of key flags (GC selection, heap sizes, compressed oops, container support, …) and all non-default manageable flags |
| Java Flight Recorder | `FlightRecorderMXBean` | Availability and active recordings |
| Runtime version and resources | `Runtime` | Parsed version (feature/interim/update/patch/build), container-aware processor count and memory limits |
| Process | `ProcessHandle` | PID, parent PID, user, executable, command line, start instant, CPU time |
| Locale, charset and time zone | `Charset`, `Locale`, `ZoneId` | Default charset and encodings (`file.encoding`, `native.encoding`, `stdout.encoding`, `sun.jnu.encoding`), locales, time zone, line separator |
| Modules | `ModuleLayer` | Modules resolved in the boot layer |
| Environment variables | `System.getenv()` | All environment variables, `PATH` split into entries. Values whose names look like secrets (`TOKEN`, `SECRET`, `PASSWORD`, `KEY`, `AUTH`, …) are masked. |
| Java compiler | `javax.tools.ToolProvider` | System Java compiler and supported source versions |
| JDI | `com.sun.jdi.Bootstrap` | JDI interface version and the VM version of a debuggee launched through the default connector |

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

> **Note:** The report contains system properties, paths, the command line and environment variables. Even with masking of
> secret-looking environment variables, review the output before posting it publicly.

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

## License

[GNU Lesser General Public License v2.1](LICENSE)
