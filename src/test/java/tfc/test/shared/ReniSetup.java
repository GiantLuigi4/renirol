package tfc.test.shared;

import org.lwjgl.vulkan.*;
import tfc.renirol.ReniContext;
import tfc.renirol.Renirol;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.device.support.image.ReniSwapchainCapabilities;
import tfc.renirol.frontend.hardware.util.DeviceQuery;
import tfc.renirol.frontend.hardware.util.ReniHardwareCapability;
import tfc.renirol.frontend.rendering.framebuffer.SwapChain;
import tfc.renirol.frontend.rendering.selectors.ChannelInfo;
import tfc.renirol.frontend.rendering.selectors.FormatSelector;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.frontend.windowing.glfw.Setup;
import tfc.renirol.frontend.windowing.winnt.WinNTWindow;

public class ReniSetup {
    public static final ReniContext GRAPHICS_CONTEXT = new ReniContext();
    public static final GenericWindow WINDOW;

    static {
        Setup.performanceSetup();
//        if (Scenario.useRenderDoc) Setup.loadRenderdoc();
        if (!Renirol.BACKEND.equals("OpenGL"))
            Setup.noAPI();
        WINDOW = Scenario.useWinNT ? new WinNTWindow(
                800, 800,
                "reni-test"
        ) : new GLFWWindow(
                800, 800,
                "reni-test"
        );

        if (Renirol.BACKEND.equals("VULKAN")) {
            GRAPHICS_CONTEXT.requestExtensions(
                    EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
            );
            GRAPHICS_CONTEXT.setFlags(0); // optional
        }
        GRAPHICS_CONTEXT.setup(WINDOW, "ReniTest", 1, 0, 0); // inits vulkan instance
        GRAPHICS_CONTEXT.supportingDevice(
                new DeviceQuery()
                        .require((dev) -> { // print information about the devices
                            System.out.println(dev.getName());
                            System.out.println(" - Driver");
                            System.out.println("  - Driver Name        : " + dev.getDriverName());
                            System.out.println("  - Driver Info        : " + dev.getDriverInfo());
                            System.out.println("  - Driver API         : " + Renirol.BACKEND);
                            System.out.println("  - Driver API Version : " + dev.getDriverAPIVersion());
                            System.out.println(" - Device");
                            System.out.println("  - Type               : " + dev.getType());
                            System.out.println("  - VendorID           : " + dev.information.getVendorID());
                            System.out.println("  - VendorID (hex)     : " + Integer.toHexString(dev.information.getVendorID()));
                            System.out.println("  - Vendor Name        : " + dev.information.getVendorName());
                            return true;
                        })
                        //necessary features
                        .require(ReniHardwareCapability.SUPPORTS_INDICES.configured(
                                ReniQueueType.GRAPHICS, ReniQueueType.TRANSFER
                        ))
                        .reniRecommended()
                        // if any integrated GPU meets the requirements, then filter out any non-dedicated GPU
//                        .prioritizeIntegrated()
                        // low-importance features
                        .request(100, ReniHardwareCapability.SUPPORTS_INDICES.configured(ReniQueueType.COMPUTE))
                        // microsoft seems to emulate GPUs with "Dozen" being the driver name, and these are kinda horrible at functioning
                        // so filter those out if possible
                        .request(-2000, device -> device.getDriverName().toString().contains("Dozen"))
        ); // does nothing with OpenGL
        System.out.println(GRAPHICS_CONTEXT.getHardware().getName());
        GRAPHICS_CONTEXT.withLogical(
                Scenario.configureDevice(
                        GRAPHICS_CONTEXT.getHardware().createLogical()
                                .enableIfPossible(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)
                                .enableIfPossible(NVLowLatency.VK_NV_LOW_LATENCY_EXTENSION_NAME)
                                // TODO: should probably support shared pairs
                                // i.e. split(shared(GRAPHICS, TRANSFER), shared(COMPUTE, TRANSFER))
                                .requestSharedIndices(
                                        // if compute pipeline is supported, then use it
                                        // elsewise, do not
                                        ReniHardwareCapability.SUPPORTS_INDICES.configured(ReniQueueType.COMPUTE).supportQuery.test(GRAPHICS_CONTEXT.getHardware()) ?
                                                new ReniQueueType[]{ReniQueueType.GRAPHICS, ReniQueueType.TRANSFER, ReniQueueType.COMPUTE} :
                                                new ReniQueueType[]{ReniQueueType.GRAPHICS, ReniQueueType.TRANSFER}
                                )
                ).create()
        );
        WINDOW.initContext(GRAPHICS_CONTEXT);
    }

    public static final FormatSelector selector = new FormatSelector()
            .channels(
                    new ChannelInfo('r', 8, 16, 32),
                    new ChannelInfo('g', 8, 16, 32),
                    new ChannelInfo('b', 8, 16, 32)
            )
            .type("SRGB");
    public static final int DEPTH_FORMAT = VK13.VK_FORMAT_D16_UNORM;

    public static void initialize() {
        WINDOW.pollSize();
        final SwapChain presentChain = ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain();
        final ReniSwapchainCapabilities capabilities = ReniSetup.GRAPHICS_CONTEXT.getHardware().features.image(
                ReniSetup.GRAPHICS_CONTEXT.getSurface()
        );
        presentChain.create(
                ReniSetup.WINDOW.getWidth(),
                ReniSetup.WINDOW.getHeight(),
                selector,
                Math.min(capabilities.surfaceCapabilities.minImageCount() + 2, capabilities.surfaceCapabilities.maxImageCount()),
                capabilities.presentModes.get(0)
        );
        if (Scenario.useDepth) {
            ReniSetup.GRAPHICS_CONTEXT.createDepth();
            ReniSetup.GRAPHICS_CONTEXT.depthBuffer().create(
                    ReniSetup.WINDOW.getWidth(),
                    ReniSetup.WINDOW.getHeight(),
                    DEPTH_FORMAT
            );
        }
        ReniSetup.WINDOW.show();
        capabilities.destroy();
    }
}
