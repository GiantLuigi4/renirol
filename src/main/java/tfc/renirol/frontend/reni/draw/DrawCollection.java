package tfc.renirol.frontend.reni.draw;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.reni.draw.batch.Drawable;

public abstract class DrawCollection extends Drawable {
	public GraphicsPipeline pipeline;

	//@formatter:off
	public abstract void draw(CommandBuffer buffer);
	public abstract void setupState(Runnable r);
	public abstract void add(CommandBuffer buffer, Drawable drawable);
	//@formatter:on
	
	@Override
	public final void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int start, int end) {
		throw new RuntimeException("Cannot draw a draw collection with a start nor end index");
	}
	
	@Override
	public final void draw(CommandBuffer buffer, GraphicsPipeline pipeline) {
		draw(buffer);
	}
	
	@Override
	public final int size() {
		return 0;
	}
	
	@Override
	public final void bind(CommandBuffer buffer) {
		// no-op
	}
}
