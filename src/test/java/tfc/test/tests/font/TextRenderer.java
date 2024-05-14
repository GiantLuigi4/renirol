package tfc.test.tests.font;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FreeType;
import tfc.renirol.frontend.enums.BindPoint;
import tfc.renirol.frontend.enums.BufferUsage;
import tfc.renirol.frontend.enums.DescriptorType;
import tfc.renirol.frontend.enums.IndexSize;
import tfc.renirol.frontend.enums.flags.AdvanceRate;
import tfc.renirol.frontend.enums.format.AttributeFormat;
import tfc.renirol.frontend.enums.modes.image.FilterMode;
import tfc.renirol.frontend.enums.modes.image.MipmapMode;
import tfc.renirol.frontend.enums.modes.image.WrapMode;
import tfc.renirol.frontend.hardware.device.ReniLogicalDevice;
import tfc.renirol.frontend.hardware.util.ReniDestructable;
import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.command.pipeline.PipelineState;
import tfc.renirol.frontend.rendering.pass.RenderPass;
import tfc.renirol.frontend.rendering.resource.buffer.BufferDescriptor;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.rendering.resource.buffer.GPUBuffer;
import tfc.renirol.frontend.rendering.resource.descriptor.DescriptorSet;
import tfc.renirol.frontend.rendering.resource.descriptor.ImageInfo;
import tfc.renirol.frontend.rendering.resource.image.texture.TextureSampler;
import tfc.renirol.frontend.reni.font.ReniFont;
import tfc.renirol.frontend.reni.font.ReniGlyph;
import tfc.test.shared.VertexElements;
import tfc.test.shared.VertexFormats;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
    GPUBuffer draw;
    GPUBuffer qIndicies;

    public void draw(
            Runnable startPass,
            String text,
            CommandBuffer cmd,
            GraphicsPipeline pipeline0,
            DescriptorSet set
    ) {
        cmd.bindDescriptor(BindPoint.GRAPHICS, pipeline0, set);
        cmd.bindVbo(0, quad);
        cmd.bindVbo(1, draw);
        cmd.bindIbo(IndexSize.INDEX_16, qIndicies);

        HashMap<AtlasInfo, ByteBuffer> buffer = new HashMap<>();
        float x = 0;
        float y = 0;

        //@formatter:off
        //noinspection PointlessArithmeticExpression
        final int MEMORY_FOOTPRINT =
                2 * 4 +
                4 * 4 +
                4 * 4 +
                // formatting reasons
                // compiler precomputes this anyway iirc
                0
                ;
        //@formatter:on


        float[] scaleX = new float[1];
        float[] scale = new float[1];
        GLFW.glfwGetMonitorContentScale(GLFW.glfwGetPrimaryMonitor(), scaleX, scale);
        float bottom = ((font.height() * 45) / font.unitsPerEM()) * scale[0];
        for (char c : text.toCharArray()) {
            AtlasInfo inf = getAtlas(c);
            ByteBuffer buf = buffer.computeIfAbsent(inf, (k) -> draw.createByteBuf());
            FloatBuffer fb = buf.asFloatBuffer();
            // vec2 offset
            fb.put(x);
            fb.put(y);

            ReniGlyph glyph = font.glyph(c, FreeType.FT_LOAD_CROP_BITMAP);
            // vec4 drawBounds
            // TODO: can pre-sum left&top into offset
            fb.put(glyph.left);
            fb.put(bottom - glyph.height);
            fb.put(glyph.left + glyph.width);
            fb.put(bottom);

            float[] uvBounds = inf.atlas.getBounds(c);
            // vec4 uvBounds
            fb.put(uvBounds[2]);
            fb.put(uvBounds[3]);
            fb.put(uvBounds[0]);
            fb.put(uvBounds[1]);

            buf.position(buf.position() + fb.position() * 4);

            x += glyph.advanceX;
        }

        buffer.forEach((k, v) -> {
            v.flip();
            int count = v.limit() / MEMORY_FOOTPRINT;
            cmd.endPass();

            set.bind(0, 0, DescriptorType.SAMPLED_IMAGE, k.info);
            draw.upload(0, v.limit(), v);

            startPass.run();
            cmd.bindVbo(1, draw);
            cmd.drawIndexed(
                    0,
                    0, count,
                    0, 6
            );
        });

        for (ByteBuffer value : buffer.values()) {
            MemoryUtil.memFree(value);
        }
    }

    HashMap<Character, AtlasInfo> infs = new HashMap<>();

    protected AtlasInfo getAtlas(char c) {
        AtlasInfo inf = infs.get(c);
        if (inf == null) inf = preload(c);
        return inf;
    }

    static final DataFormat formatLeft = VertexFormats.POS2;
    static final DataFormat formatRight = VertexFormats.POS2_POS4_COLOR4;
    static final BufferDescriptor descLeft = new BufferDescriptor(formatLeft);
    static final BufferDescriptor descRight = new BufferDescriptor(formatRight);

    static {
        descLeft.describe(0);
        descLeft.attribute(0, 0, AttributeFormat.RG32_FLOAT, formatLeft.offset(VertexElements.POSITION_XY));

        descRight.advance(AdvanceRate.PER_INSTANCE);
        descRight.describe(1);
        descRight.attribute(1, 1, AttributeFormat.RG32_FLOAT, formatRight.offset(VertexElements.POSITION_XY));
        descRight.attribute(1, 2, AttributeFormat.RGBA32_FLOAT, formatRight.offset(VertexElements.POSITION_XYZW));
        descRight.attribute(1, 3, AttributeFormat.RGBA32_FLOAT, formatRight.offset(VertexElements.COLOR_RGBA));
    }

    public void bind(PipelineState state) {
        state.vertexInput(descLeft, descRight);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AtlasInfo atlasInfo = (AtlasInfo) o;
            return Objects.equals(atlas, atlasInfo.atlas) && Objects.equals(sampler, atlasInfo.sampler) && Objects.equals(info, atlasInfo.info);
        }

        @Override
        public int hashCode() {
            return Objects.hash(atlas, sampler, info);
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
        quad.allocate();
        ByteBuffer buffer = quad.createByteBuf();
        FloatBuffer fb = buffer.asFloatBuffer();
        fb.put(new float[]{
                1, 0,
                1, 1,
                0, 1,
                0, 0,
        });
        quad.upload(0, buffer);
        MemoryUtil.memFree(buffer);

        qIndicies = new GPUBuffer(device, BufferUsage.INDEX, 6 * 2);
        qIndicies.allocate();
        buffer = qIndicies.createByteBuf();
        ShortBuffer sb = buffer.asShortBuffer();
        sb.put(0, (short) 0);
        sb.put(1, (short) 1);
        sb.put(2, (short) 2);
        sb.put(3, (short) 2);
        sb.put(4, (short) 3);
        sb.put(5, (short) 0);
        qIndicies.upload(0, buffer);
        MemoryUtil.memFree(buffer);

        draw = new GPUBuffer(
                device,
                BufferUsage.VERTEX,
                4086 * 4086
        );
        draw.allocate();
    }

    public AtlasInfo preload(char c) {
        ReniGlyph glyph = font.glyph(c, FreeType.FT_LOAD_CROP_BITMAP);
        for (AtlasInfo atlas : atlases) {
            if (atlas.addGlyph(glyph)) {
                infs.put(c, atlas);
                return atlas;
            }
        }
        Atlas newAt;
        AtlasInfo inf;
        atlases.add(inf = AtlasInfo.create(newAt = new Atlas(device, atlasWidth, atlasHeight)));
        newAt.addGlyph(glyph);
        infs.put(c, inf);
        return inf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextRenderer renderer = (TextRenderer) o;
        return atlasWidth == renderer.atlasWidth && atlasHeight == renderer.atlasHeight && Objects.equals(quad, renderer.quad) && Objects.equals(draw, renderer.draw) && Objects.equals(qIndicies, renderer.qIndicies) && Objects.equals(device, renderer.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quad, draw, qIndicies, atlasWidth, atlasHeight, device);
    }


}
