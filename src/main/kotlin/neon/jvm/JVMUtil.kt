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

    fun String.splitPaths() = replace(":\\", "####").replace(":",";").replace("####", ":\\").split(";")

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
    // println("pid: ${runtime.pid}") // since Java 10
    println("Classpath:\n${runtime.classPath.splitPaths().joinToListString()}")
    println("Library path:\n${runtime.libraryPath.splitPaths().joinToListString()}")
    println("isBootClassPathSupported:\n ${runtime.isBootClassPathSupported}")
    if (runtime.isBootClassPathSupported) println(
        "Boot Classpath:\n${
            runtime.bootClassPath.splitPaths().joinToListString()
        }}"
    )
    println()
    println("Spec name:\n  ${runtime.specName}")
    println("Spec Vendor:\n  ${runtime.specVendor}")
    println("Spec Version:\n  ${runtime.specVersion}")
    println("Management Spec Version:\n ${runtime.managementSpecVersion}")
    println()
    println("Start time:\n  ${runtime.startTime}")
    println("Uptime:\n  ${runtime.uptime}")
    println()
    println("VM Name:\n  ${runtime.vmName}")
    println("VM vendor:\n  ${runtime.vmVendor}")
    println("VM version:\n  ${runtime.vmVersion}")
    println("==================================")
    println("Java compiler information:")
    println()
    val compiler = ToolProvider.getSystemJavaCompiler()
    println("Name:\n " + compiler.name())
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