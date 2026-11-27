import neon.jvm.JVMUtilKt;

/**
 * Uses neon-util as a JitPack dependency. Running it checks that the library and its transitive
 * dependencies (e.g. kotlin-stdlib) are resolvable and work at runtime.
 * Requires neon-util 0.1.0 or newer (printJavaInformation() was renamed to printFullJavaInformation()).
 */
public class JitPackTest {
    public static void main(String[] args) {
        JVMUtilKt.printFullJavaInformation();
    }
}
