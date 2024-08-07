package tfc.renirol.frontend.rendering.command;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.frontend.enums.BindPoint;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.IndexSize;
import tfc.renirol.frontend.enums.flags.ImageUsage;
import tfc.renirol.frontend.enums.flags.ShaderStageFlags;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.enums.modes.CompareOp;
import tfc.renirol.frontend.enums.modes.CullMode;
import tfc.renirol.frontend.enums.modes.FrontFace;
import tfc.renirol.frontend.enums.modes.PrimitiveType;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.device.queue.ReniQueue;
import tfc.renirol.frontend.hardware.device.queue.ReniQueueFamily;
import tfc.renirol.frontend.rendering.command.barrier.ImageBarrier;
import tfc.renirol.frontend.rendering.command.pipeline.ComputePipeline;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.debug.DebugMarker;
import tfc.renirol.frontend.rendering.framebuffer.Framebuffer;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.pass.ReniPassAttachment;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorSet;
import tfc.renirol.frontend.rendering.resource.image.itf.ImageBacked;
import tfc.renirol.itf.ReniDestructable;
import tfc.renirol.itf.ReniTaggable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/*
 * On GL:
 *      this can be an NVCommandList, DisplayList, or a CommandDispatcher, dependent on hardware capabilities and if it is requested to be reusable
 *      reusable->NVCommandList
 *      non-reusable->CommandDispatcher
 *      https://github.com/nvpro-samples/gl_commandlist_basic/blob/master/basic-nvcommandlist.cpp
 *      Alternatively, if hardware support is right, it can also be a DisplayList with a legacy profile for reusable
 *
 *      Usage of these are the same as they all extend CommandBuffer
 *      Every one has its own performance benefits
 *
 * On VK:
 *      this represents a VkCommandBuffer
 */
// TODO: automatically select between KHR and normal variants based on hardware support
public class CommandBuffer implements ReniDestructable, ReniTaggable<CommandBuffer> {
    final VkDevice device;
    final boolean ownPool;
    final long pool;
    final PointerBuffer buffers;
    final VkCommandBuffer cmd;

    final VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc();
    final VkSubmitInfo submitInfo = VkSubmitInfo.calloc();


    public CommandBuffer(VkDevice device, VkCommandBuffer cmd, int flags) {
        this.device = device;
        this.buffers = null;
        ownPool = false;
        pool = 0;
        this.cmd = cmd;

        setup(flags);
    }

    public static CommandBuffer create(ReniLogicalDevice device, ReniQueueFamily queueFamily, boolean primary, boolean forReuse) {
        return new CommandBuffer(device, queueFamily, primary, forReuse, false);
    }

    public static CommandBuffer create(ReniLogicalDevice device, ReniQueueFamily queueFamily, boolean primary, boolean forReuse, boolean shortLived) {
        return new CommandBuffer(device, queueFamily, primary, forReuse, shortLived);
    }

    private CommandBuffer(ReniLogicalDevice device, ReniQueueFamily queueFamily, boolean primary, boolean forReuse, boolean shortLived) {
        this.device = device.getDirect(VkDevice.class);
        ownPool = true;
        VkCommandPoolCreateInfo poolCreateInfo = VkCommandPoolCreateInfo.calloc();
        poolCreateInfo.sType(VK13.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
        if (shortLived)
            poolCreateInfo.flags(VK13.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
        else poolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

        poolCreateInfo.queueFamilyIndex(queueFamily.family);
        pool = VkUtil.getCheckedLong(
                (buf) -> VK13.nvkCreateCommandPool(device.getDirect(VkDevice.class), poolCreateInfo.address(), 0, MemoryUtil.memAddress(buf))
        );
        poolCreateInfo.free();

        VkCommandBufferAllocateInfo alloc = VkCommandBufferAllocateInfo.calloc();
        alloc.sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        alloc.commandBufferCount(1);
        alloc.commandPool(pool);
        alloc.level(primary ? VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK13.VK_COMMAND_BUFFER_LEVEL_SECONDARY);
        buffers = MemoryUtil.memAllocPointer(1);

        VkUtil.check(VK13.nvkAllocateCommandBuffers(device.getDirect(VkDevice.class), alloc.address(), MemoryUtil.memAddress(buffers)));
        cmd = new VkCommandBuffer(buffers.get(0), device.getDirect(VkDevice.class));
        alloc.free();

        setup(forReuse ? VK13.VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT : VK13.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
    }

    IntBuffer dstStage = MemoryUtil.memAllocInt(1);
    PointerBuffer theBufferOfMyself = MemoryUtil.memAllocPointer(1);
    LongBuffer waitSemaphore = MemoryUtil.memAllocLong(1);
    LongBuffer signalSemaphore = MemoryUtil.memAllocLong(1);

    public void startLabel(DebugMarker marker) {
        EXTDebugUtils.nvkCmdBeginDebugUtilsLabelEXT(cmd, marker.address());
    }

    DebugMarker marker = null;

    public void startLabel(String name, float r, float g, float b, float a) {
        if (this.marker == null)
            marker = new DebugMarker();
        marker.name(name).color(r, g, b, a);
        EXTDebugUtils.nvkCmdBeginDebugUtilsLabelEXT(cmd, marker.address());
    }

    public void endLabel() {
        EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(cmd);
    }

    private void setup(int flags) {
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(flags); // Optional
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

        theBufferOfMyself.put(0, cmd);
        submitInfo.pWaitDstStageMask(dstStage);
        submitInfo.pCommandBuffers(theBufferOfMyself);
        submitInfo.pWaitSemaphores(waitSemaphore);
        submitInfo.waitSemaphoreCount(1);
        submitInfo.pSignalSemaphores(signalSemaphore);
    }

    public void reset() {
        VK10.vkResetCommandBuffer(cmd, 0);
    }

    final VkViewport.Buffer viewport = VkViewport.calloc(1);
    final VkOffset2D offset2D = VkOffset2D.calloc();
    final VkRect2D.Buffer scissor = VkRect2D.calloc(1);

    public void viewport(
            float x, float y,
            float width, float height,
            float minDepth, float maxDepth
    ) {
        viewport.x(x);
        viewport.y(y);
        viewport.width(width);
        viewport.height(height);
        viewport.minDepth(minDepth);
        viewport.maxDepth(maxDepth);
        VK10.nvkCmdSetViewport(cmd, 0, viewport.capacity(), viewport.address());
    }

    public void scissor(int x, int y, int width, int height) {
        scissor.offset().set(x, y);
        scissor.extent().set(width, height);
        VK10.nvkCmdSetScissor(cmd, 0, scissor.capacity(), scissor.address());
    }

    public void viewportScissor(
            float x, float y,
            float width, float height,
            float minDepth, float maxDepth) {
        viewport(x, y, width, height, minDepth, maxDepth);
        scissor((int) x, (int) y, (int) width, (int) height);
    }

    GraphicsPipeline boundPipe;

    public void bindPipe(GraphicsPipeline pipeline) {
        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle);
    }

    public void draw(int firstVert, int vertices) {
        VK13.vkCmdDraw(cmd, vertices, 1, firstVert, 0);
    }

    public void drawMeshTask(int x, int y, int z) {
        EXTMeshShader.vkCmdDrawMeshTasksEXT(cmd, x, y, z);
    }

    public void drawInstanced(int firstVert, int vertices, int firstInstance, int count) {
        VK10.vkCmdDraw(cmd, vertices, count, firstVert, firstInstance);
    }

    public void endPass() {
        KHRDynamicRendering.vkCmdEndRenderingKHR(cmd);
    }

    public void cullMode(CullMode mode) {
        VK13.vkCmdSetCullMode(cmd, mode.id);
    }

    public void primitiveType(PrimitiveType type) {
        VK13.vkCmdSetPrimitiveTopology(cmd, type.id);
    }

    public void usePrimitiveRestart(boolean value) {
        VK13.vkCmdSetPrimitiveRestartEnable(cmd, value);
    }

    public void useDepthTest(boolean value) {
        VK13.vkCmdSetDepthTestEnable(cmd, value);
    }

    public void depthOperation(CompareOp op) {
        VK13.vkCmdSetDepthCompareOp(cmd, op.id);
    }

    public void setFrontFace(FrontFace face) {
        VK13.vkCmdSetFrontFace(cmd, face.id);
    }

    @Override
    public void destroy() {
        beginInfo.free();
        submitInfo.free();

        MemoryUtil.memFree(dstStage);
        MemoryUtil.memFree(theBufferOfMyself);
        MemoryUtil.memFree(waitSemaphore);
        MemoryUtil.memFree(signalSemaphore);

        if (ownPool) {
            VK13.nvkFreeCommandBuffers(device, pool, 1, MemoryUtil.memAddress(buffers));
            VK13.nvkDestroyCommandPool(device, pool, 0);
        }
    }

    public void begin() {
        VkUtil.check(VK10.nvkBeginCommandBuffer(cmd, beginInfo.address()));
    }

    public void end() {
        VkUtil.check(VK10.vkEndCommandBuffer(cmd));
    }

    public void callBuffer(CommandBuffer other) {
        PointerBuffer pb = MemoryUtil.memAllocPointer(1);
        pb.put(0, other.cmd);
        VK13.nvkCmdExecuteCommands(cmd, 1, MemoryUtil.memAddress(pb));
        MemoryUtil.memFree(pb);
    }

    public void submitAsync(ReniQueue queue, int expectedStage, long fence, long waitSemaphore, long signalSemaphore) {
        dstStage.put(0, expectedStage);
        this.waitSemaphore.put(0, waitSemaphore);
        this.signalSemaphore.put(0, signalSemaphore);
        VkUtil.check(nvkQueueSubmit(queue.getDirect(VkQueue.class), 1, submitInfo.address(), fence));
    }

    @Deprecated(forRemoval = true)
    public void submit(ReniQueue queue, StageMask targetStage) {
        submitBlocking(queue, targetStage);
    }

    public void submitBlocking(ReniQueue queue, StageMask targetStage) {
        submitAsync(queue, targetStage);
        queue.await();
    }

    public void submitAsync(ReniQueue queue, StageMask targetStage) {
        dstStage.put(0, targetStage.value);
        VkSubmitInfo info = VkSubmitInfo.calloc();
        info.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
        info.pCommandBuffers(theBufferOfMyself);
        dstStage.put(0, VK13.VK_PIPELINE_STAGE_NONE);
        info.pWaitDstStageMask(dstStage);

        VkUtil.check(nvkQueueSubmit(queue.getDirect(VkQueue.class), 1, info.address(), 0));
        info.free();
    }

    public void beginPass(RenderPassInfo pass, Framebuffer buffer, VkExtent2D extents) {
        VkRenderingInfo info = VkRenderingInfo.calloc();
        info.sType$Default();

        info.renderArea().offset(offset2D);
        info.renderArea().extent(extents);
        VkRenderingAttachmentInfo.Buffer buffer1 = VkRenderingAttachmentInfo.calloc(pass.getColorAttachments().size());
        int index = 0;
        int ridx = 0;
        VkRenderingAttachmentInfo depthInf = null;
        for (ReniPassAttachment attachment : pass.getAttachments()) {
            VkRenderingAttachmentInfo attachmentInfo;

            if (attachment.isDepth)
                depthInf = attachmentInfo = VkRenderingAttachmentInfo.calloc();
            else attachmentInfo = buffer1.get(index);
            attachmentInfo.sType$Default();

            attachmentInfo.loadOp(attachment.load.load);
            attachmentInfo.storeOp(attachment.store.store);

            if (!noClear) {
                if (!attachment.isDepth) attachmentInfo.clearValue(clearColor.get(0));
                else attachmentInfo.clearValue(depthColor.get(0));
            }

            attachmentInfo.imageLayout(attachment.initialLayout.value);
            attachmentInfo.resolveImageLayout(attachment.targetLayout.value);

            attachmentInfo.imageView(buffer.getView(ridx));

            if (attachment.isDepth)
                info.pDepthAttachment(attachmentInfo);
            else
                index++;
            ridx++;
        }
        info.pColorAttachments(buffer1);
        info.layerCount(1);

        KHRDynamicRendering.nvkCmdBeginRenderingKHR(
                cmd, info.address()
        );

        info.free();
        if (depthInf != null)
            depthInf.free();
    }

    final VkClearValue.Buffer clearColor = VkClearValue.calloc(1);
    final VkClearValue.Buffer depthColor = VkClearValue.calloc(1);
    final VkClearValue.Buffer clearValues = VkClearValue.calloc(2);

    public void clearColor(float r, float g, float b, float a) {
        FloatBuffer buffer1 = clearColor.get(0).color().float32();
        buffer1.put(0, r);
        buffer1.put(1, g);
        buffer1.put(2, b);
        buffer1.put(3, a);

        noClear = false;
    }

    public void clearDepth(float depth) {
        depthColor.depthStencil().depth(depth);

        clearValues.put(0, clearColor.get(0));
        clearValues.put(1, depthColor.get(0));

        noClear = false;
    }

    public void clearStencil(int value) {
        depthColor.depthStencil().stencil(value);

        clearValues.put(0, clearColor.get(0));
        clearValues.put(1, depthColor.get(0));

        noClear = false;
    }

    public void transition(ImageBarrier barrier) {
        VkImageMemoryBarrier.Buffer barriers = VkImageMemoryBarrier.malloc(1);
        barriers.put(0, barrier.getDirect(VkImageMemoryBarrier.class));
        VK13.nvkCmdPipelineBarrier(
                cmd,
                barrier.getSrcStage().value, barrier.getDstSage().value,
                0, 0, 0, 0, 0,
                1, barriers.address()
        );
        barriers.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            long image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        barrier.srcAccessMask(0); // TODO
        barrier.dstAccessMask(0); // TODO
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            ImageBacked image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image.getHandle());
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        barrier.srcAccessMask(0); // TODO
        barrier.dstAccessMask(0); // TODO
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            long image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout,
            AccessMask oldAccess, AccessMask newAccess
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.srcAccessMask(oldAccess.value);
        barrier.dstAccessMask(newAccess.value);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            ImageBacked image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout,
            AccessMask oldAccess, AccessMask newAccess
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.srcAccessMask(oldAccess.value);
        barrier.dstAccessMask(newAccess.value);
        barrier.image(image.getHandle());
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            long image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout,
            AccessMask oldAccess, AccessMask newAccess,
            ImageUsage usage
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.srcAccessMask(oldAccess.value);
        barrier.dstAccessMask(newAccess.value);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(usage.aspect);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            ImageBacked image,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout,
            AccessMask oldAccess, AccessMask newAccess,
            ImageUsage usage
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.srcAccessMask(oldAccess.value);
        barrier.dstAccessMask(newAccess.value);
        barrier.image(image.getHandle());
        barrier.subresourceRange().aspectMask(usage.aspect);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            long image,
            ImageUsage usage,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(usage.aspect);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        // TODO: expose
        barrier.srcAccessMask(0);
        barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transition(
            ImageBacked image,
            ImageUsage usage,
            StageMask oldStage, StageMask newStage,
            ImageLayout oldLayout, ImageLayout newLayout
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image.getHandle());
        barrier.subresourceRange().aspectMask(usage.aspect);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        // TODO: expose
        barrier.srcAccessMask(0);
        barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    @Deprecated(forRemoval = true)
    public void transfer(
            ImageBacked image,
            ImageUsage usage,
            StageMask oldStage, StageMask newStage,
            AccessMask oldAccess, AccessMask newAccess,
            ImageLayout oldLayout, ImageLayout newLayout,
            ReniQueueFamily from, ReniQueueFamily to
    ) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout.value);
        barrier.newLayout(newLayout.value);
        barrier.srcQueueFamilyIndex(from.family);
        barrier.dstQueueFamilyIndex(to.family);
        barrier.image(image.getHandle());
        barrier.subresourceRange().aspectMask(usage.aspect);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);
        // TODO: expose
        barrier.srcAccessMask(oldAccess.value);
        barrier.dstAccessMask(newAccess.value);
        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, null,
                barrier
        );
        barrier.free();
    }

    boolean noClear = true;

    public void noClear() {
        noClear = true;
    }

    public void bindVbo(int slot, GPUBuffer ebo) {
        LongBuffer buffer = MemoryUtil.memAllocLong(1);
        LongBuffer oset = MemoryUtil.memCallocLong(1);
        buffer.put(0, ebo.getHandle());
        oset.put(0, 0);
        VK13.nvkCmdBindVertexBuffers(
                cmd,
                slot,
                1,
                MemoryUtil.memAddress(buffer),
                MemoryUtil.memAddress(oset)
        );
        MemoryUtil.memFree(buffer);
        MemoryUtil.memFree(oset);
    }

    public void bindIbo(IndexSize size, GPUBuffer ebo) {
        VK13.vkCmdBindIndexBuffer(
                cmd, ebo.getHandle(),
                0, size.id
        );
    }

    public void drawIndexed(
            int firstVert,
            int firstInstance, int count,
            int firstIndex, int indices
    ) {
        VK13.vkCmdDrawIndexed(
                cmd,
                indices, count,
                firstIndex, firstVert,
                firstInstance
        );
    }

    public void bindDescriptor(BindPoint bindPoint, GraphicsPipeline pipeline0, DescriptorSet set) {
        LongBuffer buf = MemoryUtil.memAllocLong(1);
        buf.put(0, set.handle);
        VK13.nvkCmdBindDescriptorSets(
                cmd, bindPoint.id,
                pipeline0.layout.handle,
                0,
                1, MemoryUtil.memAddress(buf),
                0, 0
        );
        MemoryUtil.memFree(buf);
    }

    public void bindDescriptor(BindPoint bindPoint, GraphicsPipeline pipeline0, DescriptorSet set, int id) {
        LongBuffer buf = MemoryUtil.memAllocLong(1);
        buf.put(0, set.handle);
        VK13.nvkCmdBindDescriptorSets(
                cmd, bindPoint.id,
                pipeline0.layout.handle,
                id,
                1, MemoryUtil.memAddress(buf),
                0, 0
        );
        MemoryUtil.memFree(buf);
    }

    public void copy(GPUBuffer src, long srcOffset, GPUBuffer dst, long dstOffset, long size) {
        VkBufferCopy.Buffer regions = VkBufferCopy.calloc(1);
        regions.get(0).srcOffset(srcOffset).dstOffset(dstOffset).size(size);
        VK13.nvkCmdCopyBuffer(
                cmd,
                src.getHandle(), dst.getHandle(),
                1, regions.address()
        );
        regions.free();
    }

    public void copyImage(
            ImageBacked src, ImageLayout srcLayout,
            int srcX, int srcY,
            ImageBacked dst, ImageLayout dstLayout,
            int dstX, int dstY,
            int extX, int extY
    ) {
        VkImageCopy.Buffer regions = VkImageCopy.calloc(1);
        regions.get(0).srcOffset().set(srcX, srcY, 0);
        regions.get(0).dstOffset().set(dstX, dstY, 0);
        regions.get(0).extent().set(extX, extY, 1);

        regions.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        regions.srcSubresource().baseArrayLayer(0);
        regions.srcSubresource().layerCount(1);
        regions.srcSubresource().mipLevel(0);
        regions.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        regions.dstSubresource().baseArrayLayer(0);
        regions.dstSubresource().layerCount(1);
        regions.dstSubresource().mipLevel(0);

        VK13.nvkCmdCopyImage(
                cmd, src.getHandle(), srcLayout.value,
                dst.getHandle(), dstLayout.value,
                1, regions.address()
        );
        regions.free();
    }

    public <T> T getDirect(Class<T> clazz) {
        return (T) cmd;
    }

    public void pushConstants(
            long layout, ShaderStageFlags[] stages,
            int start, int size,
            ByteBuffer data
    ) {
        int flags = 0;
        for (ShaderStageFlags stage : stages)
            flags |= stage.bits;
        VK13.nvkCmdPushConstants(
                cmd, layout,
                flags, start, size,
                MemoryUtil.memAddress(data)
        );
    }

    public void bufferData(
            GPUBuffer buffer,
            int start, int amount,
            ByteBuffer data
    ) {
        VK13.nvkCmdUpdateBuffer(
                cmd, buffer.getHandle(),
                start, amount, MemoryUtil.memAddress(data)
        );
    }

    public void transitionBuffer(
            GPUBuffer buffer,
            StageMask oldStage, StageMask newStage,
            AccessMask srcAccess, AccessMask dstAccess
    ) {
        VkBufferMemoryBarrier.Buffer barrier = VkBufferMemoryBarrier.calloc(1);
        barrier.sType$Default();
        barrier.buffer(buffer.getHandle());
        barrier.size(buffer.getSize());
        barrier.srcAccessMask(srcAccess.value);
        barrier.dstAccessMask(dstAccess.value);

        VK13.vkCmdPipelineBarrier(
                cmd,
                oldStage.value, newStage.value,
                0,
                null, barrier,
                null
        );

        barrier.free();
    }

    @Override
    public CommandBuffer setName(String name) {
        VkDebugUtilsObjectNameInfoEXT objectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.malloc();
        objectNameInfoEXT.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
        objectNameInfoEXT.objectType(VK_OBJECT_TYPE_COMMAND_BUFFER);
        objectNameInfoEXT.objectHandle(buffers.get(0));
        objectNameInfoEXT.pNext(0);
        ByteBuffer buf = MemoryUtil.memUTF8(name);
        objectNameInfoEXT.pObjectName(buf);
        EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, objectNameInfoEXT);
        MemoryUtil.memFree(buf);
        if (ownPool) {
            buf = MemoryUtil.memUTF8(name + " (pool)");
            objectNameInfoEXT.objectType(VK_OBJECT_TYPE_COMMAND_POOL);
            objectNameInfoEXT.objectHandle(pool);
            objectNameInfoEXT.pObjectName(buf);
            EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, objectNameInfoEXT);
            MemoryUtil.memFree(buf);
        }
        objectNameInfoEXT.free();
        return this;
    }

    public void bindCompute(ComputePipeline computePipeline) {
        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline.handle);
    }

    public void compute(int groupX, int groupY, int groupZ) {
        vkCmdDispatch(cmd, groupX, groupY, groupZ);
    }
}
