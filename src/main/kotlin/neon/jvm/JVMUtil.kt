package neon.jvm

import com.sun.jdi.Bootstrap
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.time.Duration
import java.time.Instant
import javax.tools.ToolProvider

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
    printOperatingSystemInformation()
    printMemoryInformation()
    printMemoryPoolInformation()
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
