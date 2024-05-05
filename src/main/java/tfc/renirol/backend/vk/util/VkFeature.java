package tfc.renirol.backend.vk.util;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

public abstract class VkFeature {
    public abstract void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo);
}
