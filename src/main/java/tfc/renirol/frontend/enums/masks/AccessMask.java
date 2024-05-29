package tfc.renirol.frontend.enums.masks;

import org.lwjgl.vulkan.VK13;

public enum AccessMask {
    NONE(VK13.VK_ACCESS_NONE),
    SHADER_READ(VK13.VK_ACCESS_SHADER_READ_BIT),
    SHADER_WRITE(VK13.VK_ACCESS_SHADER_WRITE_BIT),
    COLOR_WRITE(VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT),
    COLOR_READ(VK13.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT),
    TRANSFER_READ(VK13.VK_ACCESS_TRANSFER_READ_BIT),
    TRANSFER_WRITE(VK13.VK_ACCESS_TRANSFER_WRITE_BIT),
    DEPTH_WRITE(VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT),
    DEPTH_READ(VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT),
    ;
    public final int value;

    AccessMask(int value) {
        this.value = value;
    }
}
