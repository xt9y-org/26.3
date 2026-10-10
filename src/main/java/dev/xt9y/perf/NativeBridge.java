package dev.xt9y.perf;

import java.nio.file.Path;

/** Loader-neutral JNI bridge. Rust owns sample histories and chunk counters. */
public final class NativeBridge {
    private static volatile boolean ready;
    private static volatile String error = "Native Rust library unavailable";
    private NativeBridge() {}

    public static synchronized void initialize(Path configPath) {
        if (ready) return;
        try {
            load();
            nativeInitialize(configPath.toAbsolutePath().toString());
            ready = true;
            error = "";
        } catch (Throwable exception) {
            error = exception.toString();
            PerfMod.LOGGER.warn("Rust backend unavailable; no world changes applied", exception);
        }
    }

    private static void load() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(java.util.Locale.ROOT);
        String platform = os.contains("mac") ? "macos" : os.contains("win") ? "windows" : "linux";
        String cpu = arch.equals("arm64") || arch.equals("aarch64") ? "aarch64" : "x86_64";
        String filename = System.mapLibraryName("xt9y263");
        String resource = "/natives/" + platform + "-" + cpu + "/" + filename;
        try (var source = NativeBridge.class.getResourceAsStream(resource)) {
            if (source == null) {
                System.loadLibrary("xt9y263");
                return;
            }
            Path library = java.nio.file.Files.createTempFile("xt9y263-", "-" + filename);
            java.nio.file.Files.copy(source, library, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            library.toFile().deleteOnExit();
            System.load(library.toAbsolutePath().toString());
        }
    }
    public static boolean ready() { return ready; }
    public static String error() { return error; }
    public static void record(int kind, long nanos) { if (ready) nativeRecord(kind, nanos); }
    public static void serverStart(long nanos) { if (ready) nativeServerStart(nanos); }
    public static void chunkEvent(int kind) { if (ready) nativeChunkEvent(kind); }
    public static float[] metrics() { return ready ? nativeMetrics() : new float[13]; }
    public static float[] history(int kind) { return ready ? nativeHistory(kind) : new float[0]; }
    public static String queueRegeneration(Path world, int x, int z, int radius) {
        return ready ? nativeQueueRegeneration(world.toString(), x, z, radius)
                : "Rust unavailable; regeneration was not queued.";
    }
    public static String regenerationStatus() {
        return ready ? nativeRegenerationStatus() : error;
    }

    private static native void nativeInitialize(String path);
    private static native void nativeRecord(int kind, long nanos);
    private static native void nativeServerStart(long nanos);
    private static native void nativeChunkEvent(int kind);
    private static native float[] nativeMetrics();
    private static native float[] nativeHistory(int kind);
    private static native String nativeQueueRegeneration(String path, int x, int z, int radius);
    private static native String nativeRegenerationStatus();
}
