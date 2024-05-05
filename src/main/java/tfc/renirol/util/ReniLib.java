package tfc.renirol.util;

public class ReniLib {
    static {
        // TODO: extract native automatically
        System.loadLibrary("renirol-natives-x86-64");
    }

    public static void initialize() {
    }
}
