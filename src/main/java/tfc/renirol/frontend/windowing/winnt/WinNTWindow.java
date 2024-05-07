package tfc.renirol.frontend.windowing.winnt;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.windows.*;
import tfc.renirol.ReniContext;
import tfc.renirol.Renirol;
import tfc.renirol.frontend.windowing.GenericWindow;
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

    public WinNTWindow(int width, int height, String name) {
        super(WinNTWindowManager.INSTANCE);
        clz = WNDCLASSEX.calloc();
        clz.lpszClassName(className = MemoryUtil.memUTF16Safe("C_JWRT" + (id++) + ((char) 0), true));
        clz.lpszMenuName(menuName = MemoryUtil.memUTF16Safe("M_JWRT" + (id++) + ((char) 0), true));
        clz.hInstance(hInst);
        clz.lpfnWndProc(new WindowProc() {
            @Override
            public long invoke(long hwnd, int uMsg, long wParam, long lParam) {
                switch (uMsg) {
                    case User32.WM_CLOSE -> shouldClose = true;
                }
                return User32.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
        });
        clz.style(User32.CS_HREDRAW | User32.CS_VREDRAW);
        clz.cbSize(clz.sizeof());
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

    public void swap() {
        context.swapBuffers(this);
    }

    public void pollSize() {
        User32.GetWindowRect(hwnd, rect);
    }

    @Override
    public void swapAndPollSize() {
        context.swapBuffers(this);
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

    public void captureMouse() {
        ReniUser32.SetCapture(hwnd);
    }

    public void freeMouse() {
        ReniUser32.ReleaseCapture();
    }

    @Override
    public ReniContext getContext() {
        return context;
    }
}
