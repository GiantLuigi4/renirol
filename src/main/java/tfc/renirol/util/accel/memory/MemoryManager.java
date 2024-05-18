package tfc.renirol.util.accel.memory;

// TODO: pure java equivalent

import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.util.accel.memory.cpp.BestFitManager;
import tfc.renirol.util.accel.memory.java.BestFitAllocator;

/**
 * Manages memory addresses but doesn't actually allocate anything.<br>
 * Used to manage memory on the GPU.
 */
public abstract class MemoryManager implements ReniDestructable {
    /**
     * @param size      the size of the memory block
     * @param alignment alignment bytes (4 results in a minimum block size of 16)
     * @return A best fit memory manager implementation for the block, chosen automatically based off if reni's natives support the operating system and architecture. <br>
     * Will be either an {@link BestFitAllocator} if reni's natives are not supported, or an {@link BestFitManager} if they are.
     */
    public static MemoryManager createBestFit(int size, int alignment) {
        // TODO: check OS&architecture
        return new BestFitManager(size, alignment);
    }

    public MemoryManager(int size, int alignment) {
    }


    public abstract int allocate(int requested_size);

    public abstract boolean release(int address);
}
