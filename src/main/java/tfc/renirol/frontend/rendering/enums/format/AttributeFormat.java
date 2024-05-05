package tfc.renirol.frontend.rendering.enums.format;

import static org.lwjgl.vulkan.VK10.*;

public enum AttributeFormat {
    R32_FLOAT(VK_FORMAT_R32_SFLOAT),
    RG32_FLOAT(VK_FORMAT_R32G32_SFLOAT),
    RGB32_FLOAT(VK_FORMAT_R32G32B32_SFLOAT),
    RGBA32_FLOAT(VK_FORMAT_R32G32B32A32_SFLOAT),

    RGB8_UINT(VK_FORMAT_R8G8B8_UINT),
    RGB16_UINT(VK_FORMAT_R16G16B16_UINT),
    RGB32_UINT(VK_FORMAT_R32G32B32_UINT),
    RGBA8_UINT(VK_FORMAT_R8G8B8A8_UINT),
    RGBA16_UINT(VK_FORMAT_R16G16B16A16_UINT),
    RGBA32_UINT(VK_FORMAT_R32G32B32A32_UINT);

    public final int id;

    AttributeFormat(int id) {
        this.id = id;
    }
}
