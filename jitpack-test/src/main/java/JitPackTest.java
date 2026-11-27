import neon.jvm.JVMUtilKt;

/**
 * Uses neon-util as a JitPack dependency. Running it checks that the library and its transitive
 * dependencies (e.g. kotlin-stdlib) are resolvable and work at runtime.
 */
public class JitPackTest {
    public static void main(String[] args) {
        JVMUtilKt.printJavaInformation();
    }
}
