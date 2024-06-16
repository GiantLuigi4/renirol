package tfc.renirol;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.enums.flags.SwapchainUsage;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.hardware.device.ReniHardwareDevice;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.util.DeviceQuery;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.fencing.Fence;
import tfc.renirol.frontend.rendering.fencing.Semaphore;
import tfc.renirol.frontend.rendering.framebuffer.Attachment;
import tfc.renirol.frontend.rendering.framebuffer.chain.ChainBuffer;
import tfc.renirol.frontend.rendering.framebuffer.chain.SwapChain;
import tfc.renirol.frontend.rendering.framebuffer.chain.SwapchainImage;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.image.Image;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.util.ReadOnlyList;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReniContext implements ReniDestructable {
    private VkInstance instance;
    private ReadOnlyList<ReniHardwareDevice> DEVICES;
    private final List<String> extensions = new ArrayList<>();
    private int myFlags;
    private ReniHardwareDevice hardware;
    private ReniLogicalDevice logical;
    private long surface;
    private ChainBuffer buffer;
    private SwapChain graphicsChain;
    private List<Attachment> additional = new ArrayList<>();

    public ReniContext() {
    }

    /**
     * Usage of this method depends on backend
     *
     * @param objects a list of objects representing the information necessary for the context<br>
     *                for VK:<br>
     *                VkInstance, VkPhysicalDevice/ReniHardwareDevice, VkLogicalDevice/ReniLogicalDevice, long (surface), Swapchain
     */
    public void set(Object... objects) {

    }

    public void requestExtensions(String... desiredExtensions) {
        this.extensions.clear();
        extensions.addAll(Arrays.asList(desiredExtensions));
    }

    public void setFlags(int flags) {
        this.myFlags = flags;
    }

    public void setup(String appName, int major, int minor, int patch) {
        setup(null, appName, major, minor, patch);
    }

    public void setup(GenericWindow window, String appName, int major, int minor, int patch) {
        VkApplicationInfo info = VkApplicationInfo.calloc();
        ByteBuffer name = MemoryUtil.memUTF8(appName);
        int appVer = VK10.VK_MAKE_VERSION(major, minor, patch);
        ByteBuffer engine = MemoryUtil.memUTF8("Renirol");
        int engineVer = VK10.VK_MAKE_VERSION(0, 0, 0);
        info.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
        info.pApplicationName(name);
        info.applicationVersion(appVer);
        info.pEngineName(engine);
        info.engineVersion(engineVer);
        info.apiVersion(VK13.VK_MAKE_API_VERSION(0, 1, 3, 0));

        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc();
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        createInfo.pApplicationInfo(info);
        createInfo.flags(myFlags);

        PointerBuffer pb;
        if (window != null) pb = window.manager.requiredVkExtensions(createInfo);
        else pb = MemoryUtil.memAllocPointer(0);

        PointerBuffer collected = MemoryUtil.memAllocPointer(pb.capacity() + extensions.size());
        for (int i = 0; i < pb.capacity(); i++) collected.put(i, pb.get(i));
        if (!extensions.isEmpty()) {
            for (int i = 0; i < extensions.size(); i++) {
                collected.put(pb.capacity() + i, MemoryUtil.memUTF8(extensions.get(i)));
            }
        }

        if (window != null) window.manager.freeExtensionBuffer(pb);
        else MemoryUtil.memFree(pb);

        createInfo.ppEnabledExtensionNames(collected);

        instance = VkUtil.getChecked(
                (buf) -> VK10.nvkCreateInstance(createInfo.address(), 0, buf.address()),
                (handle) -> new VkInstance(handle, createInfo)
        );

        createInfo.free();
        info.free();
        MemoryUtil.memFree(engine);
        MemoryUtil.memFree(name);
        MemoryUtil.memFree(collected);

        // discover physical devices
        IntBuffer deviceCount = MemoryUtil.memAllocInt(1);
        VK10.vkEnumeratePhysicalDevices(instance, deviceCount, null);
        PointerBuffer buf = MemoryUtil.memAllocPointer(deviceCount.get(0));
        VK10.vkEnumeratePhysicalDevices(instance, deviceCount, buf);
        MemoryUtil.memFree(deviceCount);
        final List<ReniHardwareDevice> devices = new ArrayList<>();
        VkUtil.iterate(buf, (handle) -> new ReniHardwareDevice(new VkPhysicalDevice(handle, instance)), devices::add);
        DEVICES = new ReadOnlyList<>(devices);
    }

    public void setupSurface(GenericWindow window) {
        this.surface = VkUtil.getLong(
                (buf) -> window.manager.createVkSurface(instance, window, 0, MemoryUtil.memAddress(buf)),
                (res) -> res
        );
        graphicsChain = new SwapChain(logical, surface);
        buffer = new ChainBuffer(frame, graphicsChain);
        semaphoreA = new Semaphore(logical);
        fenceA = semaphoreA.createFence();
        semaphoreB = new Semaphore(logical);
    }

    int depthIdx = -1;

    public void createDepth() {
        additional.add(Attachment.depth(new Image(logical).setUsage(SwapchainUsage.DEPTH)));
        buffer = new ChainBuffer(frame, graphicsChain, additional.toArray(new Attachment[0]));
        depthIdx = additional.size() - 1;
    }

    public void addBuffer(Image image, boolean depth) {
        additional.add(new Attachment(image, depth, true));
        buffer = new ChainBuffer(frame, graphicsChain, additional.toArray(new Attachment[0]));
    }

    public void setupOffscreen() {
        VkHeadlessSurfaceCreateInfoEXT createInfoEXT = VkHeadlessSurfaceCreateInfoEXT.calloc();
        createInfoEXT.sType(EXTHeadlessSurface.VK_STRUCTURE_TYPE_HEADLESS_SURFACE_CREATE_INFO_EXT);
        this.surface = VkUtil.getCheckedLong(buf -> EXTHeadlessSurface.nvkCreateHeadlessSurfaceEXT(instance, createInfoEXT.address(), 0, MemoryUtil.memAddress(buf)));
        createInfoEXT.free();
    }

    public void swapInterval(int interval) {
        // glfwSwapInterval for OpenGL
        // no-op for VK
    }

    public void onActivate(GenericWindow handle) {
        // glfwMakeContextCurrent for OpenGL
        // no-op for VK
    }

    IntBuffer frame = MemoryUtil.memAllocInt(1);
    Semaphore semaphoreA;
    Fence fenceA;
    Semaphore semaphoreB;

    public void swapBuffers(GenericWindow handle) {
        // glfwSwapBuffers for OpenGL

        LongBuffer buffer1 = MemoryUtil.memAllocLong(1);
        buffer1.put(0, graphicsChain.getId());
        IntBuffer indices = MemoryUtil.memAllocInt(1);
        indices.put(0, frame.get(0));
        LongBuffer semaphores = MemoryUtil.memAllocLong(1);
        semaphores.put(0, semaphoreB.handle);

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc();
        presentInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
        presentInfo.swapchainCount(1);
        presentInfo.pSwapchains(buffer1);
        presentInfo.pImageIndices(indices);
        presentInfo.pWaitSemaphores(semaphores);
        checkResize(
                KHRSwapchain.nvkQueuePresentKHR(logical.getStandardQueue(ReniQueueType.TRANSFER).getDirect(VkQueue.class), presentInfo.address()),
                handle
        );

        presentInfo.free();
        MemoryUtil.memFree(semaphores);
        MemoryUtil.memFree(buffer1);
        MemoryUtil.memFree(indices);
    }

    public void supportingDevice(DeviceQuery query) {
        hardware = query.select(DEVICES);
    }

    public ReniHardwareDevice getDeviceSupporting(DeviceQuery query) {
        return query.select(DEVICES);
    }

    public ReadOnlyList<ReniHardwareDevice> getDevices() {
        return DEVICES;
    }

    public ReniHardwareDevice getHardware() {
        return hardware;
    }

    public void withLogical(ReniLogicalDevice reniLogicalDevice) {
        this.logical = reniLogicalDevice;
    }

    public void destroy() {
        if (graphicsChain != null) {
            fenceA.destroy();
            semaphoreA.destroy();
            semaphoreB.destroy();
            graphicsChain.destroy();
        }
        if (logical != null)
            logical.destroy();
        KHRSurface.nvkDestroySurfaceKHR(instance, surface, 0);
        VK10.nvkDestroyInstance(instance, 0);
    }

    public long getSurface() {
        return surface;
    }

    public ReniLogicalDevice getLogical() {
        return logical;
    }

    public void prepareFrame(GenericWindow window) {
        Renirol.useContext(this);
        if (checkResize(
                graphicsChain.acquire(frame, semaphoreA.handle),
                window
        )) {
            graphicsChain.acquire(frame, semaphoreA.handle);
        }
    }

    private boolean checkResize(int result, GenericWindow window) {
        if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
            logical.waitForIdle();
            window.pollSize();
            resize(buffer, window);
            return true;
        } else if (result != VK10.VK_SUCCESS && result != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            throw new RuntimeException("Failed swapchain image");
        }
        return false;
    }

    public static void resize(ChainBuffer framebuffer, GenericWindow window) {
        framebuffer.recreate(window.getWidth(), window.getHeight());
    }

    public int getFrameIndex() {
        return frame.get(0);
    }

    public Semaphore getSemaphoreImage() {
        return semaphoreA;
    }

    public Semaphore getSemaphorePresentation() {
        return semaphoreB;
    }

    public Fence getFenceImage() {
        return fenceA;
    }

    public void submitFrame(CommandBuffer buffer) {
        buffer.submitAsync(
                getLogical().getStandardQueue(ReniQueueType.GRAPHICS),
                // TODO: look at later
                // https://themaister.net/blog/2019/08/14/yet-another-blog-explaining-vulkan-synchronization/
                VK13.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                getFenceImage().handle,
                getSemaphoreImage().handle,
                getSemaphorePresentation().handle
        );
        getFenceImage().await();
        getFenceImage().reset();
    }

    public SwapchainImage getFramebuffer() {
        return graphicsChain.getFbo(getFrameIndex());
    }

    public SwapChain defaultSwapchain() {
        return graphicsChain;
    }

    public Image depthBuffer() {
        if (depthIdx == -1) throw new RuntimeException("No depth buffer present!");

        return (Image) additional.get(depthIdx).image;
    }

    public ChainBuffer getChainBuffer() {
        return buffer;
    }

    public RenderPassInfo getPass(
            Operation load, Operation store,
            ImageLayout targetLayout
    ) {
        return buffer.genericPass(
                logical,
                load, store,
                targetLayout
        );
    }

    public void prepareChain(CommandBuffer buffer) {
        for (Attachment attachment : this.buffer) {
            if (attachment.isDepth) {
                buffer.transition(
                        attachment.image.getHandle(),
                        StageMask.TOP_OF_PIPE,
                        StageMask.FRAGMENT_TEST,
                        ImageLayout.UNDEFINED,
                        ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        AccessMask.NONE,
                        AccessMask.DEPTH_WRITE,
                        SwapchainUsage.DEPTH
                );
            } else {
                buffer.transition(
                        attachment.image.getHandle(),
                        StageMask.TOP_OF_PIPE,
                        StageMask.COLOR_ATTACHMENT_OUTPUT,
                        ImageLayout.UNDEFINED,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        AccessMask.NONE,
                        AccessMask.COLOR_WRITE
                );
            }
        }
    }

    public void preparePresent(CommandBuffer buffer) {
        buffer.transition(
                getFramebuffer().image,
                StageMask.COLOR_ATTACHMENT_OUTPUT,
                StageMask.BOTTOM_OF_PIPE,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                ImageLayout.PRESENT,
                AccessMask.COLOR_WRITE,
                AccessMask.NONE
        );
    }

    public <T> T getInstance(Class<T> vkInstanceClass) {
        return (T) instance;
    }
}
