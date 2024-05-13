package tfc.test.tests.font;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.frontend.enums.BufferUsage;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorSet;
import tfc.renirol.frontend.rendering.resource.descriptor.ImageInfo;
import tfc.renirol.frontend.rendering.resource.image.texture.TextureSampler;
import tfc.renirol.frontend.reni.font.ReniFont;
import tfc.renirol.frontend.reni.font.ReniGlyph;
import tfc.test.shared.VertexFormats;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class TextRenderer implements ReniDestructable {
    public final ReniFont font;

    public void destroy() {
        for (AtlasInfo atlas : atlases) {
            atlas.atlas.destroy();
            atlas.info.destroy();
            atlas.sampler.destroy();
        }
    }

    GPUBuffer quad;

    public void draw(
            String text,
            CommandBuffer buffer,
            GraphicsPipeline pipeline0,
            DescriptorSet set
    ) {
    }

    private record AtlasInfo(Atlas atlas, TextureSampler sampler, ImageInfo info) {
        private static AtlasInfo create(Atlas atlas) {
            TextureSampler sampler = atlas.createSampler(
                    WrapMode.BORDER,
                    WrapMode.BORDER,
                    FilterMode.LINEAR,
                    FilterMode.LINEAR,
                    MipmapMode.NEAREST,
                    false, 16f,
                    0f, 0f, 0f
            );
            ImageInfo info = new ImageInfo(atlas.getImage(), sampler);
            return new AtlasInfo(atlas, sampler, info);
        }

        public boolean addGlyph(ReniGlyph glyph) {
            return atlas.addGlyph(glyph);
        }
    }

    private final List<AtlasInfo> atlases = new ArrayList<>();

    private final int atlasWidth, atlasHeight;

    private final ReniLogicalDevice device;

    public TextRenderer(ReniLogicalDevice device, ReniFont font, int atlasWidth, int atlasHeight) {
        this.device = device;
        this.font = font;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;

        quad = new GPUBuffer(device, BufferUsage.VERTEX, 4 * 4 * 2);
        ByteBuffer buffer = quad.createByteBuf();
        FloatBuffer fb = buffer.asFloatBuffer();
        fb.put(new float[]{
                0, 0,
                0, 1,
                1, 1,
                1, 0,
        });
        MemoryUtil.memFree(buffer);
    }

    public void preload(char c) {
        ReniGlyph glyph = font.glyph(c, FreeType.FT_LOAD_CROP_BITMAP);
        for (AtlasInfo atlas : atlases) {
            if (atlas.addGlyph(glyph)) {
                return;
            }
        }
        Atlas newAt;
        atlases.add(AtlasInfo.create(newAt = new Atlas(device, atlasWidth, atlasHeight)));
        newAt.addGlyph(glyph);
    }
}
