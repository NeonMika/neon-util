package neon.jvm

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class JVMUtil {
    /** Runs [block], returns everything it printed to stdout and also forwards it to the original stdout. */
    private fun captureOutput(block: () -> Unit): String {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        System.setOut(PrintStream(buffer, true))
        try {
            block()
        } finally {
            System.setOut(original)
        }
        val output = buffer.toString()
        print(output)
        return output
    }

    private fun assertPrintsSection(title: String, block: () -> Unit) {
        val output = captureOutput(block)
        assertTrue("Expected section header '$title:' in output", output.contains("$title:"))
    }

    private val sections = listOf<Pair<String, () -> Unit>>(
        "Java runtime information" to ::printRuntimeInformation,
        "Operating system information" to ::printOperatingSystemInformation,
        "Memory information" to ::printMemoryInformation,
        "Memory pool information" to ::printMemoryPoolInformation,
        "Garbage collector information" to ::printGarbageCollectorInformation,
        "Memory manager information" to ::printMemoryManagerInformation,
        "Thread information" to ::printThreadInformation,
        "Class loading information" to ::printClassLoadingInformation,
        "JIT compilation information" to ::printJitCompilationInformation,
        "Buffer pool information" to ::printBufferPoolInformation,
        "HotSpot VM flags" to ::printHotSpotVmFlags,
        "Java Flight Recorder information" to ::printFlightRecorderInformation,
        "Runtime version and resources" to ::printRuntimeResourceInformation,
        "Process information" to ::printProcessInformation,
        "Locale, charset and time zone information" to ::printLocalizationInformation,
        "Module information" to ::printModuleInformation,
        "Environment variables" to ::printEnvironmentInformation,
        "Java compiler information" to ::printJavaCompilerInformation,
        "JDI information" to ::printJdiInformation,
    )

    @Test
    fun fullJavaInformationContainsAllSectionsInOrder() {
        val output = captureOutput { printFullJavaInformation() }
        var lastIndex = -1
        for ((title, _) in sections) {
            val index = output.indexOf("\n$title:\n")
            assertTrue("Expected section '$title' in full output", index >= 0)
            assertTrue("Expected section '$title' after the previous section", index > lastIndex)
            lastIndex = index
        }
    }

    @Test
    fun runtimeInformation() = assertPrintsSection("Java runtime information", ::printRuntimeInformation)

    @Test
    fun operatingSystemInformation() = assertPrintsSection("Operating system information", ::printOperatingSystemInformation)

    @Test
    fun memoryInformation() = assertPrintsSection("Memory information", ::printMemoryInformation)

    @Test
    fun memoryPoolInformation() = assertPrintsSection("Memory pool information", ::printMemoryPoolInformation)

    @Test
    fun garbageCollectorInformation() = assertPrintsSection("Garbage collector information", ::printGarbageCollectorInformation)

    @Test
    fun memoryManagerInformation() = assertPrintsSection("Memory manager information", ::printMemoryManagerInformation)

    @Test
    fun threadInformation() = assertPrintsSection("Thread information", ::printThreadInformation)

    @Test
    fun classLoadingInformation() = assertPrintsSection("Class loading information", ::printClassLoadingInformation)

    @Test
    fun jitCompilationInformation() = assertPrintsSection("JIT compilation information", ::printJitCompilationInformation)

    @Test
    fun bufferPoolInformation() = assertPrintsSection("Buffer pool information", ::printBufferPoolInformation)

    @Test
    fun hotSpotVmFlags() = assertPrintsSection("HotSpot VM flags", ::printHotSpotVmFlags)

    @Test
    fun flightRecorderInformation() = assertPrintsSection("Java Flight Recorder information", ::printFlightRecorderInformation)

    @Test
    fun runtimeResourceInformation() = assertPrintsSection("Runtime version and resources", ::printRuntimeResourceInformation)

    @Test
    fun processInformation() = assertPrintsSection("Process information", ::printProcessInformation)

    @Test
    fun localizationInformation() = assertPrintsSection("Locale, charset and time zone information", ::printLocalizationInformation)

    @Test
    fun moduleInformation() = assertPrintsSection("Module information", ::printModuleInformation)

    @Test
    fun environmentInformation() = assertPrintsSection("Environment variables", ::printEnvironmentInformation)

    @Test
    fun environmentSecretsAreMasked() {
        val output = captureOutput { printEnvironmentInformation() }
        System.getenv()
            .filterKeys { Regex("(?i)(TOKEN|SECRET|PASSWORD)").containsMatchIn(it) }
            .filterValues { it.length >= 4 }
            .forEach { (name, value) -> assertTrue("Value of $name must be masked", !output.contains("$name=$value")) }
    }

    @Test
    fun javaCompilerInformation() = assertPrintsSection("Java compiler information", ::printJavaCompilerInformation)

    @Test
    fun jdiInformation() = assertPrintsSection("JDI information", ::printJdiInformation)

    @Test
    fun debuggerVersionByConnecting() {
        val output = captureOutput { printDebuggerVersionByConnecting() }
        assertTrue(
            "Expected debuggee VM version or error message",
            output.contains("VM version of debuggee:") || output.contains("Could not determine debuggee VM version.")
        )
    }
}
