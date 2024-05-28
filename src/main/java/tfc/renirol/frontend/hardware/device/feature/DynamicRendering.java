package tfc.renirol.frontend.hardware.device.feature;

import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

// no meaning in openngl
public class DynamicRendering implements DeviceFeature {
    public static final DynamicRendering INSTANCE = new DynamicRendering(true, true, true, true);

    boolean uniforms, storage, sampledImage, storageImage;

    public DynamicRendering(boolean uniforms, boolean storage, boolean sampledImage, boolean storageImage) {
        this.uniforms = uniforms;
        this.storage = storage;
        this.sampledImage = sampledImage;
        this.storageImage = storageImage;
    }

    public VkFeature getVkFeature() {
        return new VkFeature() {
            @Override
            public void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo) {
                VkPhysicalDeviceDynamicRenderingFeatures features = VkPhysicalDeviceDynamicRenderingFeatures.calloc();
                features.sType$Default().dynamicRendering(true);
                createInfo.pNext(features);
                builder.mustFree(features::free);
            }
        };
    }
}
