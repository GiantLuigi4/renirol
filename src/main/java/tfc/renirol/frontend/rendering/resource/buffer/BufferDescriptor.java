package tfc.renirol.frontend.rendering.resource.buffer;

import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.enums.flags.AdvanceRate;
import tfc.renirol.frontend.enums.format.AttributeFormat;
import tfc.renirol.frontend.enums.IndexSize;

import java.util.ArrayList;

public class BufferDescriptor implements ReniDestructable {
    public final DataFormat format;
    protected VkVertexInputBindingDescription bindingDescription = VkVertexInputBindingDescription.calloc();
    protected final ArrayList<VkVertexInputAttributeDescription> attribs = new ArrayList<>();
    public AdvanceRate advanceRate = AdvanceRate.PER_VERTEX;

    public BufferDescriptor(IndexSize size) {
        format = null;
    }

    public BufferDescriptor(DataFormat format) {
        this.format = format;
    }

    public BufferDescriptor advance(AdvanceRate rate) {
        this.advanceRate = rate;
        return this;
    }

    public void describe(int binding, int stride) {
        bindingDescription.binding(binding);
        bindingDescription.stride(stride);
        bindingDescription.inputRate(advanceRate.id);
    }

    public void describe(int binding) {
        bindingDescription.binding(binding);
        bindingDescription.stride(format.stride);
        bindingDescription.inputRate(advanceRate.id);
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
