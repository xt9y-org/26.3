//! Opt-in Rust profiler. No native work occurs while the UI is closed.
//! Fixed-size lock-free histories avoid heap allocation in the frame/tick path.

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jfloatArray, jint, jlong};
use std::array::from_fn;
use std::sync::LazyLock;
use std::sync::atomic::{AtomicU64, AtomicUsize, Ordering::Relaxed};

const HISTORY: usize = 240;
const FRAME: usize = 0;
const RENDER: usize = 1;
const CLIENT: usize = 2;
const SERVER: usize = 3;
const SERVER_INTERVAL: usize = 4;
const KINDS: usize = 5;

struct Series {
    samples: [AtomicU64; HISTORY],
    count: AtomicUsize,
}

impl Series {
    fn new() -> Self {
        Self {
            samples: from_fn(|_| AtomicU64::new(0)),
            count: AtomicUsize::new(0),
        }
    }

    fn push(&self, nanos: jlong) {
        if !(1..=10_000_000_000).contains(&nanos) {
            return;
        }
        let slot = self.count.fetch_add(1, Relaxed) % HISTORY;
        self.samples[slot].store(nanos as u64, Relaxed);
    }

    fn recent(&self, limit: usize, output: &mut [f32]) -> usize {
        let count = self.count.load(Relaxed);
        let length = count.min(HISTORY).min(limit).min(output.len());
        for (index, item) in output.iter_mut().take(length).enumerate() {
            let slot = (count - length + index) % HISTORY;
            *item = self.samples[slot].load(Relaxed) as f32 / 1_000_000.0;
        }
        length
    }

    fn mean(&self, limit: usize) -> f32 {
        let mut stack = [0.0f32; HISTORY];
        let count = self.recent(limit, &mut stack);
        if count == 0 {
            return 0.0;
        }
        stack[..count].iter().sum::<f32>() / count as f32
    }

    fn percentile95(&self, limit: usize) -> f32 {
        let mut stack = [0.0f32; HISTORY];
        let count = self.recent(limit, &mut stack);
        if count == 0 {
            return 0.0;
        }
        let values = &mut stack[..count];
        values.sort_unstable_by(f32::total_cmp);
        values[(count * 95).div_ceil(100).saturating_sub(1)]
    }
}

struct Metrics {
    times: [Series; KINDS],
    chunks: [AtomicU64; 3],
    last_server_start: AtomicU64,
}

static METRICS: LazyLock<Metrics> = LazyLock::new(|| Metrics {
    times: from_fn(|_| Series::new()),
    chunks: from_fn(|_| AtomicU64::new(0)),
    last_server_start: AtomicU64::new(0),
});

fn snapshot() -> [f32; 15] {
    let series = &METRICS.times;
    let frame = series[FRAME].mean(120);
    let interval = series[SERVER_INTERVAL].mean(20);

    [
        if frame > 0.0 { 1000.0 / frame } else { 0.0 },
        frame,
        series[RENDER].mean(120),
        series[CLIENT].mean(60),
        series[SERVER].mean(40),
        if interval > 0.0 {
            (1000.0 / interval).min(20.0)
        } else {
            0.0
        },
        METRICS.chunks[0].load(Relaxed) as f32,
        METRICS.chunks[1].load(Relaxed) as f32,
        METRICS.chunks[2].load(Relaxed) as f32,
        0.0,
        0.0,
        series[FRAME].recent_last(),
        series[SERVER].recent_last(),
        series[FRAME].percentile95(120),
        series[SERVER].percentile95(40),
    ]
}

impl Series {
    fn recent_last(&self) -> f32 {
        let count = self.count.load(Relaxed);
        if count == 0 {
            0.0
        } else {
            self.samples[(count - 1) % HISTORY].load(Relaxed) as f32 / 1_000_000.0
        }
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
        METRICS.times[kind as usize].push(nanos);
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
    let old = METRICS.last_server_start.swap(nanos as u64, Relaxed);
    if old != 0 && (nanos as u64) > old {
        METRICS.times[SERVER_INTERVAL].push((nanos as u64 - old) as jlong);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeChunkEvent(
    _env: JNIEnv,
    _class: JClass,
    kind: jint,
) {
    if (0..3).contains(&kind) {
        METRICS.chunks[kind as usize].fetch_add(1, Relaxed);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_dev_xt9y_perf_NativeBridge_nativeMetrics(
    mut env: JNIEnv,
    _class: JClass,
) -> jfloatArray {
    let metrics = snapshot();
    let Ok(array) = env.new_float_array(metrics.len() as i32) else {
        return std::ptr::null_mut();
    };
    if env.set_float_array_region(&array, 0, &metrics).is_err() {
        return std::ptr::null_mut();
    }
    array.into_raw()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bounded_samples_ignore_invalid_durations() {
        let samples = Series::new();
        samples.push(-1);
        samples.push(0);
        samples.push(10_000_000_001);
        assert_eq!(samples.mean(120), 0.0);
        for n in 1..=300 {
            samples.push(n * 1_000_000);
        }
        assert_eq!(samples.recent_last(), 300.0);
        assert_eq!(samples.mean(240), 180.5);
    }

    #[test]
    fn computes_p95_without_allocations() {
        let samples = Series::new();
        for n in 1..=100 {
            samples.push(n * 1_000_000);
        }
        assert_eq!(samples.percentile95(100), 95.0);
        assert_eq!(samples.percentile95(20), 99.0);
    }

    #[test]
    fn zero_samples_do_not_produce_nan_or_infinity() {
        assert!(snapshot().iter().all(|value| value.is_finite()));
    }
}
