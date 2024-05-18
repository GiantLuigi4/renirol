package tfc.renirol.frontend.rendering;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.itf.ReniTaggable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_OBJECT_TYPE_QUEUE;

public class ReniQueue implements ReniTaggable<ReniQueue> {
    private final ReniLogicalDevice device;
    private final VkQueue direct;

    public ReniQueue(ReniLogicalDevice device, VkQueue direct) {
        this.device = device;
        this.direct = direct;
    }

    public <T> T getDirect(Class<T> type) {
        return (T) direct;
    }

    public void await() {
        VK13.vkQueueWaitIdle(direct);
    }

    List<ByteBuffer> bufs = new ArrayList<>();

    @Override
    public ReniQueue setName(String name) {
        for (ByteBuffer buf : bufs) MemoryUtil.memFree(buf);
        bufs.clear();

        VkDebugUtilsObjectNameInfoEXT objectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.create();
        objectNameInfoEXT.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
        objectNameInfoEXT.objectType(VK_OBJECT_TYPE_QUEUE);
        objectNameInfoEXT.objectHandle(direct.address());
        ByteBuffer buf = MemoryUtil.memUTF8(name);
        objectNameInfoEXT.pObjectName(buf);
        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device.getDirect(VkDevice.class), objectNameInfoEXT);
        bufs.add(buf);
        objectNameInfoEXT.free();
        return this;
    }
}
