package tfc.renirol.frontend.reni.draw.batch;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;

//@formatter:off
public interface Drawable {
	void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int start, int end);
	void draw(CommandBuffer buffer, GraphicsPipeline pipeline);
	int size();
	void bind(CommandBuffer buffer);
}
//@formatter:on
