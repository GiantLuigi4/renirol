package tfc.renirol.frontend.reni.draw.instance;

import tfc.renirol.frontend.rendering.command.CommandBuffer;//@formatter:off
public interface Instanceable {
	InstanceKey comparator();
	// TODO: I get the impression that this method is going to make me want to have a stroke in the future
	// 		 figure out something to do about it
	void setup(CommandBuffer buffer, int id);
}
//@formatter:on
