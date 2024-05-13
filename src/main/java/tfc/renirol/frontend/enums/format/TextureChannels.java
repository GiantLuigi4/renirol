package tfc.renirol.frontend.enums.format;

import org.lwjgl.stb.STBImage;
import org.lwjgl.vulkan.VK13;

public enum TextureChannels {
    R(1, STBImage.STBI_grey),
    GRAY(1, STBImage.STBI_grey),
    RG(2, STBImage.STBI_grey_alpha),
    GRAY_ALPHA(2, STBImage.STBI_grey_alpha),
    RGB(3, STBImage.STBI_rgb),
    RGBA(4, STBImage.STBI_rgb_alpha),
    DEFAULT(-1, STBImage.STBI_default),
    ;

    public final int count;
    public final int stbi;

    TextureChannels(int count, int stbi) {
        this.count = count;
        this.stbi = stbi;
    }
}
