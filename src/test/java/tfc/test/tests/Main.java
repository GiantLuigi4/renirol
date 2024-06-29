package tfc.test.tests;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.enums.masks.AccessMask;
import tfc.renirol.frontend.enums.masks.DynamicStateMasks;
import tfc.renirol.frontend.enums.masks.StageMask;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;

import java.io.InputStream;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        ReniSetup.initialize();

        RenderPassInfo pass;
        {
            pass = new RenderPassInfo(ReniSetup.GRAPHICS_CONTEXT.getLogical(), ReniSetup.GRAPHICS_CONTEXT.getSurface());
            pass.colorAttachment(
                    Operation.CLEAR, Operation.PERFORM,
                    ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.PRESENT,
                    ReniSetup.selector
            );
        }

        ShaderCompiler compiler = new ShaderCompiler();
        compiler.setupGlsl();
        Shader VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(Main.class.getClassLoader().getResourceAsStream("shader.vert")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "vert",
                "main"
        );
        Shader FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(Main.class.getClassLoader().getResourceAsStream("shader.frag")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "frag",
                "main"
        );

        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.dynamicState(DynamicStateMasks.VIEWPORT, DynamicStateMasks.SCISSOR);
        GraphicsPipeline pipeline0 = new GraphicsPipeline(pass, state, VERT, FRAG);

        try {
            ReniSetup.WINDOW.grabContext();
            CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniSetup.GRAPHICS_CONTEXT.getLogical().getQueueFamily(ReniQueueType.GRAPHICS), true,
                    false
            );
            buffer.clearColor(0, 0, 0, 1);

            while (!ReniSetup.WINDOW.shouldClose()) {
                ReniSetup.GRAPHICS_CONTEXT.prepareFrame(ReniSetup.WINDOW);

                buffer.begin();

                buffer.transition(
                        ReniSetup.GRAPHICS_CONTEXT.getFramebuffer(),
                        StageMask.TOP_OF_PIPE,
                        StageMask.COLOR_ATTACHMENT_OUTPUT,
                        ImageLayout.UNDEFINED,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        AccessMask.NONE,
                        AccessMask.COLOR_WRITE
                );

                buffer.startLabel("Main Pass", 0.5f, 0, 0, 0.5f);
                buffer.beginPass(pass, ReniSetup.GRAPHICS_CONTEXT.getChainBuffer(), ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents());
                buffer.bindPipe(pipeline0);
                buffer.viewportScissor(
                        0, 0,
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().width(),
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().height(),
                        0f, 1f
                );
                buffer.drawInstanced(0, 3, 0, new Random().nextInt(18) + 1);
                buffer.endPass();
                buffer.endLabel();

                buffer.transition(
                        ReniSetup.GRAPHICS_CONTEXT.getFramebuffer(),
                        StageMask.COLOR_ATTACHMENT_OUTPUT,
                        StageMask.BOTTOM_OF_PIPE,
                        ImageLayout.COLOR_ATTACHMENT_OPTIMAL,
                        ImageLayout.PRESENT,
                        AccessMask.COLOR_WRITE,
                        AccessMask.NONE
                );

                buffer.end();

                ReniSetup.GRAPHICS_CONTEXT.submitFrame(
                        ReniSetup.GRAPHICS_CONTEXT.getLogical().getStandardQueue(ReniQueueType.GRAPHICS),
                        buffer
                );

                ReniSetup.WINDOW.swapAndPollSize();
                GLFWWindow.poll();

                ReniSetup.GRAPHICS_CONTEXT.getLogical().await();
            }
            buffer.destroy();
        } catch (Throwable err) {
            err.printStackTrace();
        }

        ReniSetup.GRAPHICS_CONTEXT.getLogical().await();
        FRAG.destroy();
        VERT.destroy();
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
