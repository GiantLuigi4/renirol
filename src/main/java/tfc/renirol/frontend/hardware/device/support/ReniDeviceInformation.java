package tfc.renirol.frontend.hardware.device.support;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.Vendors;

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
        Vendors vendor = getVendorEnum();
        if (vendor == Vendors.UNKNOWN)
            return VkUtil.find(VK13.class, "VK_VENDOR_ID_", properties.vendorID());
        return vendor.name;
    }

    public Vendors getVendorEnum() {
        int id = properties.vendorID();
        for (Vendors value : Vendors.values()) {
            if (value.matches(id)) {
                return value;
            }
        }
        return Vendors.UNKNOWN;
    }
}
