package tfc.renirol.frontend.rendering.resource.buffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.BufferUsage;
import tfc.renirol.frontend.enums.IndexSize;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.itf.ReniTaggable;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_OBJECT_TYPE_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_OBJECT_TYPE_DEVICE_MEMORY;

public class GPUBuffer implements ReniDestructable, ReniTaggable<GPUBuffer> {
    ReniLogicalDevice device;
    long buffer;
    VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc();
    long memory;
    boolean ownMemory = false;
    long size;

    public GPUBuffer(ReniLogicalDevice device, long buffer, long memory, long size) {
        this.device = device;
        this.buffer = buffer;
        this.memory = memory;
        this.size = size;

        VK13.nvkGetBufferMemoryRequirements(device.getDirect(VkDevice.class), buffer, memRequirements.address());
    }

    public GPUBuffer(ReniLogicalDevice device, BufferUsage usage, long memory, long size) {
        this.device = device;

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc();
        bufferInfo.sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(this.size = size);
        bufferInfo.usage(usage.id);
        bufferInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
        this.device = device;
        this.buffer = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateBuffer(
                this.device.getDirect(VkDevice.class), bufferInfo.address(), 0, MemoryUtil.memAddress(buf)
        ));
        bufferInfo.free();
        VK13.vkBindBufferMemory(device.getDirect(VkDevice.class), buffer, memory, 0);

        this.memory = memory;
        this.size = size;

        VK13.nvkGetBufferMemoryRequirements(device.getDirect(VkDevice.class), buffer, memRequirements.address());
    }

    public GPUBuffer(ReniLogicalDevice device, IndexSize size, BufferUsage usage, int vertices) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc();
        bufferInfo.sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(this.size = (long) size.size * vertices);
        bufferInfo.usage(usage.id);
        bufferInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
        this.device = device;
        this.buffer = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateBuffer(
                this.device.getDirect(VkDevice.class), bufferInfo.address(), 0, MemoryUtil.memAddress(buf)
        ));
        bufferInfo.free();

        VK13.nvkGetBufferMemoryRequirements(device.getDirect(VkDevice.class), buffer, memRequirements.address());
    }

    public GPUBuffer(ReniLogicalDevice device, BufferUsage usage, int size) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc();
        bufferInfo.sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(this.size = size);
        bufferInfo.usage(usage.id);
        bufferInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
        this.device = device;
        this.buffer = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateBuffer(
                this.device.getDirect(VkDevice.class), bufferInfo.address(), 0, MemoryUtil.memAddress(buf)
        ));
        bufferInfo.free();

        VK13.nvkGetBufferMemoryRequirements(device.getDirect(VkDevice.class), buffer, memRequirements.address());
    }

    public GPUBuffer(ReniLogicalDevice device, BufferDescriptor descriptor, BufferUsage usage, int vertices) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc();
        bufferInfo.sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(this.size = (long) descriptor.format.stride * vertices);
        bufferInfo.usage(usage.id);
        bufferInfo.sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
        this.device = device;
        this.buffer = VkUtil.getCheckedLong((buf) -> VK13.nvkCreateBuffer(
                this.device.getDirect(VkDevice.class), bufferInfo.address(), 0, MemoryUtil.memAddress(buf)
        ));
        bufferInfo.free();

        VK13.nvkGetBufferMemoryRequirements(device.getDirect(VkDevice.class), buffer, memRequirements.address());
    }

    public void allocate() {
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc();
        allocInfo.sType(VK13.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(device.findMemoryType(
                memRequirements.memoryTypeBits(),
                VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        ));

        memory = VkUtil.getCheckedLong((buf) -> VK13.nvkAllocateMemory(device.getDirect(VkDevice.class), allocInfo.address(), 0, MemoryUtil.memAddress(buf)));
        VK13.vkBindBufferMemory(device.getDirect(VkDevice.class), buffer, memory, 0);

        ownMemory = true;
    }

    public void upload(int offset, ByteBuffer data) {
        upload(offset, data.limit() - data.position(), data);
    }

    public void upload(int offset, int length, ByteBuffer data) {
        VkDevice device = this.device.getDirect(VkDevice.class);

        PointerBuffer pData = MemoryUtil.memAllocPointer(1);
        VkUtil.check(VK10.vkMapMemory(device, memory, offset, length, 0, pData));
        long dataptr = pData.get(0);
        MemoryUtil.memFree(pData);

        ByteBuffer buf = MemoryUtil.memByteBuffer(dataptr, length);
        buf.put(data);
        VK10.vkUnmapMemory(device, memory);
    }

    public void destroy() {
        memRequirements.free();
        VkDevice device = this.device.getDirect(VkDevice.class);
        VK13.nvkDestroyBuffer(device, buffer, 0);
        VK13.nvkFreeMemory(device, memory, 0);
    }

    public ByteBuffer createByteBuf() {
        return MemoryUtil.memAlloc((int) size);
    }

    public long getHandle() {
        return buffer;
    }

    public long getSize() {
        return size;
    }

    // not guaranteed to exist; depends on backend
    public long getMemory() {
        return memory;
    }

    public void destroyBuffer() {
        memRequirements.free();
        VkDevice device = this.device.getDirect(VkDevice.class);
        VK13.nvkDestroyBuffer(device, buffer, 0);
    }

    @Override
    public GPUBuffer setName(String name) {
        VkDebugUtilsObjectNameInfoEXT objectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.malloc();
        objectNameInfoEXT.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
        objectNameInfoEXT.objectType(VK_OBJECT_TYPE_BUFFER);
        objectNameInfoEXT.objectHandle(buffer);
        objectNameInfoEXT.pNext(0);
        ByteBuffer buf = MemoryUtil.memUTF8(name);
        objectNameInfoEXT.pObjectName(buf);
        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device.getDirect(VkDevice.class), objectNameInfoEXT);
        MemoryUtil.memFree(buf);
        if (ownMemory) {
            buf = MemoryUtil.memUTF8(name + " (Memory)");
            objectNameInfoEXT.pObjectName(buf);
            objectNameInfoEXT.objectHandle(memory);
            objectNameInfoEXT.objectType(VK_OBJECT_TYPE_DEVICE_MEMORY);
            EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device.getDirect(VkDevice.class), objectNameInfoEXT);
            MemoryUtil.memFree(buf);
        }
        objectNameInfoEXT.free();
        return this;
    }
}
