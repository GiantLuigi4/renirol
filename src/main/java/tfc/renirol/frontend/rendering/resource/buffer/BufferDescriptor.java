package tfc.renirol.frontend.rendering.resource.buffer;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.enums.format.AttributeFormat;
import tfc.renirol.frontend.rendering.enums.IndexSize;

import java.util.ArrayList;

public class BufferDescriptor implements ReniDestructable {
    public final DataFormat format;
    protected VkVertexInputBindingDescription bindingDescription = VkVertexInputBindingDescription.calloc();
    protected final ArrayList<VkVertexInputAttributeDescription> attribs = new ArrayList<>();

    public BufferDescriptor(IndexSize size) {
        format = null;
    }

    public BufferDescriptor(DataFormat format) {
        this.format = format;
    }

    public void describe(int binding, int stride) {
        bindingDescription.binding(binding);
        bindingDescription.stride(stride);
        // TODO: what the heck?
        bindingDescription.inputRate(VK13.VK_VERTEX_INPUT_RATE_VERTEX);
    }

    public void describe(int binding) {
        bindingDescription.binding(binding);
        bindingDescription.stride(format.stride);
        // TODO: what the heck?
        bindingDescription.inputRate(VK13.VK_VERTEX_INPUT_RATE_VERTEX);
    }

    public void clearAttribs() {
        for (VkVertexInputAttributeDescription attrib : attribs) attrib.free();
        attribs.clear();
    }

    public void attribute(
            int binding,
            int location,
            AttributeFormat format,
            int offset
    ) {
        VkVertexInputAttributeDescription attributeDescription = VkVertexInputAttributeDescription.calloc();
        attributeDescription.binding(binding);
        attributeDescription.location(location);
        attributeDescription.format(format.id);
        attributeDescription.offset(offset);
        attribs.add(attributeDescription);
    }

    public VkVertexInputBindingDescription getBindingDescription() {
        return bindingDescription;
    }

    public ArrayList<VkVertexInputAttributeDescription> getAttribs() {
        return attribs;
    }

    @Override
    public void destroy() {
        bindingDescription.free();
        clearAttribs();
    }
}
