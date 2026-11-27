package neon.jvm

import com.sun.jdi.Bootstrap
import com.sun.management.HotSpotDiagnosticMXBean
import com.sun.management.VMOption
import java.io.File
import java.lang.management.BufferPoolMXBean
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.tools.ToolProvider
import jdk.management.jfr.FlightRecorderMXBean

/**
 * Prints all available information about the running JVM, i.e., all sections below in this order.
 *
 * @param printEnvironment whether to include the environment variables ([printEnvironmentInformation]). Off by default, since
 * environment variables often contain sensitive values that should not end up in logs, even though secret-looking ones are masked.
 */
@JvmOverloads
fun printFullJavaInformation(printEnvironment: Boolean = false) {
    println()
    println("|==================================|")
    println("|======== JAVA INFORMATION ========|")
    println("|==================================|")
    printRuntimeInformation()
    printOperatingSystemInformation()
    printMemoryInformation()
    printMemoryPoolInformation()
    printGarbageCollectorInformation()
    printMemoryManagerInformation()
    printThreadInformation()
    printClassLoadingInformation()
    printJitCompilationInformation()
    printBufferPoolInformation()
    printHotSpotVmFlags()
    printFlightRecorderInformation()
    printRuntimeResourceInformation()
    printProcessInformation()
    printLocalizationInformation()
    printModuleInformation()
    if (printEnvironment) {
        printEnvironmentInformation()
    }
    printJavaCompilerInformation()
    printJdiInformation()
    println("==================================")
    println()
}

private fun Collection<*>.joinToListString() = joinToString("\n") { "  - $it" }

private fun String.splitPaths() = split(File.pathSeparator).filter { it.isNotEmpty() }

/**
 * Prints the runtime information ([java.lang.management.RuntimeMXBean]): VM name, vendor and version, PID, input arguments,
 * system properties, class/library/boot path, spec versions, start time and uptime.
 */
fun printRuntimeInformation() {
    val runtime = ManagementFactory.getRuntimeMXBean()
    printSectionHeader("Java runtime information")
    printValue("Name", runtime.name)
    println("Input arguments:\n${runtime.inputArguments.joinToListString()}")
    println("System properties:\n${runtime.systemProperties.entries.joinToListString()}")
    println()
    printValue("PID", runtime.pid)
    println("Classpath:\n${runtime.classPath.splitPaths().joinToListString()}")
    println("Library path:\n${runtime.libraryPath.splitPaths().joinToListString()}")
    printValue("isBootClassPathSupported", runtime.isBootClassPathSupported)
    if (runtime.isBootClassPathSupported) {
        println("Boot Classpath:\n${runtime.bootClassPath.splitPaths().joinToListString()}")
    }
    println()
    printValue("Spec name", runtime.specName)
    printValue("Spec Vendor", runtime.specVendor)
    printValue("Spec Version", runtime.specVersion)
    printValue("Management Spec Version", runtime.managementSpecVersion)
    println()
    printValue("Start time", "${Instant.ofEpochMilli(runtime.startTime)} (${runtime.startTime} ms since epoch)")
    printValue("Uptime", "${Duration.ofMillis(runtime.uptime)} (${runtime.uptime} ms)")
    println()
    printValue("VM Name", runtime.vmName)
    printValue("VM vendor", runtime.vmVendor)
    printValue("VM version", runtime.vmVersion)
    printValue("VM info (execution mode, class data sharing)", System.getProperty("java.vm.info"))
}

/**
 * Prints the system Java compiler ([javax.tools.ToolProvider]) and its supported source versions.
 * Only available when running on a JDK, not on a JRE.
 */
fun printJavaCompilerInformation() {
    printSectionHeader("Java compiler information")
    val compiler = ToolProvider.getSystemJavaCompiler()
    if (compiler == null) {
        println("No system Java compiler available (running on a JRE?)")
        return
    }
    printValue("Name", compiler.name())
    printValue("Supported versions", compiler.sourceVersions)
}

/**
 * Prints the Java Debug Interface (JDI) version and the VM version of a debuggee launched via the default connector.
 * Requires the `jdk.jdi` module.
 */
fun printJdiInformation() {
    printSectionHeader("JDI information")
    val virtualMachineManager = try {
        Bootstrap.virtualMachineManager()
    } catch (e: Throwable) {
        println("JDI not available: $e")
        return
    }
    printValue("Virtual Machine Manager Major Interface Version", virtualMachineManager.majorInterfaceVersion())
    printValue("Virtual Machine Manager Minor Interface Version", virtualMachineManager.minorInterfaceVersion())
    printDebuggerVersionByConnecting()
}

internal fun printDebuggerVersionByConnecting() {
    try {
        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val arguments = launchingConnector.defaultArguments()
        arguments["main"]!!.setValue("NOPE")
        arguments["options"]!!.setValue("-cp .")
        val vm = launchingConnector.launch(arguments)
        printValue("VM version of debuggee", vm.version())
        vm.exit(0)
    } catch (ex: Exception) {
        println("Could not determine debuggee VM version.\nException: $ex")
    }
}

private fun printSectionHeader(title: String) {
    println("==================================")
    println("$title:")
    println()
}

private fun printValue(label: String, value: Any?) = println("$label:\n  $value")

private fun Long.formatBytes(): String {
    if (this < 0) return "undefined ($this)"
    val units = listOf("B", "KiB", "MiB", "GiB", "TiB")
    var value = this.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$this B" else "%.2f %s (%d B)".format(value, units[unit], this)
}

private fun Double.formatLoad() = if (this < 0) "not available ($this)" else "%.2f %%".format(this * 100)

/**
 * Prints operating system information ([java.lang.management.OperatingSystemMXBean]): OS, CPU load, physical/swap/virtual memory, file descriptors.
 */
fun printOperatingSystemInformation() {
    val os = ManagementFactory.getOperatingSystemMXBean()
    printSectionHeader("Operating system information")
    printValue("Name", os.name)
    printValue("Version", os.version)
    printValue("Architecture", os.arch)
    printValue("Available processors", os.availableProcessors)
    printValue("System load average (last minute)", if (os.systemLoadAverage < 0) "not available" else "%.2f".format(os.systemLoadAverage))
    if (os is com.sun.management.OperatingSystemMXBean) {
        println()
        printValue("CPU load (whole system)", os.cpuLoad.formatLoad())
        printValue("CPU load (this process)", os.processCpuLoad.formatLoad())
        printValue("Process CPU time", Duration.ofNanos(os.processCpuTime))
        printValue("Total physical memory", os.totalMemorySize.formatBytes())
        printValue("Free physical memory", os.freeMemorySize.formatBytes())
        printValue("Total swap space", os.totalSwapSpaceSize.formatBytes())
        printValue("Free swap space", os.freeSwapSpaceSize.formatBytes())
        printValue("Committed virtual memory", os.committedVirtualMemorySize.formatBytes())
    }
    if (os is com.sun.management.UnixOperatingSystemMXBean) {
        printValue("Open file descriptors", "${os.openFileDescriptorCount} (max: ${os.maxFileDescriptorCount})")
    }
}

private fun MemoryUsage.toListString() = listOf(
    "init: ${init.formatBytes()}",
    "used: ${used.formatBytes()}",
    "committed: ${committed.formatBytes()}",
    "max: ${max.formatBytes()}"
).joinToString("\n") { "  - $it" }

/**
 * Prints heap and non-heap memory usage ([java.lang.management.MemoryMXBean]).
 */
fun printMemoryInformation() {
    val memory = ManagementFactory.getMemoryMXBean()
    printSectionHeader("Memory information")
    println("Heap memory usage:\n${memory.heapMemoryUsage.toListString()}")
    println("Non-heap memory usage:\n${memory.nonHeapMemoryUsage.toListString()}")
    @Suppress("DEPRECATION")
    printValue("Objects pending finalization", memory.objectPendingFinalizationCount)
}

/**
 * Prints all memory pools ([java.lang.management.MemoryPoolMXBean]) with usage, peak usage and managers.
 */
fun printMemoryPoolInformation() {
    printSectionHeader("Memory pool information")
    for (pool in ManagementFactory.getMemoryPoolMXBeans()) {
        println("${pool.name} (${pool.type}):")
        println("  Managed by: ${pool.memoryManagerNames.joinToString(", ")}")
        println("  Usage:")
        println(pool.usage?.toListString()?.prependIndent("  ") ?: "    not available (pool invalid)")
        println("  Peak usage:")
        println(pool.peakUsage?.toListString()?.prependIndent("  ") ?: "    not available (pool invalid)")
    }
}

/**
 * Prints the active garbage collectors ([GarbageCollectorMXBean]) with collection count and time.
 */
fun printGarbageCollectorInformation() {
    printSectionHeader("Garbage collector information")
    for (gc in ManagementFactory.getGarbageCollectorMXBeans()) {
        println("${gc.name}:")
        println("  Valid: ${gc.isValid}")
        println("  Collection count: ${gc.collectionCount}")
        println("  Collection time: ${if (gc.collectionTime < 0) "not available" else Duration.ofMillis(gc.collectionTime)}")
        println("  Memory pools: ${gc.memoryPoolNames.joinToString(", ")}")
    }
}

/**
 * Prints all memory managers ([java.lang.management.MemoryManagerMXBean]) and their pools.
 */
fun printMemoryManagerInformation() {
    printSectionHeader("Memory manager information")
    for (manager in ManagementFactory.getMemoryManagerMXBeans()) {
        val kind = if (manager is GarbageCollectorMXBean) "garbage collector" else "memory manager"
        println("${manager.name} ($kind):")
        println("  Valid: ${manager.isValid}")
        println("  Memory pools: ${manager.memoryPoolNames.joinToString(", ")}")
    }
}

/**
 * Prints thread counts, deadlocks, monitoring capabilities and live threads ([java.lang.management.ThreadMXBean]).
 */
fun printThreadInformation() {
    val threads = ManagementFactory.getThreadMXBean()
    printSectionHeader("Thread information")
    printValue("Live threads", threads.threadCount)
    printValue("Daemon threads", threads.daemonThreadCount)
    printValue("Peak threads", threads.peakThreadCount)
    printValue("Total started threads", threads.totalStartedThreadCount)
    val deadlocked = threads.findDeadlockedThreads()
    printValue("Deadlocked threads", deadlocked?.let { ids -> threads.getThreadInfo(ids).joinToString(", ") { it?.threadName ?: "?" } } ?: "none")
    println()
    printValue("Thread CPU time supported", threads.isThreadCpuTimeSupported)
    printValue("Current thread CPU time supported", threads.isCurrentThreadCpuTimeSupported)
    printValue("Thread CPU time enabled", if (threads.isThreadCpuTimeSupported) threads.isThreadCpuTimeEnabled else "n/a")
    printValue("Thread contention monitoring supported", threads.isThreadContentionMonitoringSupported)
    printValue("Thread contention monitoring enabled", if (threads.isThreadContentionMonitoringSupported) threads.isThreadContentionMonitoringEnabled else "n/a")
    printValue("Object monitor usage supported", threads.isObjectMonitorUsageSupported)
    printValue("Synchronizer usage supported", threads.isSynchronizerUsageSupported)
    if (threads is com.sun.management.ThreadMXBean && threads.isThreadAllocatedMemorySupported) {
        printValue("Total allocated memory (all live threads)", threads.totalThreadAllocatedBytes.formatBytes())
    }
    println("Threads:")
    for (info in threads.getThreadInfo(threads.allThreadIds).filterNotNull().sortedBy { it.threadId }) {
        println("  - [${info.threadId}] ${info.threadName} (${info.threadState}${if (info.isDaemon) ", daemon" else ""})")
    }
}

/**
 * Prints class loading statistics ([java.lang.management.ClassLoadingMXBean]).
 */
fun printClassLoadingInformation() {
    val classLoading = ManagementFactory.getClassLoadingMXBean()
    printSectionHeader("Class loading information")
    printValue("Currently loaded classes", classLoading.loadedClassCount)
    printValue("Total loaded classes", classLoading.totalLoadedClassCount)
    printValue("Unloaded classes", classLoading.unloadedClassCount)
    printValue("Verbose class loading", classLoading.isVerbose)
}

/**
 * Prints the JIT compiler and its total compilation time ([java.lang.management.CompilationMXBean]).
 */
fun printJitCompilationInformation() {
    printSectionHeader("JIT compilation information")
    val compilation = ManagementFactory.getCompilationMXBean()
    if (compilation == null) {
        println("No JIT compiler available")
        return
    }
    printValue("Name", compilation.name)
    printValue("Compilation time monitoring supported", compilation.isCompilationTimeMonitoringSupported)
    if (compilation.isCompilationTimeMonitoringSupported) {
        printValue("Total compilation time", Duration.ofMillis(compilation.totalCompilationTime))
    }
}

/**
 * Prints the direct and mapped buffer pools ([BufferPoolMXBean]).
 */
fun printBufferPoolInformation() {
    printSectionHeader("Buffer pool information")
    for (pool in ManagementFactory.getPlatformMXBeans(BufferPoolMXBean::class.java)) {
        println("${pool.name}:")
        println("  Buffer count: ${pool.count}")
        println("  Memory used: ${pool.memoryUsed.formatBytes()}")
        println("  Total capacity: ${pool.totalCapacity.formatBytes()}")
    }
}

private val INTERESTING_VM_FLAGS = listOf(
    "UseSerialGC", "UseParallelGC", "UseG1GC", "UseZGC", "UseShenandoahGC", "UseEpsilonGC",
    "InitialHeapSize", "MaxHeapSize", "MinHeapSize", "MaxDirectMemorySize", "MaxMetaspaceSize", "ReservedCodeCacheSize",
    "UseCompressedOops", "UseCompressedClassPointers", "UseCompactObjectHeaders",
    "UseContainerSupport", "ActiveProcessorCount", "MaxRAMPercentage", "InitialRAMPercentage",
    "TieredCompilation", "UseStringDeduplication", "UseLargePages", "UseNUMA",
    "HeapDumpOnOutOfMemoryError", "HeapDumpPath", "ExitOnOutOfMemoryError", "ThreadStackSize"
)

/**
 * Prints the effective values of important HotSpot VM flags and all non-default manageable flags ([HotSpotDiagnosticMXBean]).
 */
fun printHotSpotVmFlags() {
    printSectionHeader("HotSpot VM flags")
    val hotSpot = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)
    if (hotSpot == null) {
        println("HotSpotDiagnosticMXBean not available (not a HotSpot VM?)")
        return
    }
    for (name in INTERESTING_VM_FLAGS) {
        val option = try {
            hotSpot.getVMOption(name)
        } catch (_: IllegalArgumentException) {
            null // Flag does not exist in this VM version
        } ?: continue
        println("  - ${option.name} = ${option.value} (origin: ${option.origin}${if (option.isWriteable) ", writeable" else ""})")
    }
    val nonDefault = hotSpot.diagnosticOptions.filter { it.origin != VMOption.Origin.DEFAULT }
    println("Non-default writeable (manageable) flags:")
    println(if (nonDefault.isEmpty()) "  none" else nonDefault.joinToString("\n") { "  - ${it.name} = ${it.value} (origin: ${it.origin})" })
}

/**
 * Prints Java Flight Recorder availability and recordings ([FlightRecorderMXBean]).
 */
fun printFlightRecorderInformation() {
    printSectionHeader("Java Flight Recorder information")
    val flightRecorder = try {
        ManagementFactory.getPlatformMXBean(FlightRecorderMXBean::class.java)
    } catch (_: Throwable) {
        null // jdk.management.jfr module not present or JFR not supported
    }
    if (flightRecorder == null) {
        println("Java Flight Recorder not available")
        return
    }
    val recordings = flightRecorder.recordings
    println("Recordings:")
    println(
        if (recordings.isEmpty()) "  none"
        else recordings.joinToString("\n") { "  - [${it.id}] ${it.name} (state: ${it.state}, destination: ${it.destination ?: "-"})" }
    )
}

/**
 * Prints the parsed [Runtime.version] and the container-aware processor count and memory limits of [Runtime].
 */
fun printRuntimeResourceInformation() {
    val runtime = Runtime.getRuntime()
    val version = Runtime.version()
    printSectionHeader("Runtime version and resources")
    printValue("Version", version)
    printValue("Feature / interim / update / patch", "${version.feature()} / ${version.interim()} / ${version.update()} / ${version.patch()}")
    printValue("Pre-release", version.pre().orElse("-"))
    printValue("Build", version.build().map { it.toString() }.orElse("-"))
    printValue("Optional", version.optional().orElse("-"))
    println()
    printValue("Available processors (as seen by the JVM, container-aware)", runtime.availableProcessors())
    printValue("Max memory", runtime.maxMemory().let { if (it == Long.MAX_VALUE) "unlimited" else it.formatBytes() })
    printValue("Total memory", runtime.totalMemory().formatBytes())
    printValue("Free memory", runtime.freeMemory().formatBytes())
}

/**
 * Prints information about the current process ([ProcessHandle]).
 */
fun printProcessInformation() {
    val process = ProcessHandle.current()
    val info = process.info()
    printSectionHeader("Process information")
    printValue("PID", process.pid())
    printValue("Parent PID", process.parent().map { it.pid().toString() }.orElse("unknown"))
    printValue("User", info.user().orElse("unknown"))
    printValue("Executable", info.command().orElse("unknown"))
    printValue("Command line", info.commandLine().orElse("unknown"))
    printValue("Start instant", info.startInstant().map { it.toString() }.orElse("unknown"))
    printValue("Total CPU duration", info.totalCpuDuration().map { it.toString() }.orElse("unknown"))
    printValue("Child processes", process.children().count())
}

/**
 * Prints the default charset, encodings, locales, time zone and line separator.
 */
fun printLocalizationInformation() {
    printSectionHeader("Locale, charset and time zone information")
    printValue("Default charset", Charset.defaultCharset())
    printValue("file.encoding", System.getProperty("file.encoding"))
    printValue("native.encoding", System.getProperty("native.encoding"))
    printValue("stdout.encoding", System.getProperty("stdout.encoding"))
    printValue("sun.jnu.encoding (file names)", System.getProperty("sun.jnu.encoding"))
    printValue("Default locale", Locale.getDefault())
    printValue("Default locale (display)", Locale.getDefault(Locale.Category.DISPLAY))
    printValue("Default locale (format)", Locale.getDefault(Locale.Category.FORMAT))
    printValue("Default time zone", ZoneId.systemDefault())
    printValue("Line separator", System.lineSeparator().map { "\\u%04X".format(it.code) }.joinToString(""))
}

/**
 * Prints the modules resolved in the boot layer ([ModuleLayer.boot]).
 */
fun printModuleInformation() {
    printSectionHeader("Module information")
    val modules = ModuleLayer.boot().modules().sortedBy { it.name }
    println("Boot layer modules (${modules.size}):")
    println(modules.joinToString("\n") { module ->
        "  - ${module.name}${module.descriptor?.rawVersion()?.map { "@$it" }?.orElse("") ?: ""}"
    })
}

private val SECRET_ENV_NAME = Regex("(?i)(TOKEN|SECRET|PASSWORD|PASSWD|PWD_|CREDENTIAL|API_?KEY|PRIVATE_?KEY|ACCESS_?KEY|AUTH)")

/**
 * Prints all environment variables, with `PATH` split into entries and secret-looking values masked.
 */
fun printEnvironmentInformation() {
    printSectionHeader("Environment variables")
    val environment = System.getenv().toSortedMap()
    println(environment.entries.joinToString("\n") { (name, value) ->
        if (name.equals("PATH", ignoreCase = true)) {
            "  - $name:\n" + value.split(File.pathSeparator).filter { it.isNotEmpty() }.joinToString("\n") { "      $it" }
        } else {
            "  - $name=${if (SECRET_ENV_NAME.containsMatchIn(name)) "****** (masked)" else value}"
        }
    })
}
