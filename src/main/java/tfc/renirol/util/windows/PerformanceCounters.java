package tfc.renirol.util.windows;

import tfc.renirol.util.ReniLib;

public class PerformanceCounters {
    static {
        ReniLib.initialize();
    }

    public static native long QueryPerformanceFrequency();

    public static native long QueryPerformanceCounter();

    public static native long GetTickCount();
}
