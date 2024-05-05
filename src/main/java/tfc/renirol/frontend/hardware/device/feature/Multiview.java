package tfc.renirol.frontend.hardware.device.feature;

import org.lwjgl.vulkan.*;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

// no meaning in openngl
public class Multiview implements DeviceFeature {
    public static final Multiview INSTANCE = new Multiview();

    public Multiview() {
    }

    public VkFeature getVkFeature() {
        return new VkFeature() {
            @Override
            public void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo) {
                if (device.supportsExtension(KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME)) {
                    builder.enable(KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME);
                }
            }
        };
    }
}
