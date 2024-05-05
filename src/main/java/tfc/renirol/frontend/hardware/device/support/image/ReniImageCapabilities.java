package tfc.renirol.frontend.hardware.device.support.image;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.util.Pair;

import java.nio.IntBuffer;

public class ReniImageCapabilities implements ReniDestructable {
    public final Pair<Integer, Integer> minSize;
    public final Pair<Integer, Integer> maxSize;

    // TODO: make these not be native types
    public final VkSurfaceCapabilitiesKHR surfaceCapabilities;
    public final VkSurfaceFormatKHR.Buffer formats;
    public final IntBuffer presentModes;

    public ReniImageCapabilities(
            Pair<Integer, Integer> minSize, Pair<Integer, Integer> maxSize,
            VkSurfaceCapabilitiesKHR surfaceCapabilities, VkSurfaceFormatKHR.Buffer formats,
            IntBuffer presentModes
    ) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.surfaceCapabilities = surfaceCapabilities;
        this.formats = formats;
        this.presentModes = presentModes;
    }

    @Override
    public void destroy() {
        surfaceCapabilities.free();
        formats.free();
        MemoryUtil.memFree(presentModes);
    }
}
