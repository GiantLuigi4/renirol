package tfc.renirol.frontend.hardware.device.feature;

import org.lwjgl.vulkan.EXTMultiDraw;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

// no meaning in openngl
public class MultiDraw implements DeviceFeature {
    public static final MultiDraw INSTANCE = new MultiDraw();

    public MultiDraw() {
    }

    public VkFeature getVkFeature() {
        return new VkFeature() {
            @Override
            public void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo) {
                if (device.supportsExtension(EXTMultiDraw.VK_EXT_MULTI_DRAW_EXTENSION_NAME)) {
                    builder.enable(EXTMultiDraw.VK_EXT_MULTI_DRAW_EXTENSION_NAME);
                }
            }
        };
    }
}
