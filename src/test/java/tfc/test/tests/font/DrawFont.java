package tfc.test.tests.font;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import tfc.renirol.frontend.enums.ImageLayout;
import tfc.renirol.frontend.enums.Operation;
import tfc.renirol.frontend.enums.masks.DynamicStateMasks;
import tfc.renirol.frontend.hardware.device.ReniQueueType;
import tfc.renirol.frontend.hardware.device.queue.ReniQueue;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.command.shader.Shader;
import tfc.renirol.frontend.rendering.pass.RenderPassInfo;
import tfc.renirol.frontend.reni.font.ReniFont;
import tfc.renirol.frontend.windowing.glfw.GLFWWindow;
import tfc.renirol.util.ShaderCompiler;
import tfc.test.shared.ReniSetup;

import java.io.InputStream;

public class DrawFont {
    public static void main(String[] args) {
        ReniSetup.initialize();

        final RenderPassInfo startPass = ReniSetup.GRAPHICS_CONTEXT.getPass(
                Operation.CLEAR, Operation.PERFORM,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL
        );
        final RenderPassInfo pass = ReniSetup.GRAPHICS_CONTEXT.getPass(
                Operation.PERFORM, Operation.PERFORM,
                ImageLayout.COLOR_ATTACHMENT_OPTIMAL
        );

        final ShaderCompiler compiler = new ShaderCompiler();
        compiler.setupGlsl();
        final Shader VERT = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(DrawFont.class.getClassLoader().getResourceAsStream("test/font/f2/shader.vert")),
                Shaderc.shaderc_glsl_vertex_shader,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                "vert",
                "main"
        );
        final Shader FRAG = new Shader(
                compiler,
                ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                read(DrawFont.class.getClassLoader().getResourceAsStream("test/font/f2/shader.frag")),
                Shaderc.shaderc_glsl_fragment_shader,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                "frag",
                "main"
        );

        PipelineState state = new PipelineState(ReniSetup.GRAPHICS_CONTEXT.getLogical());
        state.dynamicState(DynamicStateMasks.SCISSOR, DynamicStateMasks.VIEWPORT);

        ReniFont font;
        TextRenderer renderer;
        {
            InputStream is = DrawFont.class.getClassLoader().getResourceAsStream("test/font/font.ttf");
//            InputStream is = DrawFont.class.getClassLoader().getResourceAsStream("test/font/help.ttf");
            font = new ReniFont(is);
            renderer = new TextRenderer(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(), font,
                    1024, 1024
//                    64, 79 // minimum usable without the program crashing with jetbrains mono regular
            );
            font.setPixelSizes(0, 64);
            state.descriptorLayouts(renderer.getLayout());

            try {
                is.close();
            } catch (Throwable ignored) {
            }

            renderer.preload(' ');
            renderer.preload('\t');
            for (char c = 'a'; c <= 'z'; c++) renderer.preload(c);
            for (char c = 'A'; c <= 'Z'; c++) renderer.preload(c);
            for (char c = '0'; c <= '9'; c++) renderer.preload(c);

            char[] special = ",.!?'\";:-=_+~`@#$%^&*()[]{}\\|".toCharArray();
            for (char c : special) renderer.preload(c);
        }

        renderer.bind(state);
        state.constantBuffer(VK13.VK_SHADER_STAGE_VERTEX_BIT, 4);
        GraphicsPipeline pipeline0 = new GraphicsPipeline(pass, state, VERT, FRAG);

        ReniQueue queue = ReniSetup.GRAPHICS_CONTEXT.getLogical().getStandardQueue(ReniQueueType.GRAPHICS);

        try {
            ReniSetup.WINDOW.grabContext();
            final CommandBuffer buffer = CommandBuffer.create(
                    ReniSetup.GRAPHICS_CONTEXT.getLogical(),
                    ReniSetup.GRAPHICS_CONTEXT.getLogical().getQueueFamily(ReniQueueType.GRAPHICS), true,
                    false
            ).setName("Main Command Buffer");

            while (!ReniSetup.WINDOW.shouldClose()) {
                ReniSetup.GRAPHICS_CONTEXT.prepareFrame(ReniSetup.WINDOW);

                buffer.begin();

                ReniSetup.GRAPHICS_CONTEXT.prepareChain(buffer);

                buffer.startLabel("Main Pass", 0.5f, 0, 0, 0.5f);
                buffer.clearColor(0, 0, 0, 1);
                buffer.beginPass(startPass, ReniSetup.GRAPHICS_CONTEXT.getChainBuffer(), ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents());
                buffer.noClear();

                buffer.bindPipe(pipeline0);
                buffer.viewportScissor(
                        0, 0,
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().width(),
                        ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents().height(),
                        0f, 1f
                );
                buffer.startLabel("Draw Text", 0, 0.5f, 0, 0.5f);
                renderer.draw(() -> {
                    buffer.beginPass(pass, ReniSetup.GRAPHICS_CONTEXT.getChainBuffer(), ReniSetup.GRAPHICS_CONTEXT.defaultSwapchain().getExtents());
                }, "Hello, how are you doing today! \"←\"\nHi!! I am a second ililelne\nDon't worry about the illusion\nI can draw infinite lines of font (not really)\neeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n\nThis text is using 13600 bytes to tell the GPU what\nto draw.", buffer, pipeline0);
                buffer.endLabel();
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

        renderer.destroy();
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
