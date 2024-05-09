package tfc.renirol.frontend.reni.draw.instance;

import tfc.renirol.frontend.rendering.command.CommandBuffer;import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;//@formatter:off
public abstract class InstanceKey {
	@Override public abstract int hashCode();
	@Override public abstract boolean equals(Object obj);
	public abstract void bind();
	public abstract void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int count);
}
//@formatter:on
