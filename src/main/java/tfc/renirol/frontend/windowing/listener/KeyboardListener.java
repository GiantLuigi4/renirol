package tfc.renirol.frontend.windowing.listener;

public interface KeyboardListener {
    void keyPress(int key, int scan, int mods);
    void keyRelease(int key, int scan, int mods);
    void keyType(int key, int scan, int mods);
}
