package tfc.test.tests.font;

import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.frontend.reni.font.ReniFont;
import tfc.renirol.frontend.reni.font.ReniGlyph;

import java.io.InputStream;

public class FontTest {
    public static void main(String[] args) {
        InputStream is = FontDraw.class.getClassLoader().getResourceAsStream("test/font/font.ttf");
        ReniFont font = new ReniFont(is);
        font.setPixelSizes(0, 64);
        ReniGlyph glyph = font.glyph('a', FreeType.FT_LOAD_DEFAULT);
        STBImageWrite.stbi_write_png(
                "test.png",
                glyph.width, glyph.height,
                1, glyph.buffer,
                0
        );
    }
}
