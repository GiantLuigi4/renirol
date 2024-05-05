package tfc.renirol.util;

import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;

public class CompilationResult {
    long handle;

    public CompilationResult(long handle) {
        this.handle = handle;
    }

    public boolean isSuccess() {
        return Shaderc.shaderc_result_get_compilation_status(handle) == Shaderc.shaderc_compilation_status_success;
    }

    public void printLog() {
        String err = Shaderc.shaderc_result_get_error_message(handle);
        if (!err.isBlank())
            System.err.println(err);
    }

    public ByteBuffer getBytes() {
        return Shaderc.shaderc_result_get_bytes(handle);
    }

    public void free() {
        Shaderc.shaderc_result_release(handle);
    }
}
