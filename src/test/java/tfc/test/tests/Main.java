package tfc.test.tests;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;

import java.io.InputStream;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        ReniSetup.initialize();

        RenderPass pass;
        {
            RenderPassInfo info = new RenderPassInfo(ReniSetup.GRAPHICS_CONTEXT.getLogical(), ReniSetup.GRAPHICS_CONTEXT.getSurface());
            pass = info.colorAttachment(
                    Operation.DONT_CARE, Operation.PERFORM,
                    ImageLayout.COLOR_ATTACHMENT_OPTIMAL, ImageLayout.PRESENT,
                    ReniSetup.selector
            ).dependency().subpass().create();
            info.destroy();
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
        state.dynamicState();
        GraphicsPipeline pipeline0 = new GraphicsPipeline(state, pass, VERT, FRAG);

        try {
            ReniSetup.WINDOW.grabContext();
            CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniQueueType.GRAPHICS, true,
                    false
            );
            buffer.noClear();

            while (!ReniSetup.WINDOW.shouldClose()) {
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
                buffer.drawInstanced(0, 3, 0, new Random().nextInt(18) + 1);
                buffer.endPass();
                buffer.endLabel();
                buffer.end();

                ReniSetup.GRAPHICS_CONTEXT.submitFrame(buffer);

                ReniSetup.WINDOW.swapAndPollSize();
                GLFWWindow.poll();

                ReniSetup.GRAPHICS_CONTEXT.getLogical().waitForIdle();

                VK13.nvkDestroyFramebuffer(ReniSetup.GRAPHICS_CONTEXT.getLogical().getDirect(VkDevice.class), fbo, 0);
            }
            buffer.destroy();
        } catch (Throwable err) {
            err.printStackTrace();
        }

        ReniSetup.GRAPHICS_CONTEXT.getLogical().waitForIdle();
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
