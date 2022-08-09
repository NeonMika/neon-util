package neon.jvm

import org.junit.Test

class JVMUtil {
    @Test
    fun noErrorWhilePrintingJavaInformation() {
        printJavaInformation()
    }

    @Test
    fun noErrorWhilePrintingDebuggerInformation() {
        printDebuggerVersionByConnecting()
    }
}