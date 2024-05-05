package tfc.renirol.frontend.hardware.device;

import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.rendering.ReniQueue;
import tfc.renirol.util.Pair;

import java.util.HashMap;

public class ReniLogicalDevice {
    private final VkDevice direct;
    public final ReniHardwareDevice hardware;
    protected final HashMap<ReniQueueType, Integer> standardIndices;
    protected final HashMap<ReniQueueType, ReniQueue> queues = new HashMap<>();

    public ReniLogicalDevice(HashMap<ReniQueueType, Integer> standardIndices, VkDevice direct, ReniHardwareDevice hardware) {
        this.standardIndices = standardIndices;
        this.direct = direct;
        this.hardware = hardware;
        standardIndices.forEach((k, v) -> {
            queues.put(k, VkUtil.get(
                    (buf) -> VK10.nvkGetDeviceQueue(direct, v, 0, buf.address()),
                    (handle) -> new ReniQueue(this, new VkQueue(handle, direct))
            ));
        });
    }

    public ReniQueue getStandardQueue(ReniQueueType type) {
        return queues.get(type);
    }

    // generic to avoid class loading/compilation issues
    public <T> T getDirect(Class<T> type) {
        return (T) direct;
    }

    public void destroy() {
        VK10.nvkDestroyDevice(direct, 0);
    }

    public int getQueueFamily(ReniQueueType queueType) {
        return standardIndices.get(queueType);
    }

    public void waitForIdle() {
        VK13.vkDeviceWaitIdle(direct);
    }

    public int findMemoryType(int type, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        VK13.vkGetPhysicalDeviceMemoryProperties(direct.getPhysicalDevice(), memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if (
//                    (type & (1 << i)) == 0 &&
                    (type & i) != 0 &&
                            (memProperties.memoryTypes(i).propertyFlags() & properties) == properties
            ) {
                memProperties.free();
                return i;
            }
        }

        memProperties.free();
        throw new RuntimeException("Failed to find suitable memory type!");
    }
}
