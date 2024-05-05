package tfc.renirol.frontend.hardware.device.support;

import org.lwjgl.vulkan.VK13;

public enum ReniDeviceType {
    DISCRETE_GPU(VK13.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU),
    INTEGRATED_GPU(VK13.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU),
    VIRTUAL_GPU(VK13.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU),
    CPU(VK13.VK_PHYSICAL_DEVICE_TYPE_CPU),
    OTHER(VK13.VK_PHYSICAL_DEVICE_TYPE_OTHER),
    OPENGL(-1),
    ;

    final int vk;

    ReniDeviceType(int vk) {
        this.vk = vk;
    }

    private static final ReniDeviceType[] values = values();

    public static ReniDeviceType of(int vk) {
        for (ReniDeviceType value : values)
            if (value.vk == vk) return value;
        return null;
    }
}
