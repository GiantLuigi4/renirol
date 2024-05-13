package tfc.renirol.frontend.reni.font;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.frontend.hardware.util.ReniDestructable;

import java.nio.ByteBuffer;

public class ReniGlyph implements ReniDestructable {
    public final ByteBuffer buffer;
    public final int left, top;
    public final int width, height;
    public final char symbol;
    public final int index;

    public ReniGlyph(char symbol, FT_Face face, int index, int flags) {
        this.index = index;
        this.symbol = symbol;
        int error = FreeType.FT_Load_Glyph(
                face, index,
                flags
        );
        if (error != 0) throw new RuntimeException("Failed to load glyph: " + FreeType.FT_Error_String(error));
        error = FreeType.FT_Render_Glyph(
                face.glyph(),
                FreeType.FT_RENDER_MODE_SDF
        );
        if (error != 0) throw new RuntimeException("Failed to render glyph: " + FreeType.FT_Error_String(error));

        left = face.glyph().bitmap_left();
        top = face.glyph().bitmap_left();
        buffer = face.glyph().bitmap().buffer(
                (width = face.glyph().bitmap().width()) *
                        (height = face.glyph().bitmap().rows())
        );
    }

    public void destroy() {
        MemoryUtil.memFree(buffer);
    }
}
