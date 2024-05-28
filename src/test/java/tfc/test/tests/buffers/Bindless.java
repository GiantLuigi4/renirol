package tfc.test.tests.buffers;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import tfc.renirol.frontend.enums.*;
import tfc.renirol.frontend.enums.flags.DescriptorBindingFlags;
import tfc.renirol.frontend.enums.flags.DescriptorPoolFlags;
import tfc.renirol.frontend.enums.flags.ShaderStageFlags;
import tfc.renirol.frontend.enums.format.AttributeFormat;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorLayout;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorLayoutInfo;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorPool;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorSet;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;
import tfc.test.shared.Scenario;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

class Bindless {
    public static void main(String[] args) {
        Scenario.FEATURES.add(tfc.renirol.frontend.hardware.device.feature.Bindless.INSTANCE);
        ReniSetup.initialize();

        final RenderPassInfo pass;
        {
            pass = new RenderPassInfo(ReniSetup.GRAPHICS_CONTEXT.getLogical(), ReniSetup.GRAPHICS_CONTEXT.getSurface());
            pass.colorAttachment(
                    Operation.CLEAR, Operation.PERFORM,
                    ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.PRESENT,
                    ReniSetup.selector
            );
        }

        final ShaderCompiler compiler = new ShaderCompiler();
        compiler.setupGlsl();
        final Shader VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(Bindless.class.getClassLoader().getResourceAsStream("test/bindless/shader.vert")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "vert",
                "main"
        );
        final Shader FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(Bindless.class.getClassLoader().getResourceAsStream("test/bindless/shader.frag")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "frag",
                "main"
        );

        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.dynamicState();

        DataFormat format = VertexFormats.POS4_COLOR4;

        final BufferDescriptor desc0 = new BufferDescriptor(format);
        desc0.describe(0);
        desc0.attribute(0, 0, AttributeFormat.RGB32_FLOAT, format.offset(VertexElements.POSITION_XYZW));
        desc0.attribute(0, 1, AttributeFormat.RGB32_FLOAT, format.offset(VertexElements.COLOR_RGBA));

        state.vertexInput(desc0);

        // TODO: ideally this stuff would be abstracted away more
        final DescriptorPool pool = new DescriptorPool(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                1,
                new DescriptorPoolFlags[]{DescriptorPoolFlags.UPDATER_AFTER_BIND},
                DescriptorPool.PoolInfo.of(DescriptorType.UNIFORM_BUFFER, 10)
        );
        DescriptorLayout layout;
        {
            final DescriptorLayoutInfo info = new DescriptorLayoutInfo(
                    0, DescriptorType.UNIFORM_BUFFER,
                    1, ShaderStageFlags.VERTEX
            ).flags(DescriptorBindingFlags.PARTIALLY_BOUND, DescriptorBindingFlags.UPDATE_AFTER_BIND);

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

        DataFormat uniformFormat = new DataFormat(VertexElements.COLOR_RGB);
        BufferDescriptor descriptor = new BufferDescriptor(uniformFormat);
        GPUBuffer uniformBuffer = new GPUBuffer(
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                descriptor, BufferUsage.UNIFORM,
                1
        );
        uniformBuffer.allocate();
        set.bind(0, 0, DescriptorType.UNIFORM_BUFFER, uniformBuffer);

        GraphicsPipeline pipeline0 = new GraphicsPipeline(pass, state, VERT, FRAG);

        final GPUBuffer vbo = new GPUBuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical(), desc0, BufferUsage.VERTEX, 4);
        final GPUBuffer ibo = new GPUBuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical(), IndexSize.INDEX_16, BufferUsage.INDEX, 6);
        vbo.allocate();
        ibo.allocate();
        final ByteBuffer buffer1 = vbo.createByteBuf();
        final ByteBuffer indices = ibo.createByteBuf();
        indices.asShortBuffer().put(new short[]{
                0, 1, 2,
                3, 1, 0,
        });
        ibo.upload(0, indices);
        MemoryUtil.memFree(indices);

        ByteBuffer uniformBBuf = uniformBuffer.createByteBuf();

        try {
            int frame = 0;

            ReniSetup.WINDOW.grabContext();
            final CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniQueueType.GRAPHICS, true,
                    false
            );
            buffer.clearColor(0, 0, 0, 1);

            while (!ReniSetup.WINDOW.shouldClose()) {
                frame++;

                {
                    uniformBBuf.position(0).asFloatBuffer().put(new float[]{
                            1.0f, 1.0f, 1.0f
                    });
                    uniformBuffer.upload(0, uniformBBuf);
                }
                {
                    final FloatBuffer fb = buffer1.position(0).asFloatBuffer();
                    fb.put(
                            0,
                            new float[]{
                                    (float) -Math.cos(Math.toRadians(frame)), (float) -Math.sin(Math.toRadians(frame)), 0, 0,
                                    1, 0, 0, 1,

                                    (float) Math.cos(Math.toRadians(frame)), (float) Math.sin(Math.toRadians(frame)), 0, 0,
                                    0, 1, 0, 1,

                                    (float) Math.cos(Math.toRadians(frame + 90)), (float) Math.sin(Math.toRadians(frame + 90)), 0, 0,
                                    0, 0, 0, 1,

                                    (float) -Math.cos(Math.toRadians(frame + 90)), (float) -Math.sin(Math.toRadians(frame + 90)), 0, 0,
                                    1, 1, 0, 1,
                            }
                    );
                    vbo.upload(0, buffer1);
                }

                ReniSetup.GRAPHICS_CONTEXT.prepareFrame(ReniSetup.WINDOW);

                buffer.begin();

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
                        0, 6
                );
                buffer.endPass();
                buffer.endLabel();
                buffer.end();

                ReniSetup.GRAPHICS_CONTEXT.submitFrame(buffer);
                ReniSetup.GRAPHICS_CONTEXT.getLogical().getStandardQueue(ReniQueueType.GRAPHICS).await();

                ReniSetup.WINDOW.swapAndPollSize();
                GLFWWindow.poll();

                ReniSetup.GRAPHICS_CONTEXT.getLogical().waitForIdle();
            }
            buffer.destroy();
        } catch (Throwable err) {
            err.printStackTrace();
        }

        layout.destroy();
        pool.destroy();
        MemoryUtil.memFree(buffer1);
        ibo.destroy();
        vbo.destroy();
        ReniSetup.GRAPHICS_CONTEXT.getLogical().waitForIdle();
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
