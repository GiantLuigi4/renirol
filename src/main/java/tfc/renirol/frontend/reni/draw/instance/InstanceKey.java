package tfc.renirol.frontend.reni.draw.instance;

import tfc.renirol.frontend.rendering.command.CommandBuffer;import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;//@formatter:off
public interface InstanceKey {
	int hashCode();
	boolean equals(Object obj);
	void bind(CommandBuffer buffer);
	void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int count);
	// upload data from here
	void prepareCall(CommandBuffer buffer);
}
//@formatter:on
