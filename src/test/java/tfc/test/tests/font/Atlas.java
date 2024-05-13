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
import tfc.test.shared.ReniSetup;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

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
    final DescriptorSet set;

    final CommandBuffer buffer;
    final RenderPass pass;

    final long fbo;
    private final Image img;

    final ShaderCompiler compiler = new ShaderCompiler();
    final Shader VERT, FRAG;
    final VkExtent2D extents;
    final FrameBuffer frameBuffer;

    final GPUBuffer vbo;

    public Atlas(ReniLogicalDevice logicalDevice, int width, int height) {
        img = new Image(logicalDevice).setUsage(SwapchainUsage.GENERIC);
        img.create(this.width = width, this.height = height, VK13.VK_FORMAT_R8G8B8A8_SRGB);

        vbo = new GPUBuffer(
                logicalDevice, BufferUsage.VERTEX,
                4 * 4 * 6
        );
        vbo.allocate();

        buffer = CommandBuffer.create(
                logicalDevice, ReniQueueType.GRAPHICS,
                true, false
        );
        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.viewport(0, 0, width, height, 0, 1);
        state.scissor(0, 0, width, height);
        extents = VkExtent2D.calloc();
        extents.set(width, height);

        this.logicalDevice = logicalDevice;

        DataFormat format = VertexFormats.POS2_UV2;

        final BufferDescriptor desc0 = new BufferDescriptor(format);
        desc0.describe(0);
        desc0.attribute(0, 0, AttributeFormat.RG32_FLOAT, format.offset(VertexElements.POSITION_XY));
        desc0.attribute(0, 1, AttributeFormat.RG32_FLOAT, format.offset(VertexElements.UV0));
        state.vertexInput(desc0);

        pool = new DescriptorPool(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                1,
                new DescriptorPoolFlags[0],
                DescriptorPool.PoolInfo.of(DescriptorType.UNIFORM_BUFFER, 10)
        );
        DescriptorLayout layout;
        {
            final DescriptorLayoutInfo info = new DescriptorLayoutInfo(
                    0, DescriptorType.COMBINED_SAMPLED_IMAGE,
                    1, ShaderStageFlags.FRAGMENT
            );

            layout = new DescriptorLayout(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    0, info
            );

            info.destroy();
        }
        state.descriptorLayouts(layout);

        set = new DescriptorSet(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                pool, layout
        );

        compiler.setupGlsl();
        VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(LoadAtlas.class.getClassLoader().getResourceAsStream("shader/blit.vsh")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "blit_vert", "main"
        );
        FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(LoadAtlas.class.getClassLoader().getResourceAsStream("shader/blit.fsh")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "blit_frag", "main"
        );

        RenderPassInfo passInfo = new RenderPassInfo(logicalDevice);
        pass = passInfo.colorAttachment(
                Operation.PERFORM, Operation.PERFORM,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                VK13.VK_FORMAT_R8G8B8A8_SRGB
        ).dependency().subpass().create();
        passInfo.destroy();

        pipeline = new GraphicsPipeline(state, pass, VERT, FRAG);

//        layout.destroy();
//        desc0.destroy();

        frameBuffer = new FrameBuffer(logicalDevice, img);
        fbo = frameBuffer.forPass(pass);
    }

    DescriptorPool pool;
    GraphicsPipeline pipeline;

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

    HashMap<Character, float[]> glyphBounds = new HashMap<>();
    HashMap<Integer, float[]> glyphBoundsByIndex = new HashMap<>();

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

        float[] bounds = new float[4];
        bounds[0] = lastX / (float) width;
        bounds[1] = lastY / (float) height;
        bounds[2] = glyph.width / (float) width;
        bounds[3] = glyph.height / (float) height;
        glyphBounds.put(glyph.symbol, bounds);

        lastX += glyph.width;

        return true;
    }

    private void blitGlyph(ReniGlyph glyph) {
        if (glyph.buffer == null)
            return;

        float texelX = 1f / width;
        float texelY = 1f / height;

        Texture tx = new Texture(
                logicalDevice, glyph.width, glyph.height,
                TextureChannels.R, BitDepth.DEPTH_8, glyph.buffer.position(0)
        );

        TextureSampler sampler = tx.createSampler(
                WrapMode.CLAMP,
                WrapMode.CLAMP,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                MipmapMode.NEAREST,
                false, 0f,
                0f, 0f, 0f
        );
        ImageInfo info = new ImageInfo(tx, sampler);
        set.bind(0, 0, DescriptorType.COMBINED_SAMPLED_IMAGE, info);

        ByteBuffer data = vbo.createByteBuf();
        FloatBuffer fb = data.asFloatBuffer();

        float left = lastX;
        float top = lastY;
        float right = left + glyph.width;
        float bottom = top + glyph.height;

        left *= texelX;
        top *= texelY;
        right *= texelX;
        bottom *= texelY;
        fb.put(new float[]{
                right, bottom, 1, 1,
                left, bottom, 0, 1,
                left, top, 0, 0,

                right, top, 1, 0,
                right, bottom, 1, 1,
                left, top, 0, 0,
        });

//        buffer.begin();
//        buffer.bufferData(vbo, 0, 4 * 4 * 6, data);
//        buffer.end();
//        buffer.submit(logicalDevice.getStandardQueue(ReniQueueType.TRANSFER));
//        buffer.reset();
        vbo.upload(0, data);

        // TODO: optimize
        buffer.begin();

        buffer.startLabel("atlas", 0, 0.5f, 0, 0.5f);
        buffer.transition(
                img.getHandle(), StageMask.TOP_OF_PIPE, StageMask.GRAPHICS,
                ImageLayout.SHADER_READONLY, ImageLayout.COLOR_ATTACHMENT_OPTIMAL
        );

        buffer.noClear();
        buffer.beginPass(pass, fbo, extents);
        buffer.bindPipe(pipeline);
        buffer.bindDescriptor(BindPoint.GRAPHICS, pipeline, set);

        buffer.bindVbo(0, vbo);
        buffer.draw(0, 6);

        buffer.endPass();

        buffer.transition(
                img.getHandle(), StageMask.GRAPHICS, StageMask.TOP_OF_PIPE,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.SHADER_READONLY
        );

        buffer.endLabel();
        buffer.end();
        buffer.submit(logicalDevice.getStandardQueue(ReniQueueType.GRAPHICS));
        buffer.reset();

        info.destroy();
        sampler.destroy();
        tx.destroy();

        MemoryUtil.memFree(data);
    }

    public void destroy() {
        buffer.destroy();
        pass.destroy();
        pipeline.destroy();
        pool.destroy();
        set.destroy();
        VERT.destroy();
        FRAG.destroy();
        extents.free();
        vbo.destroy();
        VK13.nvkDestroyFramebuffer(logicalDevice.getDirect(VkDevice.class), fbo, 0);
        frameBuffer.destroy();
        img.destroy();
        compiler.destroy();
    }

    public void reset() {
        lastX = 0;
        lastY = 0;
        rwHeight = 0;
    }
}
