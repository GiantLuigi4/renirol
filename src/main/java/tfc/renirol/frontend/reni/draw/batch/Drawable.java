package tfc.renirol.frontend.reni.draw.batch;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;

//@formatter:off
public abstract class Drawable {
	public abstract void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int start, int end);
	public abstract void draw(CommandBuffer buffer, GraphicsPipeline pipeline);
	public abstract int size();
	public abstract void bind(CommandBuffer buffer);
}
//@formatter:on
