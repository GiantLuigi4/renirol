package tfc.test.tests.dim;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import tfc.renirol.frontend.enums.*;
import tfc.renirol.frontend.enums.flags.DescriptorPoolFlags;
import tfc.renirol.frontend.enums.flags.ShaderStageFlags;
import tfc.renirol.frontend.enums.format.AttributeFormat;
import tfc.renirol.frontend.enums.format.BitDepth;
import tfc.renirol.frontend.enums.format.TextureChannels;
import tfc.renirol.frontend.enums.format.TextureFormat;
import tfc.renirol.frontend.enums.masks.DynamicStateMasks;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.device.queue.ReniQueue;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.*;
import tfc.renirol.frontend.rendering.resource.image.texture.Texture;
import tfc.renirol.frontend.rendering.resource.image.texture.TextureSampler;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;
import tfc.test.shared.Scenario;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class DepthBuffer {
    public static void main(String[] args) {
        Scenario.useDepth = true;
        ReniSetup.initialize();

        final RenderPassInfo pass = ReniSetup.GRAPHICS_CONTEXT.getPass(
                Operation.CLEAR, Operation.PERFORM,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL
        );

        final ShaderCompiler compiler = new ShaderCompiler();
        compiler.setupGlsl();
        final Shader VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(DepthBuffer.class.getClassLoader().getResourceAsStream("test/depth/shader.vert")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "vert",
                "main"
        );
        final Shader FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(DepthBuffer.class.getClassLoader().getResourceAsStream("test/depth/shader.frag")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "frag",
                "main"
        );

        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.depthTest(true).depthMask(true);
        state.dynamicState(DynamicStateMasks.SCISSOR, DynamicStateMasks.VIEWPORT);

        DataFormat format = VertexFormats.POS4_UV2;

        final BufferDescriptor desc0 = new BufferDescriptor(format);
        desc0.describe(0);
        desc0.attribute(0, 0, AttributeFormat.RGB32_FLOAT, format.offset(VertexElements.POSITION_XYZW));
        desc0.attribute(0, 1, AttributeFormat.RG32_FLOAT, format.offset(VertexElements.UV0));

        state.vertexInput(desc0);

        // TODO: ideally this stuff would be abstracted away more
        final DescriptorPool pool = new DescriptorPool(
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
        DescriptorSet set = new DescriptorSet(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                pool, layout
        );
        state.descriptorLayouts(layout);

        final GPUBuffer vbo = new GPUBuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical(), desc0, BufferUsage.VERTEX, 8);
        final GPUBuffer ibo = new GPUBuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical(), IndexSize.INDEX_16, BufferUsage.INDEX, 12);
        vbo.allocate();
        ibo.allocate();
        final ByteBuffer buffer1 = vbo.createByteBuf();
        final ByteBuffer indices = ibo.createByteBuf();
        indices.asShortBuffer().put(new short[]{
                0, 1, 2,
                3, 1, 0,

                4, 5, 6,
                7, 5, 4,
        });
        ibo.upload(0, indices);
        MemoryUtil.memFree(indices);

        InputStream is = DepthBuffer.class.getClassLoader().getResourceAsStream("test/texture/texture.png");
        Texture texture = new Texture(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                TextureFormat.PNG, TextureChannels.RGBA,
                BitDepth.DEPTH_8, is
        );
        TextureSampler sampler = texture.createSampler(
                WrapMode.BORDER,
                WrapMode.BORDER,
                FilterMode.NEAREST,
                FilterMode.NEAREST,
                MipmapMode.NEAREST,
                true, 16f,
                0f, 0f, 0f
        );
        ImageInfo info = new ImageInfo(texture, sampler);
        set.bind(0, 0, DescriptorType.COMBINED_SAMPLED_IMAGE, info);

        GraphicsPipeline pipeline0 = new GraphicsPipeline(pass, state, VERT, FRAG);

        try {
            is.close();
        } catch (Throwable ignored) {
        }

        ReniQueue queue = ReniSetup.GRAPHICS_CONTEXT.getLogical().getStandardQueue(ReniQueueType.GRAPHICS);

        try {
            int frame = 0;

            ReniSetup.WINDOW.grabContext();
            final CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniSetup.GRAPHICS_CONTEXT.getLogical().getQueueFamily(ReniQueueType.GRAPHICS), true,
                    false
            );
            buffer.clearColor(0, 0, 0, 1);
            buffer.clearDepth(1f);

            while (!ReniSetup.WINDOW.shouldClose()) {
                frame++;

                {
                    final FloatBuffer fb = buffer1.position(0).asFloatBuffer();
                    fb.put(
                            0,
                            new float[]{
                                    1, 0, 0.5f, 0,
                                    1, 0,

                                    0, 1, 0.5f, 0,
                                    0, 1,

                                    0, 0, 0.5f, 0,
                                    0, 0,

                                    1, 1, 0.5f, 0,
                                    1, 1,

                                    0.5f, 0, 0, 0,
                                    1, 0,

                                    0, 0.5f, 1, 0,
                                    0, 1,

                                    0, 0, 1, 0,
                                    0, 0,

                                    0.5f, 0.5f, 0, 0,
                                    1, 1,
                            }
                    );
                    vbo.upload(0, buffer1);
                }

                ReniSetup.GRAPHICS_CONTEXT.prepareFrame(ReniSetup.WINDOW);

                buffer.begin();

                ReniSetup.GRAPHICS_CONTEXT.prepareChain(buffer);

                buffer.startLabel("Main Pass", 0.5f, 0, 0, 0.5f);
                buffer.beginPass(pass, ReniSetup.GRAPHICS_CONTEXT.getChainBuffer(), ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents());

                buffer.bindPipe(pipeline0);
                buffer.bindDescriptor(BindPoint.GRAPHICS, pipeline0, set);
                buffer.viewportScissor(
                        0, 0,
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().width(),
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().height(),
                        0f, 1f
                );
                buffer.bindVbo(0, vbo);
                buffer.bindIbo(IndexSize.INDEX_16, ibo);
                buffer.drawIndexed(
                        0,
                        0, 1,
                        0, 12
                );
                buffer.endPass();
                buffer.endLabel();

                ReniSetup.GRAPHICS_CONTEXT.preparePresent(buffer);

                buffer.end();

                ReniSetup.GRAPHICS_CONTEXT.submitFrame(queue, buffer);

                ReniSetup.WINDOW.swapAndPollSize();
                GLFWWindow.poll();

                ReniSetup.GRAPHICS_CONTEXT.getLogical().await();
            }
            buffer.destroy();
        } catch (Throwable err) {
            err.printStackTrace();
        }

        info.destroy();
        sampler.destroy();
        texture.destroy();
        layout.destroy();
        pool.destroy();
        MemoryUtil.memFree(buffer1);
        ibo.destroy();
        vbo.destroy();
        ReniSetup.GRAPHICS_CONTEXT.getLogical().await();
        FRAG.destroy();
        VERT.destroy();
        desc0.destroy();
        pipeline0.destroy();
        pass.destroy();
        ReniSetup.GRAPHICS_CONTEXT.destroy();
        ReniSetup.WINDOW.dispose();
        GLFW.glfwTerminate();
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
}
