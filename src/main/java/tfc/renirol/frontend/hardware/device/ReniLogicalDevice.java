package tfc.renirol.frontend.hardware.device;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.frontend.hardware.device.queue.FamilyName;
import tfc.renirol.frontend.hardware.device.queue.ReniQueue;
import tfc.renirol.frontend.hardware.device.queue.ReniQueueFamily;
import tfc.renirol.frontend.hardware.util.QueueRequest;
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
    protected final HashSet<String> enabledExt;
    HashMap<FamilyName, ReniQueueFamily> families = new HashMap<>();

    public ReniLogicalDevice(List<QueueRequest.QueueInfo> chosen, VkDevice direct, ReniHardwareDevice hardware, HashSet<String> enabledExt) {
        this.direct = direct;
        this.hardware = hardware;
        for (QueueRequest.QueueInfo queueInfo : chosen) {
            families.put(new FamilyName(queueInfo.types()),
                    ReniQueueFamily.create(this, direct, queueInfo)
            );
        }
        this.enabledExt = enabledExt;
    }

    public boolean extensionEnabled(String name) {
        return enabledExt.contains(name);
    }

    public ReniQueue getStandardQueue(ReniQueueType type) {
        for (ReniQueueFamily value : families.values()) {
            if (value.types.contains(type)) {
                return value.queues.get(0);
            }
        }
        return null;
    }

    // generic to avoid class loading/compilation issues
    public <T> T getDirect(Class<T> type) {
        return (T) direct;
    }

    public void destroy() {
        VK10.nvkDestroyDevice(direct, 0);
    }

    public int getQueueFamilyIndex(ReniQueueType type) {
        for (ReniQueueFamily value : families.values()) {
            if (value.types.contains(type)) {
                return value.family;
            }
        }
        return -1;
    }

    public int getQueueFamilyIndex(FamilyName name) {
        return families.get(name).family;
    }

    public ReniQueueFamily getQueueFamily(FamilyName name) {
        return families.get(name);
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
