package tfc.renirol.frontend.reni.draw.batch;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;

import java.util.Collection;

public class BatchList {
	Collection<Drawable> drawables;
	
	public BatchList(Collection<Drawable> drawables) {
		this.drawables = drawables;
	}
	
	public void add(Drawable d) {
		drawables.add(d);
	}
	
	public Drawable draw(CommandBuffer buffer, GraphicsPipeline pipeline, Drawable last) {
		for (Drawable drawable : drawables) {
			if (last != drawable) {
				drawable.bind(buffer);
				last = drawable;
			}
			drawable.draw(buffer, pipeline);
		}
		return last;
	}
	
	public boolean isEmpty() {
		return drawables.isEmpty();
	}
}
