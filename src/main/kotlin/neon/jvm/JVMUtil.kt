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

internal fun printDebuggerVersionByConnecting() {
    try {
        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val arguments = launchingConnector.defaultArguments()
        arguments["main"]!!.setValue("NOPE")
        arguments["options"]!!.setValue(
            "-cp ."
        )
        val vm = launchingConnector.launch(arguments)
        println("VM version of debuggee:\n  ${vm.version()}")
        vm.exit(0)
    } catch (ex: Exception) {
        println("Could not determine debuggee VM version.\nException: $ex")
    }
}

fun printJavaInformation() {
    val runtime = ManagementFactory.getRuntimeMXBean()


    fun Set<*>.joinToListString() = joinToString("\n") { "  - $it" }
    fun List<*>.joinToListString() = joinToString("\n") { "  - $it" }

    fun String.splitPaths() = split(File.pathSeparator).filter { it.isNotEmpty() }

    println()
    println("|==================================|")
    println("|======== JAVA INFORMATION ========|")
    println("|==================================|")
    println("Java Runtime:")
    println()
    println("Name:\n  ${runtime.name}")
    println("Input arguments:\n${runtime.inputArguments.joinToListString()}")
    println("System properties:\n${runtime.systemProperties.entries.joinToListString()}")
    println()
    println("PID:\n  ${runtime.pid}")
    println("Classpath:\n${runtime.classPath.splitPaths().joinToListString()}")
    println("Library path:\n${runtime.libraryPath.splitPaths().joinToListString()}")
    println("isBootClassPathSupported:\n  ${runtime.isBootClassPathSupported}")
    if (runtime.isBootClassPathSupported) println(
        "Boot Classpath:\n${
            runtime.bootClassPath.splitPaths().joinToListString()
        }"
    )
    println()
    println("Spec name:\n  ${runtime.specName}")
    println("Spec Vendor:\n  ${runtime.specVendor}")
    println("Spec Version:\n  ${runtime.specVersion}")
    println("Management Spec Version:\n  ${runtime.managementSpecVersion}")
    println()
    println("Start time:\n  ${Instant.ofEpochMilli(runtime.startTime)} (${runtime.startTime} ms since epoch)")
    println("Uptime:\n  ${Duration.ofMillis(runtime.uptime)} (${runtime.uptime} ms)")
    println()
    println("VM Name:\n  ${runtime.vmName}")
    println("VM vendor:\n  ${runtime.vmVendor}")
    println("VM version:\n  ${runtime.vmVersion}")
    println("VM info (execution mode, class data sharing):\n  ${System.getProperty("java.vm.info")}")
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
    println("==================================")
    println("Java compiler information:")
    println()
    val compiler = ToolProvider.getSystemJavaCompiler()
    println("Name:\n  " + compiler.name())
    println("Supported version:\n  " + compiler.sourceVersions)
    println("==================================")
    println("JDI information:")
    println()
    println("Virtual Machine Manager Major Interface Version:")
    println("  " + Bootstrap.virtualMachineManager().majorInterfaceVersion())
    println("Virtual Machine Manager Minor Interface Version:")
    println("  " + Bootstrap.virtualMachineManager().minorInterfaceVersion())
    printDebuggerVersionByConnecting()
    println("==================================")
    println()
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

private fun printOperatingSystemInformation() {
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

private fun printMemoryInformation() {
    val memory = ManagementFactory.getMemoryMXBean()
    printSectionHeader("Memory information")
    println("Heap memory usage:\n${memory.heapMemoryUsage.toListString()}")
    println("Non-heap memory usage:\n${memory.nonHeapMemoryUsage.toListString()}")
    @Suppress("DEPRECATION")
    printValue("Objects pending finalization", memory.objectPendingFinalizationCount)
}

private fun printMemoryPoolInformation() {
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

private fun printGarbageCollectorInformation() {
    printSectionHeader("Garbage collector information")
    for (gc in ManagementFactory.getGarbageCollectorMXBeans()) {
        println("${gc.name}:")
        println("  Valid: ${gc.isValid}")
        println("  Collection count: ${gc.collectionCount}")
        println("  Collection time: ${if (gc.collectionTime < 0) "not available" else Duration.ofMillis(gc.collectionTime)}")
        println("  Memory pools: ${gc.memoryPoolNames.joinToString(", ")}")
    }
}

private fun printMemoryManagerInformation() {
    printSectionHeader("Memory manager information")
    for (manager in ManagementFactory.getMemoryManagerMXBeans()) {
        val kind = if (manager is GarbageCollectorMXBean) "garbage collector" else "memory manager"
        println("${manager.name} ($kind):")
        println("  Valid: ${manager.isValid}")
        println("  Memory pools: ${manager.memoryPoolNames.joinToString(", ")}")
    }
}

private fun printThreadInformation() {
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

private fun printClassLoadingInformation() {
    val classLoading = ManagementFactory.getClassLoadingMXBean()
    printSectionHeader("Class loading information")
    printValue("Currently loaded classes", classLoading.loadedClassCount)
    printValue("Total loaded classes", classLoading.totalLoadedClassCount)
    printValue("Unloaded classes", classLoading.unloadedClassCount)
    printValue("Verbose class loading", classLoading.isVerbose)
}

private fun printJitCompilationInformation() {
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

private fun printBufferPoolInformation() {
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

private fun printHotSpotVmFlags() {
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

private fun printFlightRecorderInformation() {
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

private fun printRuntimeResourceInformation() {
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

private fun printProcessInformation() {
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

private fun printLocalizationInformation() {
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
