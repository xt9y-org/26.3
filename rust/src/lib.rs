//! JNI bridge: lock-free tick sampling, user config, isolated Rust chunk benchmarks.

mod worldgen;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use std::fs;
use std::path::PathBuf;
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering::Relaxed};
use std::sync::{Mutex, OnceLock};

static SERVER_LAST: AtomicU64 = AtomicU64::new(0);
static SERVER_TOTAL: AtomicU64 = AtomicU64::new(0);
static SERVER_COUNT: AtomicU64 = AtomicU64::new(0);
static CLIENT_LAST: AtomicU64 = AtomicU64::new(0);
static CLIENT_TOTAL: AtomicU64 = AtomicU64::new(0);
static CLIENT_COUNT: AtomicU64 = AtomicU64::new(0);
static QUALITY: AtomicU32 = AtomicU32::new(100);
static CONFIG_PATH: OnceLock<Mutex<PathBuf>> = OnceLock::new();

fn record(last: &AtomicU64, total: &AtomicU64, count: &AtomicU64, nanos: jlong) {
    if (0..=5_000_000_000).contains(&nanos) {
        last.store(nanos as u64, Relaxed);
        total.fetch_add(nanos as u64, Relaxed);
        count.fetch_add(1, Relaxed);
    }
}

fn config_path() -> &'static Mutex<PathBuf> {
    CONFIG_PATH.get_or_init(|| Mutex::new(PathBuf::new()))
}

fn save_quality(value: u32) {
    let Ok(path) = config_path().lock() else {
        return;
    };
    if path.as_os_str().is_empty() {
        return;
    }
    if let Some(parent) = path.parent() {
        let _ = fs::create_dir_all(parent);
    }
    let _ = fs::write(
        &*path,
        format!(
            "# Rust benchmark setting. Minecraft worldgen is unaffected.\nquality={value}\n"
        ),
    );
}

fn snapshot() -> String {
    let ticks = SERVER_COUNT.load(Relaxed);
    let clients = CLIENT_COUNT.load(Relaxed);
    let server_mean = if ticks == 0 {
        0.0
    } else {
        SERVER_TOTAL.load(Relaxed) as f64 / ticks as f64 / 1e6
    };
    let client_mean = if clients == 0 {
        0.0
    } else {
        CLIENT_TOTAL.load(Relaxed) as f64 / clients as f64 / 1e6
    };
    format!(
        "Rust backend active\nServer tick: {:.2} ms last / {:.2} ms mean ({} samples)\nClient tick: {:.2} ms last / {:.2} ms mean ({} samples)\nExperimental sampling quality: {}%\nLive worldgen: Vanilla, unchanged",
        SERVER_LAST.load(Relaxed) as f64 / 1e6,
        server_mean,
        ticks,
        CLIENT_LAST.load(Relaxed) as f64 / 1e6,
        client_mean,
        clients,
        QUALITY.load(Relaxed)
    )
}

fn jstring(env: &mut JNIEnv, value: &str) -> jstring {
    env.new_string(value)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeInitialize(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) {
    if let Ok(input) = env.get_string(&path) {
        let path = PathBuf::from(input.to_string_lossy().into_owned());
        if let Ok(contents) = fs::read_to_string(&path) {
            for line in contents.lines() {
                let parsed = line.trim().strip_prefix("quality=")
                    .and_then(|value| value.trim().parse::<u32>().ok());
                if let Some(q) = parsed {
                    QUALITY.store(q.clamp(1, 100), Relaxed);
                }
            }
        }
        if let Ok(mut out) = config_path().lock() {
            *out = path;
        }
        save_quality(QUALITY.load(Relaxed));
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeObserveServerTick(
    _env: JNIEnv,
    _class: JClass,
    nanos: jlong,
) {
    record(&SERVER_LAST, &SERVER_TOTAL, &SERVER_COUNT, nanos);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeObserveClientTick(
    _env: JNIEnv,
    _class: JClass,
    nanos: jlong,
) {
    record(&CLIENT_LAST, &CLIENT_TOTAL, &CLIENT_COUNT, nanos);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeSnapshot(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    jstring(&mut env, &snapshot())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeQuality(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    QUALITY.load(Relaxed) as jint
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeSetQuality(
    _env: JNIEnv,
    _class: JClass,
    quality: jint,
) {
    let quality = quality.clamp(1, 100) as u32;
    QUALITY.store(quality, Relaxed);
    save_quality(quality);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeBenchmark(
    mut env: JNIEnv,
    _class: JClass,
    seed: jlong,
    chunks: jint,
    workers: jint,
) -> jstring {
    // Invoked by a Java virtual thread, never by the render or tick thread.
    let output = match worldgen::benchmark(
        seed as u64,
        chunks.clamp(1, 4096) as usize,
        QUALITY.load(Relaxed) as u8,
        workers.clamp(1, 64) as usize,
    ) {
        Ok(b) => format!(
            "{} chunks | {}% quality | {} workers | {:.2} ms | {:.0} chunks/s | checksum {:016x}",
            b.chunks,
            b.quality,
            b.workers,
            b.elapsed.as_secs_f64() * 1000.0,
            b.chunks as f64 / b.elapsed.as_secs_f64().max(1e-9),
            b.checksum
        ),
        Err(error) => format!("Benchmark error: {error}"),
    };
    jstring(&mut env, &output)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn metrics_reject_invalid_samples() {
        let last = AtomicU64::new(0);
        let total = AtomicU64::new(0);
        let count = AtomicU64::new(0);
        record(&last, &total, &count, 1234);
        record(&last, &total, &count, -1);
        assert_eq!(last.load(Relaxed), 1234);
        assert_eq!(total.load(Relaxed), 1234);
        assert_eq!(count.load(Relaxed), 1);
    }
}
