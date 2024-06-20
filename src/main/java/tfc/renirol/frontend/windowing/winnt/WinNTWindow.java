package tfc.renirol.frontend.windowing.winnt;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.*;
import tfc.renirol.ReniContext;
import tfc.renirol.Renirol;
import tfc.renirol.frontend.windowing.GenericWindow;
import tfc.renirol.frontend.windowing.listener.KeyboardListener;
import tfc.renirol.frontend.windowing.listener.MouseListener;
import tfc.renirol.util.windows.ReniUser32;

import java.nio.ByteBuffer;

public class WinNTWindow extends GenericWindow {
    WNDCLASSEX clz;
    ByteBuffer className;
    ByteBuffer windowName;
    ByteBuffer menuName;
    static int id = 0;
    static final long hInst = WinBase.GetModuleHandle((ByteBuffer) null);
    final long hwnd;

    private ReniContext context;
    private final RECT rect = RECT.calloc();
    private boolean shouldClose = false;

    int modsL = 0;
    int modsR = 0;
    int xSum = 0;
    int ySum = 0;

    public WinNTWindow(int width, int height, String name) {
        super(WinNTWindowManager.INSTANCE);
        clz = WNDCLASSEX.calloc();
        clz.lpszClassName(className = MemoryUtil.memUTF16Safe("C_JWRT" + (id++) + ((char) 0), true));
//        clz.lpszMenuName(menuName = MemoryUtil.memUTF16Safe("M_JWRT" + (id++) + ((char) 0), true));
        clz.hInstance(hInst);
        clz.lpfnWndProc(new WindowProc() {
            @Override
            public long invoke(long hwnd, int uMsg, long wParam, long lParam) {
                switch (uMsg) {
                    case User32.WM_SIZE,
                            User32.WM_MOVE,
                            0x0007, // set focus
                            User32.WM_CAPTURECHANGED -> {
                        if (captureMouse) {
                            captureMouse();
                        }
                    }

                    case 0x0008 -> { // kill focus
                        if (captureMouse) {
                            User32.ClipCursor(null);
                        }
                    }

                    case User32.WM_CLOSE -> shouldClose = true;
                    case User32.WM_MOUSEMOVE -> {
                        int x = (int) (lParam & 0xFFFF);
                        int y = (int) ((lParam >> 16) & 0xFFFF);

                        if (captureMouse) {
                            RECT r = getClip();

                            int cCentX = ((r.right() - r.left()) / 2);
                            int cCentY = ((r.bottom() - r.top()) / 2);

                            if (x == cCentX && y == cCentY)
                                return 0;

                            int centerX = (r.right() + r.left()) / 2;
                            int centerY = (r.top() + r.bottom()) / 2;

                            xSum += x - cCentX;
                            ySum += y - cCentY;
                            User32.SetCursorPos(centerX, centerY);

                            if (mouseListener != null) {
                                mouseListener.mouseMove(xSum, ySum);
                                return User32.DefWindowProc(hwnd, uMsg, wParam, lParam);
                            }

                            r.free();
                        } else {
                            if (mouseListener != null) {
                                mouseListener.mouseMove(x, y);
                                return User32.DefWindowProc(hwnd, uMsg, wParam, lParam);
                            }
                        }
                    }
                    case User32.WM_KEYDOWN -> {
                        int scan = (int) ((lParam >> 16) & 0xFF);
                        int code = (int) (wParam);

                        switch (code) {
                            case User32.VK_LSHIFT -> {
                                modsL |= GLFW.GLFW_MOD_SHIFT;
                                code = GLFW.GLFW_KEY_LEFT_SHIFT;
                            }
                            case User32.VK_RSHIFT -> {
                                modsR |= GLFW.GLFW_MOD_SHIFT;
                                code = GLFW.GLFW_KEY_RIGHT_SHIFT;
                            }
                            case User32.VK_LMENU -> {
                                modsL |= GLFW.GLFW_MOD_ALT;
                                code = GLFW.GLFW_KEY_LEFT_ALT;
                            }
                            case User32.VK_RMENU -> {
                                modsR |= GLFW.GLFW_MOD_ALT;
                                code = GLFW.GLFW_KEY_RIGHT_ALT;
                            }
                            case User32.VK_LCONTROL -> {
                                modsL |= GLFW.GLFW_MOD_CONTROL;
                                code = GLFW.GLFW_KEY_LEFT_CONTROL;
                            }
                            case User32.VK_RCONTROL -> {
                                modsR |= GLFW.GLFW_MOD_CONTROL;
                                code = GLFW.GLFW_KEY_RIGHT_CONTROL;
                            }
                            case User32.VK_LWIN -> {
                                modsL |= GLFW.GLFW_MOD_SUPER;
                                code = GLFW.GLFW_KEY_LEFT_SUPER;
                            }
                            case User32.VK_RWIN -> {
                                modsR |= GLFW.GLFW_MOD_SUPER;
                                code = GLFW.GLFW_KEY_RIGHT_SUPER;
                            }
                        }

                        if (keyboardListener != null) {
                            keyboardListener.keyPress(
                                    code, scan, modsL | modsR
                            );
                            return 0;
                        }
                    }
                    case User32.WM_CHAR -> {
                        int scan = 0;
                        char ch = (char) (wParam);
//                        int repeatCount = lParam & 0xFFFF;
                        if (keyboardListener != null) {
                            keyboardListener.keyType(
                                    ch, scan, modsL | modsR
                            );
                            return 0;
                        }
                    }
                    case User32.WM_MOUSEACTIVATE -> {
                        // When you click activate the window, we want Mouse to ignore it.
                        return 0x0021;
                    }

                    // WinNT has this dumb thing where pressing alt pauses the program by default
                    // this prevents that
                    // it's not even like this feature exists to pause the program when tabbed out, as it only happens if you release alt without tabbing out
                    // so I really do not have any idea why this exists
                    case User32.WM_SYSKEYUP -> {
                        return 1;
                    }

                    case User32.WM_KEYUP -> {
                        int scan = (int) ((lParam >> 16) & 0xFF);
                        int code = (int) (wParam);

                        int mv = 0;
                        boolean left = false;
                        switch (code) {
                            case User32.VK_LSHIFT -> {
                                mv = GLFW.GLFW_MOD_SHIFT;
                                left = true;
                                code = GLFW.GLFW_KEY_LEFT_SHIFT;
                            }
                            case User32.VK_RSHIFT -> {
                                mv = GLFW.GLFW_MOD_SHIFT;
                                left = false;
                                code = GLFW.GLFW_KEY_RIGHT_SHIFT;
                            }
                            case User32.VK_LMENU -> {
                                mv = GLFW.GLFW_MOD_ALT;
                                left = true;
                                code = GLFW.GLFW_KEY_LEFT_ALT;
                            }
                            case User32.VK_RMENU -> {
                                mv = GLFW.GLFW_MOD_ALT;
                                left = false;
                                code = GLFW.GLFW_KEY_RIGHT_ALT;
                            }
                            case User32.VK_LCONTROL -> {
                                mv = GLFW.GLFW_MOD_CONTROL;
                                left = true;
                                code = GLFW.GLFW_KEY_LEFT_CONTROL;
                            }
                            case User32.VK_RCONTROL -> {
                                mv = GLFW.GLFW_MOD_CONTROL;
                                left = false;
                                code = GLFW.GLFW_KEY_RIGHT_CONTROL;
                            }
                            case User32.VK_LWIN -> {
                                mv = GLFW.GLFW_MOD_SUPER;
                                left = true;
                                code = GLFW.GLFW_KEY_LEFT_SUPER;
                            }
                            case User32.VK_RWIN -> {
                                mv = GLFW.GLFW_MOD_SUPER;
                                left = false;
                                code = GLFW.GLFW_KEY_RIGHT_SUPER;
                            }
                        }

                        if (left) modsL ^= modsL & mv;
                        else modsR ^= modsR & mv;

                        if (keyboardListener != null) {
                            keyboardListener.keyRelease(
                                    code, scan, modsL | modsR
                            );
                            return 0;
                        }
                    }
                }
                return User32.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
        });
        clz.style(User32.CS_HREDRAW | User32.CS_VREDRAW);
        clz.cbSize(clz.sizeof());
        clz.hbrBackground(0x0005);
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        buffer.asIntBuffer().put(User32.IDC_ARROW);
        clz.hCursor(User32.LoadCursor(0, buffer));
        if (User32.RegisterClassEx(clz) == 0) {
            throw new RuntimeException("Failed to register window class " + WinBase.getLastError());
        }
        MemoryUtil.memFree(buffer);
        hwnd = User32.CreateWindowEx(
                0,
                className,
                windowName = MemoryUtil.memUTF16Safe(name + ((char) 0), true),
                User32.WS_OVERLAPPEDWINDOW,

                0, 0,
                width, height,
                0, 0,
                hInst, 0
        );
        if (hwnd == 0) {
            throw new RuntimeException("Failed to create window " + WinBase.getLastError());
        }
        User32.GetWindowRect(hwnd, rect);
    }

    public void show() {
        User32.ShowWindow(hwnd, User32.SW_SHOW);
    }

    public void hide() {
        User32.ShowWindow(hwnd, User32.SW_HIDE);
    }

    public void dispose() {
        User32.DestroyWindow(hwnd);
        User32.UnregisterClass(className, hInst);
    }

    @Override
    public void initContext(ReniContext graphicsContext) {
        if (this.context != null)
            throw new RuntimeException("Cannot setup multiple contexts to display to one window.");
        (this.context = graphicsContext).setupSurface(this);
    }

    @Override
    public int getWidth() {
        return rect.right() - rect.left();
    }

    @Override
    public int getHeight() {
        return rect.bottom() - rect.top();
    }

    @Override
    public void grabContext() {
        Renirol.useContext(context, this);
    }

    @Override
    public boolean shouldClose() {
        return shouldClose;
    }

    public boolean swap() {
        return context.swapBuffers(this);
    }

    public void pollSize() {
        User32.GetWindowRect(hwnd, rect);
    }

    @Override
    public boolean swapAndPollSize() {
        User32.GetWindowRect(hwnd, rect);

        MSG msg = MSG.calloc();
        while (User32.PeekMessage(msg, hwnd, 0, 0, User32.PM_REMOVE)) {
            User32.TranslateMessage(msg);
            User32.DispatchMessage(msg);
            if (msg.message() == 15) {
                break;
            }
        }
        msg.free();

        return context.swapBuffers(this);
    }

    @Override
    public long handle() {
        return hwnd;
    }

    @Override
    public void setName(String name) {
        MemoryUtil.memFree(windowName);
        windowName = MemoryUtil.memUTF16Safe(name + ((char) 0), true);
        User32.SetWindowText(hwnd, windowName);
    }

    boolean captureMouse = false;

    public void captureMouse() {
        RECT clipRect = getClip();

        User32.ClipCursor(clipRect);
        captureMouse = true;
        clipRect.free();
    }

    private RECT getClip() {
        RECT clipRect = RECT.calloc();
        ReniUser32.GetClientRect(hwnd, clipRect);

        POINT point = POINT.calloc();
        point.set(clipRect.left(), clipRect.bottom());
        User32.ClientToScreen(hwnd, point);
        clipRect.left(point.x());
        clipRect.bottom(point.y());
        point.set(clipRect.right(), clipRect.top());
        User32.ClientToScreen(hwnd, point);
        clipRect.right(point.x());
        clipRect.top(point.y());
        point.free();

        return clipRect;
    }

    @Override
    public void freeMouse() {
        captureMouse = false;
        User32.ClipCursor(null);
    }

    @Override
    public ReniContext getContext() {
        return context;
    }

    KeyboardListener keyboardListener = null;
    MouseListener mouseListener = null;

    @Override
    public void addKeyboardListener(KeyboardListener listener) {
        this.keyboardListener = listener;
    }

    @Override
    public void addMouseListener(MouseListener listener) {
        this.mouseListener = listener;
    }
}
