#include "pch.h"
#include "tfc_renirol_util_accel_cpp_BestFitManager.h"

class MemoryBlock {
public:
    int start_address;
    int size;
    bool is_allocated;
    MemoryBlock* next = nullptr;

    MemoryBlock(int start, int sz) : start_address(start), size(sz), is_allocated(false) {}
};

class BestFitAllocator {
public:
    int total_memory_size;
    int alignment;
    MemoryBlock* root = nullptr;
    std::list<MemoryBlock*> free_blocks;

public:
    BestFitAllocator(int total_size, int alignmentVal) : total_memory_size(total_size), alignment(alignmentVal) {
        root = new MemoryBlock(0, total_size);
        free_blocks.push_back(root);
    }

private:
    int prepBlock(MemoryBlock* best_fit_block, int requested_size) {
        // Align the start address to 4 bytes
        int aligned_start_address = (best_fit_block->start_address + alignment) & ~alignment;

        // Split the block if necessary
        if (best_fit_block->size > requested_size) {
            int nextSize = best_fit_block->size - requested_size;

            MemoryBlock* remaining_block = new MemoryBlock(aligned_start_address + requested_size, nextSize);
            best_fit_block->size = requested_size;
            remaining_block->next = best_fit_block->next;
            best_fit_block->next = remaining_block;

            // this new block is free to use
            free_blocks.push_back(remaining_block);
        }

        // mark it as no longer being free
        best_fit_block->is_allocated = true;
        free_blocks.remove(best_fit_block);
        return aligned_start_address;
    }

    MemoryBlock* findBlock(int requested_size) {
        MemoryBlock* best_fit_block = nullptr;
        // iterate over blocks that are currently not reserved for anything
        for (MemoryBlock* block : free_blocks) {
            if (!block->is_allocated && block->size >= requested_size) {
                if (!best_fit_block || block->size < best_fit_block->size) {
                    best_fit_block = block;

                    // if the block is the same size as what was requested, then it is obviously the best fit
                    if (best_fit_block->size == requested_size)
                        break;
                }
            }
        }
        return best_fit_block;
    }

public:
    int allocate(int requested_size) {
        // align block size
        requested_size = (requested_size + alignment) & ~alignment;

        // Find the smallest available block that fits the requested size
        MemoryBlock* best_fit_block = findBlock(requested_size);

        if (best_fit_block) {
            return prepBlock(best_fit_block, requested_size);
        } else {
            return -1; // Allocation failed
        }
    }

    bool deallocate(int address) {
        // Mark the block as unallocated
        MemoryBlock* block = root;
        MemoryBlock* earliestFree = nullptr;
        while (block != nullptr) {
            // keep track of the closest free block to the block that was just marked as free
            // this allows for blocks to be merged together, as the previous block is not referenced by the memory blocks
            // TODO: change this for efficiency reasons
            if (!earliestFree && !block->is_allocated)
                earliestFree = block;
            else earliestFree = nullptr;

            if (block->start_address == address) {
                // mark a block as free when it already is free may cause problems
                if (!block->is_allocated)
                    return false;

                block->is_allocated = false;

                // add block to list of free blocks
                free_blocks.push_back(block);

                if (earliestFree)
                    block = earliestFree;

                // merge blocks together
                MemoryBlock* nextBlock = block->next;
                while (nextBlock && !nextBlock->is_allocated) {
                    block->size += nextBlock->size;
                    block->next = nextBlock->next;
                    // that memory block no longer exists
                    // get rid of it!
                    free_blocks.remove(nextBlock);
                    free(nextBlock);
                    // update next
                    nextBlock = block->next;
                }

                return true;
            }

            block = block->next;
        }
        return false;
    }
};

JNIEXPORT jlong JNICALL Java_tfc_renirol_util_accel_memory_cpp_BestFitManager_generate
  (JNIEnv *, jclass, jint sz, jint alignment) {
  BestFitAllocator* a = new BestFitAllocator((int) sz, alignment);
  return (jlong) a;
}

JNIEXPORT jint JNICALL Java_tfc_renirol_util_accel_memory_cpp_BestFitManager_allocate
  (JNIEnv *, jclass, jlong handle, jint size) {
  return (jint) ((BestFitAllocator*)handle)->allocate((int) size);
}

JNIEXPORT jboolean JNICALL Java_tfc_renirol_util_accel_memory_cpp_BestFitManager_release
  (JNIEnv *, jclass, jlong handle, jint addr) {
  return (jboolean) ((BestFitAllocator*)handle)->deallocate(addr);
}

JNIEXPORT void JNICALL Java_tfc_renirol_util_accel_memory_cpp_BestFitManager_free
  (JNIEnv *, jclass, jlong handle) {
  BestFitAllocator* allocator = (BestFitAllocator*) handle;
  MemoryBlock* block = allocator->root;
  while (block != nullptr) {
    MemoryBlock* next = block->next;
    free(block);
    block = next;
  }
  free(allocator);
}
