package tfc.renirol.util;

import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;

public class ShaderCompiler {
    private final long compiler;
    private final long options;

    public ShaderCompiler() {
        compiler = Shaderc.shaderc_compiler_initialize();
        options = Shaderc.shaderc_compile_options_initialize();
        Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_performance);
        Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_3);
        Shaderc.shaderc_compile_options_set_target_spirv(options, Shaderc.shaderc_spirv_version_1_6);
    }

    public ShaderCompiler setupGlsl() {
        Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_glsl);
        return this;
    }

    public ShaderCompiler setupHlsl() {
        Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_hlsl);
        return this;
    }

    public ShaderCompiler debug() {
        Shaderc.shaderc_compile_options_set_generate_debug_info(options);
        return this;
    }

    public CompilationResult compile(String text, int kind, String name, String entryPoint) {
        long result = Shaderc.shaderc_compile_into_spv(
                compiler,
                text,
                kind,
                name,
                entryPoint,
                options
        );

        return new CompilationResult(result);
    }

    public void destroy() {
        Shaderc.shaderc_compile_options_release(options);
        Shaderc.shaderc_compiler_release(compiler);
    }
}
