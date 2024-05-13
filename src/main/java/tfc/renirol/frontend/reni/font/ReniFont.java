package tfc.renirol.frontend.reni.font;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap_Size;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.backend.vk.util.VkUtil;
import tfc.renirol.util.ReadOnlyList;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ReniFont {
    public static final PointerBuffer libraryPtr = MemoryUtil.memAllocPointer(1);
    public static final long library;

    static {
        int error = FreeType.FT_Init_FreeType(libraryPtr);
        if (error != 0) System.out.println("Error loading FreeType: " + FreeType.FT_Error_String(error));
        library = libraryPtr.get(0);
    }

    FT_Face face;

    static long index;

    private final ReadOnlyList<FontSize> sizes;

    public ReniFont(InputStream is) {
        ByteBuffer buffer = VkUtil.read(is);
        sizes = load(buffer);
//        MemoryUtil.memFree(buffer);
    }

    public ReniFont(ByteBuffer data) {
        sizes = load(data);
    }

    PointerBuffer pFace;

    protected ReadOnlyList<FontSize> load(ByteBuffer data) {
        final PointerBuffer face = MemoryUtil.memAllocPointer(1);
        pFace = face;
        int error = FreeType.FT_New_Memory_Face(
                library, data,
                index++, face
        );
        if (error != 0) {
            throw new RuntimeException("Error loading font: " + FreeType.FT_Error_String(error));
        }
        this.face = FT_Face.create(face.get(0));

        FT_Bitmap_Size.Buffer buf = this.face.available_sizes();
        List<FontSize> builder = new ArrayList<>();
        if (buf != null) {
            for (int i = 0; i < buf.capacity(); i++) {
                FT_Bitmap_Size size = buf.get(i);
                builder.add(new FontSize(
                        size.width(), size.height(),
                        size.x_ppem(), size.y_ppem()
                ));
            }
        }
        return new ReadOnlyList<>(builder);
    }

    public void setPixelSizes(int width, int height) {
        int error = FreeType.FT_Set_Pixel_Sizes(
                face, width, height
        );
        if (error != 0) throw new RuntimeException("Failed to set pixel sizes: " + FreeType.FT_Error_String(error));
    }

    public ReniGlyph glyph(char code, int flags) {
        return new ReniGlyph(
                code, face, FreeType.FT_Get_Char_Index(face, code), flags
        );
    }

    public ReadOnlyList<FontSize> sizes() {
        return sizes;
    }

    public long glyphCount() {
        return face.num_glyphs();
    }

    public short unitsPerEM() {
        return face.units_per_EM();
    }
}
