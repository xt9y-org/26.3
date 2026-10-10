package dev.xt9y.perf;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeBridge {
    private static volatile boolean ready;
    private static volatile String failure = "Not initialized";

    private NativeBridge() {}

    public static synchronized void initialize(Path configPath) {
        if (ready) return;
        try {
            loadNativeLibrary();
            nativeInitialize(configPath.toAbsolutePath().toString());
            ready = true;
            failure = "";
        } catch (Throwable exception) {
            ready = false;
            failure = exception.toString();
            PerfMod.LOGGER.warn("Native Rust unavailable; vanilla generation unchanged", exception);
        }
    }

    private static void loadNativeLibrary() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String platform = os.contains("mac") ? "macos" :
                os.contains("win") ? "windows" : "linux";
        String arch = architecture.equals("aarch64") || architecture.equals("arm64")
                ? "aarch64" : "x86_64";
        String filename = System.mapLibraryName("xt9y263");
        String resource = "/natives/" + platform + "-" + arch + "/" + filename;
        try (InputStream source = NativeBridge.class.getResourceAsStream(resource)) {
            if (source == null) {
                System.loadLibrary("xt9y263");
                return;
            }
            Path library = Files.createTempFile("xt9y263-", "-" + filename);
            Files.copy(source, library, StandardCopyOption.REPLACE_EXISTING);
            library.toFile().deleteOnExit();
            System.load(library.toAbsolutePath().toString());
        }
    }

    public static boolean ready() { return ready; }
    public static String failure() { return failure; }

    public static void observeServerTick(long nanos) {
        if (ready && nanos >= 0) nativeObserveServerTick(nanos);
    }

    public static void observeClientTick(long nanos) {
        if (ready && nanos >= 0) nativeObserveClientTick(nanos);
    }

    public static String snapshot() {
        return ready ? nativeSnapshot() : "Native unavailable: " + failure;
    }

    public static int quality() { return ready ? nativeQuality() : 100; }

    public static void setQuality(int value) {
        if (ready) nativeSetQuality(Math.clamp(value, 1, 100));
    }

    public static String benchmark(long seed, int chunks, int workers) {
        return ready ? nativeBenchmark(seed, chunks, Math.clamp(workers, 1, 64))
                : "Native library unavailable.";
    }

    private static native void nativeInitialize(String configPath);
    private static native void nativeObserveServerTick(long nanos);
    private static native void nativeObserveClientTick(long nanos);
    private static native String nativeSnapshot();
    private static native int nativeQuality();
    private static native void nativeSetQuality(int value);
    private static native String nativeBenchmark(long seed, int chunks, int workers);
}
