package tfc.renirol.frontend.rendering.selectors;

public class ChannelInfo {
    char name;
    int[] bitDepths;

    public ChannelInfo(char name, int... bitDepths) {
        this.name = Character.toUpperCase(name);
        this.bitDepths = bitDepths;
    }
}
