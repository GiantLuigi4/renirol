package tfc.renirol.frontend.hardware.device.feature;

import com.sun.source.tree.BreakTree;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceDescriptorIndexingFeatures;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

// no meaning in openngl
public class Bindless implements DeviceFeature {
    public static final Bindless INSTANCE = new Bindless(true, true, true, true);

    boolean uniforms, storage, sampledImage, storageImage;

    public Bindless(boolean uniforms, boolean storage, boolean sampledImage, boolean storageImage) {
        this.uniforms = uniforms;
        this.storage = storage;
        this.sampledImage = sampledImage;
        this.storageImage = storageImage;
    }

    public VkFeature getVkFeature() {
        return new VkFeature() {
            @Override
            public void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo) {
                VkPhysicalDeviceDescriptorIndexingFeatures features = VkPhysicalDeviceDescriptorIndexingFeatures.calloc();
                features
                        .sType(VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES)
                        // Enable non sized arrays
                        .runtimeDescriptorArray(true)
                        // Enable non bound descriptors slots
                        .descriptorBindingPartiallyBound(true)
                        // Enable non uniform array indexing
                        // (#extension GL_EXT_nonuniform_qualifier : require)
                        .shaderStorageBufferArrayNonUniformIndexing(storage)
                        .shaderSampledImageArrayNonUniformIndexing(sampledImage)
                        .shaderStorageImageArrayNonUniformIndexing(storageImage)
                        // All of these enables to update after the
                        // commandbuffer used the bindDescriptorsSet
                        .descriptorBindingUniformBufferUpdateAfterBind(uniforms)
                        .descriptorBindingStorageBufferUpdateAfterBind(storage)
                        .descriptorBindingSampledImageUpdateAfterBind(sampledImage)
                        .descriptorBindingStorageImageUpdateAfterBind(storageImage);
                createInfo.pNext(features);
                builder.mustFree(features::free);
            }
        };
    }
}
