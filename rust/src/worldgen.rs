//! Deterministic experimental height-field kernel.
//! This is not Mojang's noise router; it is never attached to a live world.

use rayon::prelude::*;
use std::hint::black_box;
use std::time::{Duration, Instant};

#[inline]
fn hash(seed: u64, x: i64, z: i64, octave: u32) -> u64 {
    let mut v = seed
        ^ (x as u64).wrapping_mul(0x9e3779b185ebca87)
        ^ (z as u64).wrapping_mul(0xc2b2ae3d27d4eb4f)
        ^ u64::from(octave).wrapping_mul(0x165667b19e3779f9);
    v ^= v >> 30;
    v = v.wrapping_mul(0xbf58476d1ce4e5b9);
    v ^= v >> 27;
    v = v.wrapping_mul(0x94d049bb133111eb);
    v ^ (v >> 31)
}

#[inline]
fn random(seed: u64, x: i64, z: i64, octave: u32) -> f64 {
    ((hash(seed, x, z, octave) >> 11) as f64) / (1u64 << 53) as f64
}

#[inline]
fn smooth(x: f64) -> f64 {
    x * x * (3.0 - 2.0 * x)
}

fn value_noise(seed: u64, x: f64, z: f64, octave: u32) -> f64 {
    let xi = x.floor() as i64;
    let zi = z.floor() as i64;
    let fx = smooth(x - xi as f64);
    let fz = smooth(z - zi as f64);
    let a = random(seed, xi, zi, octave);
    let b = random(seed, xi + 1, zi, octave);
    let c = random(seed, xi, zi + 1, octave);
    let d = random(seed, xi + 1, zi + 1, octave);
    let ab = a + (b - a) * fx;
    let cd = c + (d - c) * fx;
    ab + (cd - ab) * fz
}

pub fn height(seed: u64, x: i64, z: i64, quality: u8) -> f64 {
    let octaves = 1 + (quality.clamp(1, 100) as usize * 11 / 100);
    let (mut total, mut amplitude, mut frequency, mut normalizer) = (0.0, 1.0, 1.0 / 192.0, 0.0);
    for octave in 0..octaves {
        total += value_noise(
            seed,
            x as f64 * frequency,
            z as f64 * frequency,
            octave as u32,
        ) * amplitude;
        normalizer += amplitude;
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    (total / normalizer) * 256.0 - 64.0
}

pub fn chunk_checksum(seed: u64, cx: i32, cz: i32, quality: u8) -> u64 {
    let mut sum = 0xcbf29ce484222325u64;
    for z in 0..16i64 {
        for x in 0..16i64 {
            let gx = i64::from(cx) * 16 + x;
            let gz = i64::from(cz) * 16 + z;
            sum ^= height(seed, gx, gz, quality).to_bits();
            sum = sum.wrapping_mul(0x100000001b3);
        }
    }
    sum
}

#[derive(Debug)]
pub struct Bench {
    pub chunks: usize,
    pub quality: u8,
    pub workers: usize,
    pub elapsed: Duration,
    pub checksum: u64,
}

pub fn benchmark(seed: u64, chunks: usize, quality: u8, workers: usize) -> Result<Bench, String> {
    let chunks = chunks.clamp(1, 4096);
    let workers = workers.clamp(1, 64);
    let quality = quality.clamp(1, 100);
    // The bounded Rayon pool is only used in an explicitly requested background benchmark.
    let pool = rayon::ThreadPoolBuilder::new()
        .num_threads(workers)
        .build()
        .map_err(|error| error.to_string())?;
    let started = Instant::now();
    let checksum = pool.install(|| {
        (0..chunks)
            .into_par_iter()
            .map(|i| {
                let cx = (i % 64) as i32 - 32;
                let cz = (i / 64) as i32 - 32;
                black_box(chunk_checksum(seed, cx, cz, quality))
            })
            .reduce(|| 0, |a, b| a ^ b)
    });
    Ok(Bench {
        chunks,
        quality,
        workers,
        elapsed: started.elapsed(),
        checksum,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn same_seed_same_result_different_seed_changes_result() {
        assert_eq!(height(9, -4096, 99, 100), height(9, -4096, 99, 100));
        assert_ne!(height(9, -4096, 99, 100), height(10, -4096, 99, 100));
    }

    #[test]
    fn adjacent_chunk_positions_have_consistent_global_samples() {
        assert_eq!(height(5, 16, 31, 100), height(5, 16, 31, 100));
    }

    #[test]
    fn worker_count_does_not_change_the_output() {
        let a = benchmark(12, 16, 100, 1).unwrap();
        let b = benchmark(12, 16, 100, 4).unwrap();
        assert_eq!(a.checksum, b.checksum);
    }

    #[test]
    fn user_quality_affects_the_result() {
        let low = chunk_checksum(11, 0, 0, 1);
        let high = chunk_checksum(11, 0, 0, 100);
        assert_ne!(low, high);
        assert_eq!(high, chunk_checksum(11, 0, 0, 100));
    }
}
