package tfc.renirol.frontend.hardware.device.support;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;
import tfc.renirol.util.Pair;

import java.nio.IntBuffer;
import java.util.HashMap;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;

public class ReniDeviceFeatures {
    private final VkPhysicalDeviceFeatures features;
    private final ReniHardwareDevice device;

    // apparently freeing the things stored by this is illegal, so apparently I need to cache this
    private final HashMap<Long, ReniSwapchainCapabilities> map = new HashMap<>();

    public ReniSwapchainCapabilities image(long surface) {
        if (map.containsKey(surface)) {
            ReniSwapchainCapabilities caps = map.get(surface);
            VkUtil.check(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getDirect(VkPhysicalDevice.class), surface, caps.surfaceCapabilities));
            caps.update();
            return caps;
        }

        VkSurfaceCapabilitiesKHR capabilitiesKHR = null;
        if (surface != 0) {
            capabilitiesKHR = VkSurfaceCapabilitiesKHR.malloc();
            VkUtil.check(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getDirect(VkPhysicalDevice.class), surface, capabilitiesKHR));
        }

        IntBuffer countBuf = MemoryUtil.memAllocInt(1);

        VkSurfaceFormatKHR.Buffer formats = null;
        if (surface != 0) {
            vkGetPhysicalDeviceSurfaceFormatsKHR(device.getDirect(VkPhysicalDevice.class), surface, countBuf, null);
            if (countBuf.get(0) != 0) {
                VkSurfaceFormatKHR.Buffer buffer = VkSurfaceFormatKHR.malloc(countBuf.get(0));
                vkGetPhysicalDeviceSurfaceFormatsKHR(device.getDirect(VkPhysicalDevice.class), surface, countBuf, buffer);
                formats = buffer;
            }
        }

        IntBuffer presentModes = null;
        if (surface != 0) {
            vkGetPhysicalDeviceSurfacePresentModesKHR(device.getDirect(VkPhysicalDevice.class), surface, countBuf, null);
            if (countBuf.get(0) != 0) {
                IntBuffer buffer = MemoryUtil.memAllocInt(countBuf.get(0));
                vkGetPhysicalDeviceSurfacePresentModesKHR(device.getDirect(VkPhysicalDevice.class), surface, countBuf, buffer);
                presentModes = buffer;
            }
        }

        MemoryUtil.memFree(countBuf);

        ReniSwapchainCapabilities caps = new ReniSwapchainCapabilities(
                capabilitiesKHR != null ? Pair.of(capabilitiesKHR.minImageExtent().width(), capabilitiesKHR.minImageExtent().height()) : null,
                capabilitiesKHR != null ? Pair.of(capabilitiesKHR.maxImageExtent().width(), capabilitiesKHR.maxImageExtent().height()) : null,
                capabilitiesKHR,
                formats, presentModes
        );
        map.put(surface, caps);
        return caps;
    }

    public final boolean computeShader;
    public final boolean instancing;
    public final boolean multidraw;
    public final boolean geometryShader;
    public final boolean tesselationShader;
    public final boolean meshShader;
    public final boolean multiView;

    public ReniDeviceFeatures(ReniHardwareDevice device, VkPhysicalDeviceFeatures features) {
        this.device = device;

        // vk always supports these
        computeShader = true;
        instancing = true;

        multiView = features.multiViewport();
        multidraw = device.supportsExtension(EXTMultiDraw.VK_EXT_MULTI_DRAW_EXTENSION_NAME);
        meshShader = device.supportsExtension(EXTMeshShader.VK_EXT_MESH_SHADER_EXTENSION_NAME);
        tesselationShader = features.tessellationShader();
        geometryShader = features.geometryShader();

        this.features = features;
    }

}
