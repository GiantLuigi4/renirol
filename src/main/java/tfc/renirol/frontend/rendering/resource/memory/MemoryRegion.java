package tfc.renirol.frontend.rendering.resource.memory;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;

import java.util.ArrayList;
import java.util.TreeSet;

public class MemoryRegion {
    VkDevice device;
    long memory;
    TreeSet<Integer> starts = new TreeSet<>();
    ArrayList<Integer> lengths = new ArrayList<>();

    public MemoryRegion(VkDevice device, int size) {
        lengths.add(size);
    }

    public void allocate() {
        // TODO
    }
}
