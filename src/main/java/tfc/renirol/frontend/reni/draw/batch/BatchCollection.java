package tfc.renirol.frontend.reni.draw.batch;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.rendering.resource.buffer.DataFormat;
import tfc.renirol.frontend.reni.draw.DrawCollection;
import tfc.renirol.util.set.QuantizedHashSet;

import java.util.ArrayList;
import java.util.function.Consumer;

public class BatchCollection extends DrawCollection {
	ArrayList<BatchList> toDraw;
	ArrayList<Runnable> stateSetup;
	public GraphicsPipeline pipeline;
	BatchList building;
	
	public BatchCollection(Consumer<BatchCollection> initialState) {
		toDraw = new ArrayList<>();
		stateSetup = new ArrayList<>();
		stateSetup.add(() -> initialState.accept(this));
		toDraw.add(building = new BatchList(new QuantizedHashSet<>()));
	}
	
	public BatchCollection(Consumer<BatchCollection> initialState, boolean needsSort) {
		toDraw = new ArrayList<>();
		stateSetup = new ArrayList<>();
		stateSetup.add(() -> initialState.accept(this));
		if (needsSort) toDraw.add(building = new BatchList(new ArrayList<>()));
		else toDraw.add(building = new BatchList(new QuantizedHashSet<>()));
	}
	
	public void reset(Consumer<BatchCollection> initialState, boolean needsSort) {
		toDraw = new ArrayList<>();
		stateSetup = new ArrayList<>();
		stateSetup.add(() -> initialState.accept(this));
		if (needsSort) toDraw.add(building = new BatchList(new ArrayList<>()));
		else toDraw.add(building = new BatchList(new QuantizedHashSet<>()));
	}
	
	public void draw(CommandBuffer buffer) {
		Drawable last = null;
		for (int i = 0; i < stateSetup.size(); i++) {
			stateSetup.get(i).run();
			last = toDraw.get(i).draw(buffer, pipeline, last);
		}
	}
	
	public void add(CommandBuffer buffer, Drawable drawable) {
		building.add(drawable);
	}
	
	public void setupState(Runnable r) {
//		if (building.isEmpty()) stateSetup.set(stateSetup.size() - 1, r);
		stateSetup.add(r);
		toDraw.add(building = new BatchList(new QuantizedHashSet<>()));
	}
	
	public void setupState(Runnable r, boolean needsSort) {
//		if (building.isEmpty()) stateSetup.set(stateSetup.size() - 1, r);
		stateSetup.add(r);
		if (needsSort) toDraw.add(building = new BatchList(new ArrayList<>()));
		else toDraw.add(building = new BatchList(new QuantizedHashSet<>()));
	}
}
