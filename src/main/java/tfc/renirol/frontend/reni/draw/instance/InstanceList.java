package tfc.renirol.frontend.reni.draw.instance;

import tfc.renirol.frontend.rendering.command.CommandBuffer;
import tfc.renirol.frontend.rendering.command.pipeline.GraphicsPipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InstanceList {
    HashMap<InstanceKey, ArrayList<Instanceable>> instances = new HashMap<>();

    public void draw(CommandBuffer buffer, GraphicsPipeline pipeline, int maxPerCall) {
        for (Map.Entry<InstanceKey, ArrayList<Instanceable>> instanceKeyArrayListEntry : instances.entrySet()) {
            InstanceKey key = instanceKeyArrayListEntry.getKey();
            key.bind(buffer);
            ArrayList<Instanceable> instanceables = instanceKeyArrayListEntry.getValue();
            int actualI = 0;
            while (instanceables.size() - actualI != 0) {
                int i = 0;
                int v = Math.min(instanceables.size() - actualI, maxPerCall);
                for (; i < v; i++) {
                    if (instanceables.get(actualI).visible())
                        instanceables.get(actualI++).setup(buffer, i);
                    else {
                        actualI++;
                        i--;
                    }
                }

                key.prepareCall(buffer);
                key.draw(buffer, pipeline, i);
            }
        }
    }

    public void add(Instanceable instanceable) {
        ArrayList<Instanceable> instanceList = instances.computeIfAbsent(instanceable.comparator(), k -> new ArrayList<>());
        instanceList.add(instanceable);
    }
}
