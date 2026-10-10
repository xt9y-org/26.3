package dev.xt9y.perf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeBridge {
    private static volatile boolean ready;
    private static boolean attempted;
    private static String error = "Native library not loaded";

    private NativeBridge() {}

    /** Initialization happens once when the profiler is first opened. */
    public static synchronized void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
            String platform = os.contains("mac") ? "macos" : os.contains("win") ? "windows" : "linux";
            String cpu = arch.equals("aarch64") || arch.equals("arm64") ? "aarch64" : "x86_64";
            String filename = System.mapLibraryName("xt9y263");
            String path = "/natives/" + platform + "-" + cpu + "/" + filename;
            try (var input = NativeBridge.class.getResourceAsStream(path)) {
                if (input == null) {
                    System.loadLibrary("xt9y263");
                } else {
                    Path temp = Files.createTempFile("xt9y263-", "-" + filename);
                    try {
                        Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
                        System.load(temp.toAbsolutePath().toString());
                    } finally {
                        temp.toFile().deleteOnExit();
                    }
                }
            }
            ready = true;
            error = "";
        } catch (Throwable exception) {
            error = exception.toString();
            PerfMod.LOGGER.warn("Rust performance profiler unavailable", exception);
        }
    }

    public static boolean ready() { return ready; }
    public static String error() { return error; }
    public static void record(int kind, long nanos) {
        if (ready) nativeRecord(kind, nanos);
    }
    public static void serverStart(long nanos) {
        if (ready) nativeServerStart(nanos);
    }
    public static void chunkEvent(int kind) {
        if (ready) nativeChunkEvent(kind);
    }
    public static float[] metrics() {
        return ready ? nativeMetrics() : new float[15];
    }

    private static native void nativeRecord(int kind, long nanos);
    private static native void nativeServerStart(long nanos);
    private static native void nativeChunkEvent(int kind);
    private static native float[] nativeMetrics();
}
