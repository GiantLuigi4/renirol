package tfc.renirol.frontend.windowing.listener;

public interface KeyboardListener {
    void keyPress(int scan, int code, int mods);
    void keyRelease(int scan, int code, int mods);
    void keyType(int scan, int code, int mods);
}
