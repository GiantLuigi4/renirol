package tfc.test.tests.shader;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.enums.format.AttributeFormat;
import tfc.renirol.frontend.rendering.enums.BufferUsage;
import tfc.renirol.frontend.rendering.enums.ImageLayout;
import tfc.renirol.frontend.rendering.enums.Operation;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class HLSL {
    public static void main(String[] args) {
        ReniSetup.initialize();

        RenderPass pass;
        {
            RenderPassInfo info = new RenderPassInfo(ReniSetup.GRAPHICS_CONTEXT.getLogical(), ReniSetup.GRAPHICS_CONTEXT.getSurface());
            pass = info.colorAttachment(
                    Operation.CLEAR, Operation.PERFORM,
                    ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.PRESENT,
                    ReniSetup.selector
            ).dependency().subpass().create();
            info.destroy();
        }

        ShaderCompiler compiler = new ShaderCompiler();
        compiler.setupHlsl();
        Shader VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(HLSL.class.getClassLoader().getResourceAsStream("test/hlsl/shader.vert.hlsl")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "vert",
                "main"
        );
        Shader FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(HLSL.class.getClassLoader().getResourceAsStream("test/hlsl/shader.frag.hlsl")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "frag",
                "main"
        );

        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.dynamicState();

        DataFormat format = VertexFormats.POS4_COLOR4;

        BufferDescriptor desc0 = new BufferDescriptor(format);
        desc0.describe(0);
        desc0.attribute(0, 0, AttributeFormat.RGB32_FLOAT, format.offset(VertexElements.POSITION_XYZW));
        desc0.attribute(0, 1, AttributeFormat.RGB32_FLOAT, format.offset(VertexElements.COLOR_RGBA));

        state.vertexInput(desc0);

        GraphicsPipeline pipeline0 = new GraphicsPipeline(state, pass, VERT, FRAG);

        GPUBuffer vbo = new GPUBuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical(), desc0, BufferUsage.VERTEX, 6);
        vbo.allocate();
        ByteBuffer buffer1 = vbo.createByteBuf();

        try {
            int frame = 0;

            ReniSetup.WINDOW.grabContext();
            CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniQueueType.GRAPHICS, true,
                    false
            );
            buffer.clearColor(0, 0, 0, 1);

            while (!ReniSetup.WINDOW.shouldClose()) {
                frame++;

                {
                    FloatBuffer fb = buffer1.position(0).asFloatBuffer();
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

                                    (float) Math.cos(Math.toRadians(frame)), (float) Math.sin(Math.toRadians(frame)), 0, 0,
                                    0, 1, 0, 1,

                                    (float) -Math.cos(Math.toRadians(frame)), (float) -Math.sin(Math.toRadians(frame)), 0, 0,
                                    1, 0, 0, 1,
                            }
                    );
                    vbo.upload(0, buffer1);
                }

                ReniSetup.GRAPHICS_CONTEXT.prepareFrame(ReniSetup.WINDOW);
                long fbo = ReniSetup.GRAPHICS_CONTEXT.getFrameHandle(pass);

                buffer.begin();

                buffer.startLabel("Main Pass", 0.5f, 0, 0, 0.5f);
                buffer.beginPass(pass, fbo, ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents());

                buffer.bindPipe(pipeline0);
                buffer.viewportScissor(
                        0, 0,
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().width(),
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().height(),
                        0f, 1f
                );
                buffer.bindVbo(0, vbo);
                buffer.draw(0, 6);
                buffer.endPass();
                buffer.endLabel();
                buffer.end();

                ReniSetup.GRAPHICS_CONTEXT.submitFrame(buffer);
                ReniSetup.GRAPHICS_CONTEXT.getLogical().getStandardQueue(ReniQueueType.GRAPHICS).await();

                ReniSetup.WINDOW.swapAndPollSize();
                GLFWWindow.poll();

                VK13.nvkDestroyFramebuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical().getDirect(VkDevice.class), fbo, 0);
            }
            buffer.destroy();
        } catch (Throwable err) {
            err.printStackTrace();
        }

        MemoryUtil.memFree(buffer1);
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
