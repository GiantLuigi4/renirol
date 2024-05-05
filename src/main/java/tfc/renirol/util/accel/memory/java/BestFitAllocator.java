package tfc.renirol.util.accel.memory.java;

import tfc.renirol.util.accel.memory.MemoryManager;

// TODO: update to match native code
public class BestFitAllocator extends MemoryManager {
    int totalMemorySize;
    int alignment;
    MemoryBlock root;
    MemoryBlock firstFree;
    MemoryBlock prevFirst;

    public BestFitAllocator(int totalSize, int alignmentVal) {
        super(totalSize, alignmentVal);
        this.totalMemorySize = totalSize;
        this.alignment = alignmentVal;
        this.root = new MemoryBlock(0, totalSize);
        this.firstFree = root;
    }

    private int prepBlock(MemoryBlock bestFitBlock, int requestedSize) {
        // Align the start address to 4 bytes
        int alignedStartAddress = (bestFitBlock.startAddress + alignment) & ~alignment;

        // Split the block if necessary
        if (bestFitBlock.size > requestedSize) {
            int nextSize = bestFitBlock.size - requestedSize;

            MemoryBlock remainingBlock = new MemoryBlock(alignedStartAddress + requestedSize, nextSize);
            bestFitBlock.size = requestedSize;
            remainingBlock.next = bestFitBlock.next;
            bestFitBlock.next = remainingBlock;

            if (bestFitBlock == firstFree)
                firstFree = remainingBlock;
        } else if (bestFitBlock == firstFree) {
            if (bestFitBlock.next.isAllocated) {
                firstFree = bestFitBlock.next;
            } else {
                prevFirst = bestFitBlock;
            }
        }

        bestFitBlock.isAllocated = true;
        return alignedStartAddress;
    }

    private MemoryBlock findBlock(int requestedSize) {
        MemoryBlock bestFitBlock = null;
        MemoryBlock block = firstFree;
        if (block == null) {
            block = prevFirst;
            while (block != null) {
                if (!block.isAllocated) {
                    firstFree = block;
                    break;
                }
                block = block.next;
            }
        }

        while (block != null && bestFitBlock == null) {
            if (!block.isAllocated && block.size >= requestedSize) {
                if (bestFitBlock == null || block.size < bestFitBlock.size) {
                    bestFitBlock = block;
                }
            }
            block = block.next;
        }

        return bestFitBlock;
    }

    public int allocate(int requestedSize) {
        // align block size
        requestedSize = (requestedSize + alignment) & ~alignment;

        // Find the smallest available block that fits the requested size
        MemoryBlock bestFitBlock = findBlock(requestedSize);

        if (bestFitBlock != null) {
            int alignedStartAddress = prepBlock(bestFitBlock, requestedSize);
            return alignedStartAddress;
        } else {
            return -1; // Allocation failed
        }
    }

    public boolean release(int address) {
        // Mark the block as unallocated
        MemoryBlock block = root;
        while (block != null) {
            if (block.startAddress == address) {
                block.isAllocated = false;
                if (block.startAddress < firstFree.startAddress)
                    firstFree = block;
                return true;
            }
            block = block.next;
        }
        return false;
    }

    @Override
    public void destroy() {
        // no-op
    }
}
