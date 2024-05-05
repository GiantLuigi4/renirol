package tfc.renirol.frontend.rendering.resource.buffer;

import tfc.renirol.util.CyclicStack;

public class DataFormat {
    public final CyclicStack<DataElement> elements;
    public final int stride;

    public DataFormat(DataElement... elements) {
        this.elements = new CyclicStack<>(elements[0], true);
        CyclicStack<DataElement> current = this.elements;
        int stride = elements[0].bytes();

        for (int i = 1; i < elements.length; i++) {
            CyclicStack<DataElement> build = new CyclicStack<>(elements[i], false);
            current.link(build);
            current = build;
            stride += elements[i].bytes();
        }
        current.link(this.elements);

        this.stride = stride;
    }

    public int offset(DataElement target) {
        int stride = 0;
        for (DataElement element : elements) {
            if (element == target) {
                return stride;
            }
            stride += element.bytes();
        }
        throw new RuntimeException("Element not found.");
    }
}
