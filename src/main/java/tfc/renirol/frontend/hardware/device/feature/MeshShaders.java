package tfc.renirol.frontend.hardware.device.feature;

import org.lwjgl.vulkan.*;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

// no meaning in openngl
public class MeshShaders implements DeviceFeature {
    public static final MeshShaders INSTANCE = new MeshShaders();

    public MeshShaders() {
    }

    public VkFeature getVkFeature() {
        return new VkFeature() {
            @Override
            public void apply(ReniHardwareDevice device, ReniHardwareDevice.LogicalDeviceBuilder builder, VkDeviceCreateInfo createInfo) {
                if (device.supportsExtension(EXTMeshShader.VK_EXT_MESH_SHADER_EXTENSION_NAME)) {
                    builder.enable(EXTMeshShader.VK_EXT_MESH_SHADER_EXTENSION_NAME);
                    VkPhysicalDeviceMeshShaderFeaturesEXT features = VkPhysicalDeviceMeshShaderFeaturesEXT.calloc()
                            .sType(EXTMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT);
                    VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc().sType(VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);
                    features2.pNext(features);
                    VK13.vkGetPhysicalDeviceFeatures2(device.getDirect(VkPhysicalDevice.class), features2);
                    features
                            .sType(EXTMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT)
                            .meshShader(true);
                    createInfo.pNext(features);
                    builder.mustFree(features::free);
                }
            }
        };
    }
}
