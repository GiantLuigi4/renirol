package tfc.renirol.frontend.hardware.device.support;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniVendor;

public class ReniDeviceInformation {
    final VkPhysicalDeviceProperties properties;

    public ReniDeviceInformation(VkPhysicalDeviceProperties properties) {
        this.properties = properties;
    }

    public String getName() {
        return properties.deviceNameString();
    }

    public ReniDeviceType getType() {
        return ReniDeviceType.of(properties.deviceType());
    }

    public int getVendorID() {
        return properties.vendorID();
    }

    public String getVendorName() {
        ReniVendor vendor = getVendorEnum();
        if (vendor == ReniVendor.UNKNOWN)
            return VkUtil.find(VK13.class, "VK_VENDOR_ID_", properties.vendorID());
        return vendor.name;
    }

    public ReniVendor getVendorEnum() {
        int id = properties.vendorID();
        for (ReniVendor value : ReniVendor.values()) {
            if (value.matches(id)) {
                return value;
            }
        }
        return ReniVendor.UNKNOWN;
    }
}
