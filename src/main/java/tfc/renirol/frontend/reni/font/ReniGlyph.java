package tfc.renirol.frontend.reni.font;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.itf.ReniDestructable;

import java.nio.ByteBuffer;

public class ReniGlyph implements ReniDestructable {
    public final ByteBuffer buffer;
    public final int left, top;
    public final int width, height;
    public final long widthGlyph, heightGlyph;
    public final char symbol;
    public final int index;

    public final long advanceX, advanceY;
    public final short descent;

    public final long bbLeft, bbRight, bbTop, bbBottom;

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
        top = face.glyph().bitmap_top();
        final ByteBuffer bu = face.glyph().bitmap().buffer(
                (width = face.glyph().bitmap().width()) *
                        (height = face.glyph().bitmap().rows())
        );
        if (bu != null) {
            final ByteBuffer cpy = MemoryUtil.memAlloc(
                    width * height
            );
            MemoryUtil.memCopy(
                    bu,
                    cpy
            );
            buffer = cpy;
        } else buffer = null;

        advanceX = face.glyph().advance().x();
        advanceY = face.glyph().advance().y();
        widthGlyph = face.glyph().metrics().width();
        heightGlyph = face.glyph().metrics().height();
        descent = face.descender();

        bbLeft = face.bbox().xMin();
        bbRight = face.bbox().xMax();
        bbTop = face.bbox().yMin();
        bbBottom = face.bbox().yMax();
    }

    public void destroy() {
        MemoryUtil.memFree(buffer);
    }
}
