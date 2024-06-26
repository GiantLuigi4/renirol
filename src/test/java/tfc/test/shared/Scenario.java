package tfc.test.shared;

import tfc.renirol.api.DeviceFeature;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;

import java.util.ArrayList;

public class Scenario {
    public static final ArrayList<String> EXTENSIONS = new ArrayList<>();
    public static final ArrayList<DeviceFeature> FEATURES = new ArrayList<>();
    public static boolean useWinNT = false;
    public static boolean useRenderDoc = true;
    public static boolean useDepth = false;

    public static ReniHardwareDevice.LogicalDeviceBuilder configureDevice(ReniHardwareDevice.LogicalDeviceBuilder logicalDeviceBuilder) {
        FEATURES.forEach(logicalDeviceBuilder::with);

        return logicalDeviceBuilder;
    }
}
