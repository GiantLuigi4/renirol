package tfc.renirol.frontend.hardware.device.queue;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.util.QueueRequest;
import tfc.renirol.util.ReadOnlyList;

import java.util.ArrayList;
import java.util.Arrays;

public class ReniQueueFamily {
    public final ReadOnlyList<ReniQueueType> types;
    public final ReadOnlyList<ReniQueue> queues;
    public final int family;

    public ReniQueueFamily(ReadOnlyList<ReniQueueType> types, ReadOnlyList<ReniQueue> queues, int family) {
        this.types = types;
        this.queues = queues;
        this.family = family;
    }

    public static ReniQueueFamily create(
            ReniLogicalDevice logicalDevice,
            VkDevice direct,
            QueueRequest.QueueInfo queueInfo
    ) {
        ArrayList<ReniQueue> queues1 = new ArrayList<>();
        for (int i = 0; i < queueInfo.count(); i++) {
            final int fi = i;
            queues1.add(VkUtil.get(
                    (buf) -> VK10.nvkGetDeviceQueue(direct, queueInfo.index(), fi, buf.address()),
                    (handle) -> new ReniQueue(logicalDevice, new VkQueue(handle, direct), fi)
            ));
        }
        return new ReniQueueFamily(
                new ReadOnlyList<>(Arrays.asList(queueInfo.types())),
                new ReadOnlyList<>(queues1),
                queueInfo.index()
        );
    }

    public boolean hasQueue(ReniQueueType reniQueueType) {
        for (ReniQueueType type : types)
            if (type == reniQueueType)
                return true;
        return false;
    }
}
