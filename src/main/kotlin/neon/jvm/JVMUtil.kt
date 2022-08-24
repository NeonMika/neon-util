package neon.jvm

import com.sun.jdi.Bootstrap
import java.lang.management.ManagementFactory
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
        println("VM version of debuggee: ${vm.version()}")
        vm.exit(0)
    } catch (ex: Exception) {
        println("Could not determine debuggee VM version.\nException: $ex")
    }
}

fun printJavaInformation() {
    val runtime = ManagementFactory.getRuntimeMXBean()

    println("==================================")
    println("==================================")
    println("======== JAVA INFORMATION ========")
    println("==================================")
    println("==================================")
    println("Java Runtime running the debugger:")
    println()
    println("Name: ${runtime.name}")
    println("Input arguments:\n${runtime.inputArguments.joinToString("\n")}")
    println("System properties:\n${runtime.systemProperties.entries.joinToString("\n")}")
    println()
    // println("pid: ${runtime.pid}") // since Java 10
    println("Classpath:\n${runtime.classPath.split(";").joinToString("\n")}")
    println("Library path:\n${runtime.libraryPath.split(";").joinToString("\n")}")
    println("isBootClassPathSupported: ${runtime.isBootClassPathSupported}")
    if (runtime.isBootClassPathSupported) println("Boot Classpath:\n${runtime.bootClassPath.split(";").joinToString("\n")}}")
    println()
    println("Spec name: ${runtime.specName}")
    println("Spec Vendor: ${runtime.specVendor}")
    println("Spec Version: ${runtime.specVersion}")
    println("Management Spec Version: ${runtime.managementSpecVersion}")
    println()
    println("Start time: ${runtime.startTime}")
    println("Uptime: ${runtime.uptime}")
    println()
    println("VM Name: ${runtime.vmName}")
    println("VM vendor: ${runtime.vmVendor}")
    println("VM version: ${runtime.vmVersion}")
    println("==================================")
    println("Java compiler information:")
    println()
    val compiler = ToolProvider.getSystemJavaCompiler()
    println("Name: " + compiler.name())
    println("Supported version: " + compiler.sourceVersions)
    println("==================================")
    println("JDI information:")
    println()
    println("Virtual Machine Manager Major Interface Version: ${Bootstrap.virtualMachineManager().majorInterfaceVersion()}")
    println("Virtual Machine Manager Minor Interface Version: ${Bootstrap.virtualMachineManager().minorInterfaceVersion()}")
    printDebuggerVersionByConnecting()
    println("==================================")
    println()
}