//! JNI adapter. Real frame / server tick / actual Fabric chunk lifecycle counters
//! are processed in native Rust. No synthetic world-generation benchmarks.

mod regen;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jfloatArray, jint, jlong, jstring};
use std::array::from_fn;
use std::path::{Path, PathBuf};
use std::sync::LazyLock;
use std::sync::Mutex;
use std::sync::atomic::{AtomicU64, AtomicUsize, Ordering::Relaxed};

const HISTORY: usize = 240;
const FRAME: usize = 0;
const RENDER: usize = 1;
const CLIENT: usize = 2;
const SERVER: usize = 3;
const SERVER_INTERVAL: usize = 4;
const KINDS: usize = 5;

struct Series {
    data: [AtomicU64; HISTORY],
    count: AtomicUsize,
}
impl Series {
    fn new() -> Self {
        Self {
            data: from_fn(|_| AtomicU64::new(0)),
            count: AtomicUsize::new(0),
        }
    }
    fn push(&self, nanos: jlong) {
        if !(1..=10_000_000_000).contains(&nanos) {
            return;
        }
        let index = self.count.fetch_add(1, Relaxed) % HISTORY;
        self.data[index].store(nanos as u64, Relaxed);
    }
    fn samples(&self) -> Vec<f32> {
        let count = self.count.load(Relaxed);
        let len = count.min(HISTORY);
        let start = count.saturating_sub(len);
        (start..count)
            .map(|i| self.data[i % HISTORY].load(Relaxed) as f32 / 1e6)
            .collect()
    }
    fn mean(&self, limit: usize) -> f32 {
        let values = self.samples();
        let tail = &values[values.len().saturating_sub(limit)..];
        if tail.is_empty() {
            0.0
        } else {
            tail.iter().sum::<f32>() / tail.len() as f32
        }
    }
}
struct Telemetry {
    series: [Series; KINDS],
    chunks: [AtomicU64; 5],
    server_last_start: AtomicU64,
}
static METRICS: LazyLock<Telemetry> = LazyLock::new(|| Telemetry {
    series: from_fn(|_| Series::new()),
    chunks: from_fn(|_| AtomicU64::new(0)),
    server_last_start: AtomicU64::new(0),
});
static CONFIG: LazyLock<Mutex<PathBuf>> = LazyLock::new(|| Mutex::new(PathBuf::new()));
static LAST_REGEN: LazyLock<Mutex<String>> = LazyLock::new(|| Mutex::new(String::new()));

fn jstring(env: &mut JNIEnv, value: &str) -> jstring {
    env.new_string(value)
        .map(|v| v.into_raw())
        .unwrap_or(std::ptr::null_mut())
}
fn config_directory() -> PathBuf {
    CONFIG.lock().map(|p| p.clone()).unwrap_or_default()
}
fn save_message(message: String) {
    if let Ok(mut dest) = LAST_REGEN.lock() {
        *dest = message;
    }
}
fn read_message() -> String {
    LAST_REGEN.lock().map(|s| s.clone()).unwrap_or_default()
}
fn averages() -> [f32; 13] {
    let a = &METRICS.series;
    let frame = a[FRAME].mean(120);
    let server_interval = a[SERVER_INTERVAL].mean(20);
    [
        if frame > 0.0 { 1000.0 / frame } else { 0.0 },
        frame,
        a[RENDER].mean(120),
        a[CLIENT].mean(60),
        a[SERVER].mean(40),
        if server_interval > 0.0 {
            (1000.0 / server_interval).min(20.0)
        } else {
            0.0
        },
        METRICS.chunks[0].load(Relaxed) as f32,
        METRICS.chunks[1].load(Relaxed) as f32,
        METRICS.chunks[2].load(Relaxed) as f32,
        METRICS.chunks[3].load(Relaxed) as f32,
        METRICS.chunks[4].load(Relaxed) as f32,
        a[FRAME].samples().last().copied().unwrap_or(0.0),
        a[SERVER].samples().last().copied().unwrap_or(0.0),
    ]
}
fn floats(env: &mut JNIEnv, data: &[f32]) -> jfloatArray {
    let Ok(array) = env.new_float_array(data.len() as i32) else {
        return std::ptr::null_mut();
    };
    if env.set_float_array_region(&array, 0, data).is_err() {
        return std::ptr::null_mut();
    }
    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeInitialize(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) {
    if let Ok(value) = env.get_string(&path) {
        let file = PathBuf::from(value.to_string_lossy().to_string());
        let directory = file.parent().unwrap_or(Path::new(".")).to_path_buf();
        if let Ok(mut location) = CONFIG.lock() {
            *location = directory.clone();
        }
        save_message(regen::apply_pending(&directory));
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeRecord(
    _env: JNIEnv,
    _class: JClass,
    kind: jint,
    nanos: jlong,
) {
    if (0..KINDS as jint).contains(&kind) {
        METRICS.series[kind as usize].push(nanos);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeServerStart(
    _env: JNIEnv,
    _class: JClass,
    nanos: jlong,
) {
    if nanos <= 0 {
        return;
    }
    let old = METRICS.server_last_start.swap(nanos as u64, Relaxed);
    if old != 0 && nanos as u64 > old {
        METRICS.series[SERVER_INTERVAL].push((nanos as u64 - old) as jlong);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeChunkEvent(
    _env: JNIEnv,
    _class: JClass,
    kind: jint,
) {
    if (0..5).contains(&kind) {
        METRICS.chunks[kind as usize].fetch_add(1, Relaxed);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeMetrics(
    mut env: JNIEnv,
    _class: JClass,
) -> jfloatArray {
    floats(&mut env, &averages())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeHistory(
    mut env: JNIEnv,
    _class: JClass,
    kind: jint,
) -> jfloatArray {
    if !(0..KINDS as jint).contains(&kind) {
        return floats(&mut env, &[]);
    }
    floats(&mut env, &METRICS.series[kind as usize].samples())
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeQueueRegeneration(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    x: jint,
    z: jint,
    radius: jint,
) -> jstring {
    let result = match env.get_string(&path) {
        Ok(text) => regen::queue(&config_directory(), &text.to_string_lossy(), x, z, radius),
        Err(error) => Err(format!("Invalid save path: {error}")),
    };
    let message = result.unwrap_or_else(|e| format!("Regeneration not queued: {e}"));
    save_message(message.clone());
    jstring(&mut env, &message)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeRegenerationStatus(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    jstring(&mut env, &read_message())
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn telemetry_series_is_bounded_and_ordered() {
        let values = Series::new();
        for n in 1..300 {
            values.push(n * 1_000_000);
        }
        let samples = values.samples();
        assert_eq!(samples.len(), HISTORY);
        assert_eq!(samples[0], 60.0);
        assert_eq!(*samples.last().unwrap(), 299.0);
    }
    #[test]
    fn rejects_invalid_samples() {
        let values = Series::new();
        values.push(-1);
        values.push(0);
        values.push(11_000_000_000);
        assert!(values.samples().is_empty());
    }
}
