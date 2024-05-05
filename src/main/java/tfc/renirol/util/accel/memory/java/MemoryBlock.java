package tfc.renirol.util.accel.memory.java;

public class MemoryBlock {
    int startAddress;
    int size;
    boolean isAllocated;
    MemoryBlock next;

    MemoryBlock(int start, int sz) {
        this.startAddress = start;
        this.size = sz;
        this.isAllocated = false;
    }
}
