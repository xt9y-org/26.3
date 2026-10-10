//! Offline, backed-up Minecraft Anvil header invalidation.
//!
//! Only the Overworld is supported. Never touches an open save: Minecraft's
//! session.lock must be exclusively lockable. Never called on render/tick threads.
//! On next launch, Minecraft's *vanilla* generator fills absent chunks and
//! recalculates their light data. We do not substitute a different noise router.

use std::collections::{BTreeMap, BTreeSet};
use std::fs::{self, OpenOptions};
use std::io::{Read, Write};
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

const MAX_RADIUS: i32 = 2;
const PLAN_FILE: &str = "xt9y263-pending-regen.txt";

#[derive(Debug, Clone)]
struct Plan {
    world: PathBuf,
    x: i32,
    z: i32,
    radius: i32,
}

fn hex(bytes: &[u8]) -> String {
    const DIGITS: &[u8; 16] = b"0123456789abcdef";
    let mut s = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        s.push(DIGITS[(b >> 4) as usize] as char);
        s.push(DIGITS[(b & 15) as usize] as char);
    }
    s
}

fn unhex(s: &str) -> Result<Vec<u8>, String> {
    let bytes = s.as_bytes();
    if !bytes.len().is_multiple_of(2) || bytes.len() > 16_384 {
        return Err("Invalid queued save path length".into());
    }
    bytes.chunks_exact(2).map(|p| {
        let hi = (p[0] as char).to_digit(16).ok_or("Bad hexadecimal path")?;
        let lo = (p[1] as char).to_digit(16).ok_or("Bad hexadecimal path")?;
        Ok(((hi << 4) | lo) as u8)
    }).collect::<Result<Vec<_>, &str>>().map_err(str::to_string)
}

impl Plan {
    fn encode(&self) -> String {
        format!("1\t{}\t{}\t{}\t{}\n", self.x, self.z, self.radius,
            hex(self.world.to_string_lossy().as_bytes()))
    }

    fn decode(text: &str) -> Result<Self, String> {
        let parts: Vec<_> = text.trim().split('\t').collect();
        if parts.len() != 5 || parts[0] != "1" {
            return Err("Unknown regeneration queue format".into());
        }
        let parse = |value: &str| value.parse::<i32>().map_err(|_| "Invalid coordinate".to_string());
        let x = parse(parts[1])?;
        let z = parse(parts[2])?;
        let radius = parse(parts[3])?;
        if !(0..=MAX_RADIUS).contains(&radius) { return Err("Radius exceeds safety limit".into()); }
        let path = String::from_utf8(unhex(parts[4])?).map_err(|e| e.to_string())?;
        Ok(Self { world: PathBuf::from(path), x, z, radius })
    }
}

pub fn queue(config: &Path, world: &str, x: i32, z: i32, radius: i32) -> Result<String, String> {
    if !(0..=MAX_RADIUS).contains(&radius) {
        return Err("Only a radius of 0 to 2 chunks is permitted".into());
    }
    let world = fs::canonicalize(world).map_err(|e| e.to_string())?;
    if !world.join("level.dat").is_file() || !world.join("region").is_dir() {
        return Err("This is not a valid Overworld save folder".into());
    }
    // One pending plan at a time; do not silently overwrite an earlier selection.
    let path = config.join(PLAN_FILE);
    let plan = Plan { world, x, z, radius };
    OpenOptions::new().write(true).create_new(true).open(&path)
        .and_then(|mut f| f.write_all(plan.encode().as_bytes()))
        .map_err(|e| format!("Cannot queue: {e}. Clear the previous request first."))?;
    Ok(format!("Queued {} chunk(s) around ({x}, {z}). Close Minecraft and restart to apply backed-up regeneration.", (radius * 2 + 1).pow(2)))
}

fn selected(plan: &Plan) -> BTreeMap<(i32, i32), BTreeSet<usize>> {
    let mut out: BTreeMap<(i32, i32), BTreeSet<usize>> = BTreeMap::new();
    for z in (plan.z - plan.radius)..=(plan.z + plan.radius) {
        for x in (plan.x - plan.radius)..=(plan.x + plan.radius) {
            let region = (x.div_euclid(32), z.div_euclid(32));
            let index = (x.rem_euclid(32) + z.rem_euclid(32) * 32) as usize;
            out.entry(region).or_default().insert(index);
        }
    }
    out
}

fn invalidate(plan: &Plan) -> Result<String, String> {
    let save = fs::canonicalize(&plan.world).map_err(|e| e.to_string())?;
    if !save.join("level.dat").is_file() { return Err("Save level.dat is missing".into()); }
    let lock = OpenOptions::new().read(true).write(true)
        .open(save.join("session.lock")).map_err(|e| format!("Missing session.lock: {e}"))?;
    lock.try_lock().map_err(|e| format!("World is still open or locked: {e}"))?;
    // lock must remain in scope until all region files have been safely committed.
    let mut changes: Vec<(PathBuf, PathBuf)> = Vec::new();
    let stamp = SystemTime::now().duration_since(UNIX_EPOCH)
        .map_err(|e| e.to_string())?.as_millis();
    let backup = save.join("xt9y263-backups").join(format!("{stamp}"));
    let work = (|| -> Result<usize, String> {
        let mut count = 0;
        for directory in ["region", "entities", "poi"] {
            for ((rx, rz), indices) in selected(plan) {
                let relative = format!("{directory}/r.{rx}.{rz}.mca");
                let file = save.join(&relative);
                if !file.exists() { continue; }
                // External oversized chunks also need special handling; refuse rather
                // than silently leave an entity/POI sidecar behind.
                for index in &indices {
                    let x = rx * 32 + (index % 32) as i32;
                    let z = rz * 32 + (index / 32) as i32;
                    if save.join(format!("{directory}/c.{x}.{z}.mcc")).exists() {
                        return Err(format!("External .mcc file in {directory}: cannot safely regenerate"));
                    }
                }
                let mut bytes = Vec::new();
                fs::File::open(&file).and_then(|mut f| f.read_to_end(&mut bytes))
                    .map_err(|e| format!("Reading {relative}: {e}"))?;
                if bytes.len() < 8192 || bytes.len() % 4096 != 0 {
                    return Err(format!("Invalid Anvil region header: {relative}"));
                }
                let mut changed = false;
                for index in indices {
                    if bytes[index * 4..index * 4 + 4] != [0, 0, 0, 0] {
                        changed = true;
                        count += 1;
                    }
                    bytes[index * 4..index * 4 + 4].fill(0);
                    bytes[4096 + index * 4..4096 + index * 4 + 4].fill(0);
                }
                if !changed { continue; }
                let original = backup.join(&relative);
                fs::create_dir_all(original.parent().ok_or("Invalid backup directory")?)
                    .map_err(|e| e.to_string())?;
                fs::copy(&file, &original)
                    .map_err(|e| format!("Backing up {relative}: {e}"))?;
                changes.push((file.clone(), original));
                let tmp = file.with_extension("mca.xt9y263.tmp");
                let mut output = OpenOptions::new().write(true).create_new(true)
                    .open(&tmp).map_err(|e| e.to_string())?;
                output.write_all(&bytes).and_then(|()| output.sync_all())
                    .map_err(|e| format!("Writing {relative}: {e}"))?;
                drop(output);
                // World save is locked. On Windows an existing destination cannot
                // be atomically overwritten, so remove it only after a backup exists.
                if cfg!(windows) { fs::remove_file(&file).map_err(|e| e.to_string())?; }
                fs::rename(&tmp, &file).map_err(|e| format!("Replacing {relative}: {e}"))?;
            }
        }
        Ok(count)
    })();
    if work.is_err() {
        // Best-effort rollback of all touched files, never discard originals.
        for (path, original) in changes.iter().rev() {
            let _ = fs::copy(original, path);
        }
    }
    let _keep_locked = lock;
    match &mut work {
        Ok(n) => Ok(format!("Regenerated queue: invalidated {n} stored chunk entries; vanilla will rebuild terrain and lighting. Backup: {}", backup.display())),
        Err(error) => Err(format!("{error}. Original region files retained under {}", backup.display())),
    }
}

pub fn apply_pending(config: &Path) -> String {
    let path = config.join(PLAN_FILE);
    let text = match fs::read_to_string(&path) {
        Ok(s) => s,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
            return "No pending regeneration".into();
        }
        Err(error) => return format!("Cannot read queue: {error}"),
    };
    let plan = match Plan::decode(&text) {
        Ok(plan) => plan,
        Err(error) => return error,
    };
    match invalidate(&plan) {
        Ok(message) => {
            let _ = fs::remove_file(&path);
            message
        }
        Err(error) => error,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn create_world(root: &Path) -> PathBuf {
        let world = root.join("world");
        fs::create_dir_all(world.join("region")).unwrap();
        fs::write(world.join("level.dat"), b"world").unwrap();
        fs::write(world.join("session.lock"), b"").unwrap();
        world
    }

    #[test]
    fn plan_round_trip_and_coordinates() {
        let plan = Plan { world: PathBuf::from("/tmp/world test"), x: -32, z: 65, radius: 2 };
        let reloaded = Plan::decode(&plan.encode()).unwrap();
        assert_eq!(reloaded.x, -32);
        assert_eq!(reloaded.world, plan.world);
        assert_eq!(selected(&reloaded).values().map(BTreeSet::len).sum::<usize>(), 25);
        assert!(Plan::decode("garbage").is_err());
    }

    #[test]
    fn invalidates_only_selected_header_and_backs_up() {
        let base = std::env::temp_dir().join(format!("xt9y263-regen-test-{}-{}", std::process::id(),
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos()));
        let world = create_world(&base);
        let mut region = vec![0u8; 8192];
        region[..4].copy_from_slice(&[0, 0, 2, 1]);
        region[4..8].copy_from_slice(&[0, 0, 3, 1]);
        region[4096..4100].copy_from_slice(&[5, 6, 7, 8]);
        fs::write(world.join("region/r.0.0.mca"), &region).unwrap();
        let plan = Plan { world: world.clone(), x: 0, z: 0, radius: 0 };
        invalidate(&plan).unwrap();
        let new_data = fs::read(world.join("region/r.0.0.mca")).unwrap();
        assert_eq!(&new_data[..4], &[0, 0, 0, 0]);
        assert_eq!(&new_data[4..8], &[0, 0, 3, 1]);
        assert_eq!(&new_data[4096..4100], &[0, 0, 0, 0]);
        assert_eq!(fs::read_dir(world.join("xt9y263-backups")).unwrap().count(), 1);
        fs::remove_dir_all(base).unwrap();
    }
}
