package tfc.renirol.frontend.hardware.device.support.image;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.util.Pair;

import java.nio.IntBuffer;

public class ReniSwapchainCapabilities implements ReniDestructable {
    protected Pair<Integer, Integer> minSize;
    protected Pair<Integer, Integer> maxSize;

    // TODO: make these not be native types
    public final VkSurfaceCapabilitiesKHR surfaceCapabilities;
    public final VkSurfaceFormatKHR.Buffer formats;
    public final IntBuffer presentModes;

    public ReniSwapchainCapabilities(
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
//        surfaceCapabilities.free();
////        MemoryUtil.nmemFree(surfaceCapabilities.address());
//        formats.free();
////        MemoryUtil.nmemFree(formats.address());
//        MemoryUtil.memFree(presentModes);
    }

    public void update() {
        minSize = Pair.of(surfaceCapabilities.minImageExtent().width(), surfaceCapabilities.minImageExtent().height());
        maxSize = Pair.of(surfaceCapabilities.maxImageExtent().width(), surfaceCapabilities.maxImageExtent().height());
    }

    public Pair<Integer, Integer> getMinSize() {
        return minSize;
    }

    public Pair<Integer, Integer> getMaxSize() {
        return maxSize;
    }
}
