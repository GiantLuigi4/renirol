package tfc.renirol.frontend.hardware.device;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.rendering.ReniQueue;
import tfc.renirol.itf.ReniTaggable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_OBJECT_TYPE_DEVICE;

public class ReniLogicalDevice implements ReniTaggable<ReniLogicalDevice> {
    private final VkDevice direct;
    public final ReniHardwareDevice hardware;
    protected final HashMap<ReniQueueType, Integer> standardIndices;
    protected final HashMap<ReniQueueType, ReniQueue> queues = new HashMap<>();
    protected final HashSet<String> enabledExt;

    public ReniLogicalDevice(HashMap<ReniQueueType, Integer> standardIndices, VkDevice direct, ReniHardwareDevice hardware, HashSet<String> enabledExt) {
        this.standardIndices = standardIndices;
        this.direct = direct;
        this.hardware = hardware;
        standardIndices.forEach((k, v) -> {
            queues.put(k, VkUtil.get(
                    (buf) -> VK10.nvkGetDeviceQueue(direct, v, 0, buf.address()),
                    (handle) -> new ReniQueue(this, new VkQueue(handle, direct))
            ));
        });
        this.enabledExt = enabledExt;
    }

    public boolean extensionEnabled(String name) {
        return enabledExt.contains(name);
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

    public void await() {
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

    List<ByteBuffer> bufs = new ArrayList<>();

    @Override
    public ReniLogicalDevice setName(String name) {
        for (ByteBuffer buf : bufs) MemoryUtil.memFree(buf);
        bufs.clear();

        VkDebugUtilsObjectNameInfoEXT objectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.create();
        objectNameInfoEXT.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
        objectNameInfoEXT.objectType(VK_OBJECT_TYPE_DEVICE);
        objectNameInfoEXT.objectHandle(direct.address());
        ByteBuffer buf = MemoryUtil.memUTF8(name);
        objectNameInfoEXT.pObjectName(buf);
        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(direct, objectNameInfoEXT);
        bufs.add(buf);
        objectNameInfoEXT.free();
        return this;
    }
}
