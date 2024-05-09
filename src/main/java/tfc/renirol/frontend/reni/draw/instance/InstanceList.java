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
			key.bind();
			ArrayList<Instanceable> instanceables = instanceKeyArrayListEntry.getValue();
			int actualI = 0;
			int i;
			for (i = 0; actualI < instanceables.size(); i++) {
				if (i >= maxPerCall) {
					i = 0;
					key.draw(buffer, pipeline, maxPerCall);
				}
				
				instanceables.get(actualI++).setup(buffer, i);
			}
			key.draw(buffer, pipeline, i);
		}
	}
	
	public void add(Instanceable instanceable) {
		ArrayList<Instanceable> instanceList = instances.computeIfAbsent(instanceable.comparator(), k -> new ArrayList<>());
		instanceList.add(instanceable);
	}
}
