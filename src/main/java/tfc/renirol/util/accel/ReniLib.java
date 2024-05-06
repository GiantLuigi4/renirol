package tfc.renirol.util.accel;

import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReniLib {
    public static boolean nativesSupported;

    static {
        boolean supported = false;

        try {
            String file = "renirol_natives_" + switch (Platform.getArchitecture()) {
                case X64 -> "x86_64";
                case X86 -> "x86";
                case ARM32 -> "arm";
                case ARM64 -> "arm64";
                default -> throw new RuntimeException("NYI");
            };

            // TODO: extract native automatically
            InputStream is = null;
            File fl = null;
            switch (Platform.get()) {
                case WINDOWS -> {
                    fl = new File(file + ".dll");
                    if (!fl.exists()) {
                        is = ReniLib.class.getClassLoader().getResourceAsStream(
                                "lib/windows/" + file + ".dll"
                        );
                        Files.copy(is, fl.toPath());
                    }
                }
                case LINUX -> {
                    fl = new File("lib" + file + ".so");
                    if (!fl.exists()) {
                        is = ReniLib.class.getClassLoader().getResourceAsStream(
                                "lib/unix/lib" + file + ".so"
                        );
                        Files.copy(is, fl.toPath());
                    }
                }
                case MACOSX -> {
                    fl = new File(file + ".dynlib");
                    if (!fl.exists()) {
                        is = ReniLib.class.getClassLoader().getResourceAsStream(
                                "lib/mac/" + file + ".dynlib"
                        );
                        Files.copy(is, fl.toPath());
                    }
                }
                default -> throw new RuntimeException("NYI");
            }
            System.loadLibrary(file);

            try {
                is.close();
            } catch (Throwable ignored) {
            }
            supported = true;
        } catch (Throwable ignored) {
        }
        nativesSupported = supported;
        if (!nativesSupported) {
            System.err.println(
                    "[RENIROL] Natives unsupported on this platform, some things may run slower.\n" +
                            Platform.get() + " " + Platform.getArchitecture()
            );
        }
    }

    public static void initialize() {
    }
}
