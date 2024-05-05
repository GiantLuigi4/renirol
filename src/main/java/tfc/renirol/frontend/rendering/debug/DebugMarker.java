package tfc.renirol.frontend.rendering.debug;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;

import java.nio.ByteBuffer;

public class DebugMarker {
    VkDebugUtilsLabelEXT utilsLabel = VkDebugUtilsLabelEXT.calloc();
    ByteBuffer name;

    public DebugMarker() {
        utilsLabel.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
    }

    public long address() {
        return utilsLabel.address();
    }

    public DebugMarker name(String name) {
        if (this.name != null) MemoryUtil.memFree(this.name);
        utilsLabel.pLabelName(this.name = MemoryUtil.memUTF8(name));
        return this;
    }

    public DebugMarker color(float r, float g, float b, float a) {
        utilsLabel.color(0, r);
        utilsLabel.color(1, g);
        utilsLabel.color(2, b);
        utilsLabel.color(3, a);
        return this;
    }

    public void destroy() {
        utilsLabel.free();
    }
}
