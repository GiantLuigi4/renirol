package tfc.renirol.util.windows;

import org.lwjgl.system.NativeType;
import org.lwjgl.system.windows.User32;
import tfc.renirol.util.accel.ReniLib;

public class ReniUser32 {
    static {
        ReniLib.initialize();
    }

    public static native @NativeType("HWND") long SetCapture(@NativeType("HWND") long hwnd);

    public static native @NativeType("HWND") long GetCapture();

    public static native @NativeType("BOOL") boolean ReleaseCapture();

    /**
     * Gets the maximum delay between clicks for it to be considered a double click
     *
     * @return the maximum delay in milliseconds
     */
    public static native @NativeType("UINT") int GetDoubleClickTime();

    // TODO: move to custom native?
    public static @NativeType("UINT") int GetDpiForSystem() {
        return User32.GetDpiForSystem();
    }

    public static @NativeType("UINT") int GetDpiForWindow(@NativeType("HWND") long hwnd) {
        return User32.GetDpiForWindow(hwnd);
    }
}
