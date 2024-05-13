package tfc.renirol.frontend.reni.draw.instance;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;
import tfc.renirol.frontend.reni.draw.DrawCollection;
import tfc.renirol.frontend.reni.draw.batch.Drawable;

import java.util.ArrayList;
import java.util.function.Consumer;

public class InstanceCollection extends DrawCollection {
    ArrayList<InstanceList> toDraw;
    ArrayList<Runnable> stateSetup;
    public GraphicsPipeline pipeline;
    public int maxInstances = 100;
    InstanceList building;

    public InstanceCollection(Consumer<InstanceCollection> initialState) {
        toDraw = new ArrayList<>();
        stateSetup = new ArrayList<>();
        stateSetup.add(() -> initialState.accept(this));
        toDraw.add(building = new InstanceList());
    }

    public void reset(Consumer<InstanceCollection> initialState) {
        toDraw = new ArrayList<>();
        stateSetup = new ArrayList<>();
        stateSetup.add(() -> initialState.accept(this));
        toDraw.add(building = new InstanceList());
    }

    public void draw(CommandBuffer buffer) {
        for (int i = 0; i < stateSetup.size(); i++) {
            stateSetup.get(i).run();
            toDraw.get(i).draw(buffer, pipeline, maxInstances /* TODO */);
        }
    }

    public void add(CommandBuffer buffer, Drawable drawable) {
        if (drawable instanceof Instanceable instancable) {
            building.add(instancable);
        } else {
//			throw new RuntimeException("Batched objects cannot be instanced");
            setupState(() -> {
                drawable.bind(buffer);
                drawable.draw(buffer, pipeline);
            });
        }
    }

    public void setupState(Runnable r) {
        stateSetup.add(r);
        toDraw.add(building = new InstanceList());
    }
}
