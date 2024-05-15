package tfc.test.tests.font;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import tfc.renirol.frontend.enums.*;
import tfc.renirol.frontend.enums.flags.DescriptorPoolFlags;
import tfc.renirol.frontend.enums.flags.ShaderStageFlags;
import tfc.renirol.frontend.enums.format.AttributeFormat;
import tfc.renirol.frontend.enums.format.BitDepth;
import tfc.renirol.frontend.enums.format.TextureChannels;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.enums.flags.SwapchainUsage;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.framebuffer.FrameBuffer;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.*;
import tfc.renirol.frontend.rendering.resource.image.Image;
import tfc.renirol.frontend.rendering.resource.image.texture.Texture;
import tfc.renirol.frontend.rendering.resource.image.texture.TextureSampler;
import tfc.renirol.frontend.reni.font.ReniGlyph;
import tfc.renirol.util.ShaderCompiler;
import tfc.renirol.util.reni.ImageUtil;
import tfc.test.shared.ReniSetup;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Atlas {
    public TextureSampler createSampler(
            WrapMode xWrap,
            WrapMode yWrap,
            FilterMode min,
            FilterMode mag,
            MipmapMode mips,
            boolean useAnisotropy, float anisotropy,
            float lodBias, float minLod, float maxLod
    ) {
        return img.createSampler(
                xWrap, yWrap,
                min, mag,
                mips,
                useAnisotropy, anisotropy,
                lodBias, minLod, maxLod
        );
    }

    public Image getImage() {
        return img;
    }

    // atlas generation variables
    // TODO: enable positioning glyphs in blank spaces between other glyphs or smth
    int lastX = 0;
    int lastY = 0;
    int rwHeight = 0;

    final int width, height;

    final ReniLogicalDevice logicalDevice;

    final CommandBuffer buffer;

    private final Image img;

    public Atlas(ReniLogicalDevice logicalDevice, int width, int height) {
        img = new Image(logicalDevice).setUsage(SwapchainUsage.GENERIC);
        img.create(this.width = width, this.height = height, VK13.VK_FORMAT_R8G8B8A8_SRGB);

        buffer = CommandBuffer.create(
                logicalDevice, ReniQueueType.GRAPHICS,
                true, false
        );
        this.logicalDevice = logicalDevice;
    }

    public boolean addGlyph(ReniGlyph glyph) {
        return add(glyph);
    }

    public static String read(InputStream is) {
        try {
            byte[] dat = is.readAllBytes();
            try {
                is.close();
            } catch (Throwable ignored) {
            }
            return new String(dat);
        } catch (Throwable err) {
            throw new RuntimeException(err);
        }
    }

    final HashMap<Character, float[]> glyphBounds = new HashMap<>();
    final HashMap<Integer, float[]> glyphBoundsByIndex = new HashMap<>();

    boolean add(ReniGlyph glyph) {
        if (glyphBoundsByIndex.containsKey(glyph.index)) {
            glyphBounds.put(glyph.symbol, glyphBoundsByIndex.get(glyph.index));
            return true;
        }

        if (lastX + glyph.width >= width) {
            lastX = 0;
            lastY += rwHeight;
            rwHeight = 0;
        }
        if (lastY + glyph.height >= height) return false;

        rwHeight = Math.max(glyph.height, rwHeight);

        blitGlyph(glyph);

        final float[] bounds = new float[4];
        bounds[0] = lastX / (float) width;
        bounds[1] = lastY / (float) height;
        bounds[2] = bounds[0] + glyph.width / (float) width;
        bounds[3] = bounds[1] + glyph.height / (float) height;
        glyphBounds.put(glyph.symbol, bounds);

        lastX += glyph.width;

        return true;
    }

    ArrayList<Runnable> rs = new ArrayList<>();

    private void blitGlyph(ReniGlyph glyph) {
        if (glyph.buffer == null)
            return;

        final ByteBuffer buf;
        final Texture tx = new Texture(
                logicalDevice, glyph.width, glyph.height,
                TextureChannels.RGBA, BitDepth.DEPTH_8,
                buf = ImageUtil.convertChannels(
                        glyph.buffer.position(0),
                        1, 4,
                        false,
                        ImageUtil.ConvertMode.BLACK_OPAQUE
                ),
                ImageLayout.TRANSFER_SRC_OPTIMAL
        );
        MemoryUtil.memFree(buf);
        rs.add(tx::destroy);

        final float left = lastX;
        final float top = lastY;
        final float right = left + glyph.width;
        final float bottom = top + glyph.height;

        buffer.copyImage(
                tx, ImageLayout.TRANSFER_SRC_OPTIMAL,
                0, 0,
                img, ImageLayout.TRANSFER_DST_OPTIMAL,
                (int) left, (int) top,
                (int) (right - left), (int) (bottom - top)
        );
    }

    public void destroy() {
        buffer.destroy();
        img.destroy();
    }

    public void reset() {
        lastX = 0;
        lastY = 0;
        rwHeight = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Atlas atlas = (Atlas) o;
        return lastX == atlas.lastX && lastY == atlas.lastY && rwHeight == atlas.rwHeight && width == atlas.width && height == atlas.height && Objects.equals(logicalDevice, atlas.logicalDevice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lastX, lastY, rwHeight, width, height, logicalDevice);
    }

    public float[] getBounds(char c) {
        return glyphBounds.get(c);
    }

    public void beginModifications() {
        buffer.begin();
        buffer.transition(
                img.getHandle(), StageMask.TOP_OF_PIPE, StageMask.TOP_OF_PIPE,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.TRANSFER_DST_OPTIMAL
        );
    }

    public void submit() {
        buffer.transition(
                img.getHandle(), StageMask.TOP_OF_PIPE, StageMask.TOP_OF_PIPE,
                ImageLayout.TRANSFER_DST_OPTIMAL, ImageLayout.SHADER_READONLY
        );
        buffer.end();
        buffer.submit(logicalDevice.getStandardQueue(ReniQueueType.GRAPHICS));
        logicalDevice.getStandardQueue(ReniQueueType.GRAPHICS).await();
        rs.forEach(Runnable::run);
        rs.clear();
    }
}
