package tfc.renirol.util.accel.memory.cpp;

import tfc.renirol.util.ReniLib;
import tfc.renirol.util.accel.memory.MemoryManager;

public class BestFitManager extends MemoryManager {
    static {
        ReniLib.initialize();
    }

    long handle;

    public BestFitManager(int size, int alignment) {
        super(size, alignment);
        handle = generate(size, (alignment - 1));
    }

    public static native long generate(int size, int alignment);

    @Override
    public int allocate(int requested_size) {
        return allocate(handle, requested_size);
    }

    public static native int allocate(long handle, int requestedSize);

    @Override
    public boolean release(int address) {
        return release(handle, address);
    }

    public static native boolean release(long handle, int address);

    public void destroy() {
        free(handle);
    }

    public static native void free(long handle);
}
