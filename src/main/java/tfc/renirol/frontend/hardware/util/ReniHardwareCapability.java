package tfc.renirol.frontend.hardware.util;

import org.lwjgl.vulkan.EXTMultiDraw;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.NVMeshShader;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;

import java.util.ArrayList;
import java.util.function.Predicate;

public class ReniHardwareCapability {
    public static final ReniHardwareCapability MESH_SHADER = new ReniHardwareCapability(dev -> dev.features.meshShader);
    public static final ReniHardwareCapability GEOMETRY_SHADER = new ReniHardwareCapability(dev -> dev.features.geometryShader);
    public static final ReniHardwareCapability TESSELATION_SHADER = new ReniHardwareCapability(dev -> dev.features.tesselationShader);
    public static final ReniHardwareCapability COMPUTE_SHADER = new ReniHardwareCapability(dev -> dev.features.computeShader);

    public static final ReniHardwareCapability MULTI_VIEWPORT = new ReniHardwareCapability(dev -> dev.features.multiView);

    public static final ReniHardwareCapability MULTIDRAW = new ReniHardwareCapability(dev -> dev.features.multidraw);
    public static final ReniHardwareCapability INSTANCING = new ReniHardwareCapability(dev -> dev.features.instancing);
    public static final ReniHardwareCapability SWAPCHAIN = new ReniHardwareCapability(dev -> dev.supportsExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));

    public final Predicate<ReniHardwareDevice> supportQuery;

    public ReniHardwareCapability(Predicate<ReniHardwareDevice> supportQuery) {
        this.supportQuery = supportQuery;
    }

    public static class SHARED_INDICES {
        public static ReniHardwareCapability configured(ReniQueueType... indices) {
            return new ReniHardwareCapability(device -> {
                ArrayList<Integer> all = new ArrayList<>();
                for (ReniQueueType index : indices)
                    all.addAll(device.getQueueInformation(index));
                for (ReniQueueType index : indices)
                    all.retainAll(device.getQueueInformation(index));
                return !all.isEmpty();
            });
        }
    }
    public static class SUPPORTS_INDICES {
        public static ReniHardwareCapability configured(ReniQueueType... indices) {
            return new ReniHardwareCapability(device -> {
                for (ReniQueueType index : indices) {
                    if (device.getQueueInformation(index).isEmpty())
                        return false;
                }
                return true;
            });
        }
    }
}
