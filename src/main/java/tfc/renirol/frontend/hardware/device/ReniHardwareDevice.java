package tfc.renirol.frontend.hardware.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.api.DeviceFeature;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.support.ReniDeviceFeatures;
import tfc.renirol.frontend.hardware.device.support.ReniDeviceInformation;
import tfc.renirol.frontend.hardware.device.support.ReniDeviceType;
import tfc.renirol.frontend.hardware.util.QueueRequest;
import tfc.renirol.frontend.hardware.util.ReniHardwareCapability;
import tfc.renirol.util.ReadOnlyList;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ReniHardwareDevice {
    VkPhysicalDevice direct;
    // TODO: should not be public
    public final VkPhysicalDeviceProperties properties;
    public final VkPhysicalDeviceFeatures vkFeatures;

    public final ReniDeviceInformation information;
    public final ReniDeviceFeatures features;

    private final HashMap<ReniQueueType, ReadOnlyList<Integer>> queueInformation = new HashMap<>();
    private final ArrayList<QueueFamilyDescriptor> allQueues = new ArrayList<>();
    private final ReadOnlyList<QueueFamilyDescriptor> queuesReadonly = new ReadOnlyList<>(allQueues);

    // generic to avoid class loading/compilation issues
    public <T> T getDirect(Class<T> type) {
        return (T) direct;
    }

    public static class QueueFamilyDescriptor {
        public final int index;
        public final int flags;
        public final int count;

        public QueueFamilyDescriptor(int index, int flags, int count) {
            this.index = index;
            this.flags = flags;
            this.count = count;
        }
    }

    public ReniHardwareDevice(VkPhysicalDevice direct) {
        this.direct = direct;
        properties = VkPhysicalDeviceProperties.malloc();
        VK10.vkGetPhysicalDeviceProperties(direct, properties);
        vkFeatures = VkPhysicalDeviceFeatures.malloc();
        VK10.vkGetPhysicalDeviceFeatures(direct, vkFeatures);

        information = new ReniDeviceInformation(properties);
        features = new ReniDeviceFeatures(this, vkFeatures);
    }

    public boolean supportsExtension(String name) {
        IntBuffer count = MemoryUtil.memAllocInt(1);
        VK13.vkEnumerateDeviceExtensionProperties(direct, (ByteBuffer) null, count, null);

        VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.calloc(count.get(0));
        VK13.vkEnumerateDeviceExtensionProperties(direct, (ByteBuffer) null, count, availableExtensions);
        MemoryUtil.memFree(count);

        boolean exist = false;
        for (int i = 0; i < availableExtensions.capacity(); i++) {
            VkExtensionProperties availableExtension = availableExtensions.get(i);
            if (availableExtension.extensionNameString().equals(name)) {
                exist = true;
//                break;
            }
        }

        availableExtensions.free();

        return exist;
    }

    public String getName() {
        return properties.deviceNameString();
    }

    public String getDriverName() {
        VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2.calloc();
        properties2.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);
        VkPhysicalDeviceDriverProperties properties1 = VkPhysicalDeviceDriverProperties.calloc();
        properties1.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES);
        properties2.pNext(properties1);
        VK12.vkGetPhysicalDeviceProperties2(direct, properties2);
        String driver = properties1.driverNameString();
        properties1.free();
        properties2.free();
        return driver;
    }

    public String getDriverInfo() {
        VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2.calloc();
        properties2.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);
        VkPhysicalDeviceDriverProperties properties1 = VkPhysicalDeviceDriverProperties.calloc();
        properties1.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES);
        properties2.pNext(properties1);
        VK12.vkGetPhysicalDeviceProperties2(direct, properties2);
        String driver = properties1.driverInfoString();
        properties1.free();
        properties2.free();
        return driver;
    }

    public String getDriverAPIVersion() {
        VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2.calloc();
        properties2.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);
        VkPhysicalDeviceDriverProperties properties1 = VkPhysicalDeviceDriverProperties.calloc();
        properties1.sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES);
        properties2.pNext(properties1);
        VK12.vkGetPhysicalDeviceProperties2(direct, properties2);
        String driver = "VK_" + properties1.conformanceVersion().major() + "." + properties1.conformanceVersion().minor() + "." + properties1.conformanceVersion().patch();
        properties1.free();
        properties2.free();
        return driver;
    }

    protected void checkQueues() {
        if (queueInformation.isEmpty()) {
            IntBuffer queueFamilyCount = MemoryUtil.memAllocInt(1);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(direct, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer pb = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0));
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(direct, queueFamilyCount, pb);
            MemoryUtil.memFree(queueFamilyCount);

            HashMap<ReniQueueType, ArrayList<Integer>> queueChannels = new HashMap<>();
            for (ReniQueueType value : ReniQueueType.values()) {
                queueChannels.put(value, new ArrayList<>());
            }
            int[] index = new int[]{0};
            VkUtil.iterate(pb,
                    (family) -> {
                        allQueues.add(new QueueFamilyDescriptor(
                                index[0], family.queueFlags(),
                                family.queueCount()
                        ));
                        for (ReniQueueType value : ReniQueueType.values()) {
                            if (value.isApplicable(family.queueFlags())) {
                                queueChannels.get(value).add(index[0]);
                            }
                        }
                        index[0]++;
                    }
            );

            queueChannels.forEach((k, v) -> queueInformation.put(k, new ReadOnlyList<>(v)));
        }
    }

    public ReadOnlyList<Integer> getQueues(ReniQueueType type) {
        checkQueues();
        return queueInformation.get(type);
    }

    public ReadOnlyList<QueueFamilyDescriptor> getQueues() {
        checkQueues();
        return queuesReadonly;
    }

    public boolean supports(ReniHardwareCapability capability) {
        return capability.supportQuery.test(this);
    }

    public ReniDeviceType getType() {
        return ReniDeviceType.of(properties.deviceType());
    }

    public LogicalDeviceBuilder createLogical() {
        return new LogicalDeviceBuilder();
    }

    /**
     * Temp object for building a logical device
     * These are not reusable
     */
    public class LogicalDeviceBuilder {
        VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc();
        List<Runnable> releaseFuncs = new ArrayList<>();
        VkDeviceQueueCreateInfo.Buffer queueCreateInfo;

        public LogicalDeviceBuilder() {
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            boolean rba = vkFeatures.robustBufferAccess();
            vkFeatures.robustBufferAccess(false);
            mustFree(() -> vkFeatures.robustBufferAccess(rba));
            createInfo.pEnabledFeatures(vkFeatures);
            releaseFuncs.add(createInfo::free);
        }

        List<String> ext = new ArrayList<>();

        // TODO: request first indices, request specific indices

        List<QueueRequest.QueueInfo> chosen;

        public LogicalDeviceBuilder requestIndices(QueueRequest request) {
            if (queueCreateInfo != null)
                return this;

            request.prepare(ReniHardwareDevice.this, (type) -> new ArrayList<>(ReniHardwareDevice.this.getQueues(type)));
            chosen = request.select();

            HashSet<QueueRequest.QueueInfo> UNIQUE = new HashSet<>();
            for (QueueRequest.QueueInfo queueInfo : chosen) {
                UNIQUE.add(queueInfo);
            }

            queueCreateInfo = VkDeviceQueueCreateInfo.calloc(UNIQUE.size());
            int idx = 0;
            for (QueueRequest.QueueInfo i : UNIQUE) {
                FloatBuffer prior = MemoryUtil.memAllocFloat(i.count());
                for (int i1 = 0; i1 < i.count(); i1++)
                    prior.put(i1, i.count() - i1);

                VkDeviceQueueCreateInfo info = queueCreateInfo.get(idx);
                info.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                info.queueFamilyIndex(i.index());
                info.pQueuePriorities(prior);
                VkDeviceQueueCreateInfo.nqueueCount(info.address(), i.count());
                idx++;

                long addr = MemoryUtil.memAddress(prior);
                releaseFuncs.add(() -> MemoryUtil.nmemFree(addr));
            }

            createInfo.pQueueCreateInfos(queueCreateInfo);

            releaseFuncs.add(queueCreateInfo::free);

            return this;
        }

        public LogicalDeviceBuilder enableIfPossible(String extension) {
            if (ReniHardwareDevice.this.supportsExtension(extension))
                ext.add(extension);
            return this;
        }

        public LogicalDeviceBuilder enable(String extension) {
            ext.add(extension);
            return this;
        }

        public ReniLogicalDevice create() {
            if (!ext.isEmpty()) {
                PointerBuffer pb = VkUtil.toPointerBuffer(ext.toArray(new String[0]));
                createInfo.ppEnabledExtensionNames(pb);
                releaseFuncs.add(() -> VkUtil.freeStringPointerBuffer(pb));
            }

            ReniLogicalDevice device = VkUtil.getChecked(
                    (buf) -> VK10.nvkCreateDevice(direct, createInfo.address(), 0, buf.address()),
                    (handle) -> new ReniLogicalDevice(chosen, new VkDevice(handle, ReniHardwareDevice.this.direct, createInfo), ReniHardwareDevice.this, new HashSet<>(ext))
            );
            for (Runnable releaseFunc : releaseFuncs) releaseFunc.run();
            return device;
        }

        public LogicalDeviceBuilder with(DeviceFeature feature) {
            feature.getVkFeature().apply(ReniHardwareDevice.this, this, createInfo);
            return this;
        }

        public void mustFree(Runnable r) {
            releaseFuncs.add(r);
        }
    }
}
