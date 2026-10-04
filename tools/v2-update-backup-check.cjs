// Read-only comparison of two actual V2 exports around an in-place APK update.
// Never opens a device database or includes private field values in errors.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');

function load(file) {
  const bytes = fs.readFileSync(file);
  const text = bytes.toString('utf8');
  assert.ok(text.startsWith('Sauce exported Date '), 'Export header missing');
  const payload = JSON.parse(text.slice(text.indexOf('{')));
  assert.equal(payload.format, 'SAUCE_TRACKER_EXPORT_V2', 'Expected V2 export');
  assert.ok(payload.source_platform, 'Source platform missing');
  for (const name of ['entries', 'creators', 'reading_sessions', 'subscriptions',
    'subscription_seen_codes', 'subscription_events', 'daily_read_activity']) {
    assert.ok(Array.isArray(payload[name]), `Required array missing: ${name}`);
  }
  return {payload, sha256: createHash('sha256').update(bytes).digest('hex')};
}
function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).sort().join(',') + ']';
  if (value !== null && typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}';
  }
  return JSON.stringify(value);
}
function unchanged(a, b, name) {
  assert.ok(canonical(a) === canonical(b), `Changed data: ${name}`);
}
function allowForwardTimestamp(before, after, name) {
  if (before === after) return;
  const oldTime = before ? Date.parse(before) : 0;
  const newTime = after ? Date.parse(after) : 0;
  assert.ok(Number.isFinite(oldTime) && Number.isFinite(newTime) && newTime >= oldTime,
    `Timestamp moved backward: ${name}`);
}
function compareRows(before, after, name, identity, timestampFields) {
  assert.equal(after.length, before.length, `Row count changed: ${name}`);
  const byId = new Map(after.map(row => [identity(row), row]));
  assert.equal(byId.size, after.length, `Duplicate identity: ${name}`);
  const strip = row => Object.fromEntries(Object.entries(row).filter(([key]) => !timestampFields.includes(key)));
  for (const row of before) {
    const updated = byId.get(identity(row));
    assert.ok(updated, `Missing row: ${name}`);
    for (const field of timestampFields) allowForwardTimestamp(row[field], updated[field], name + '.' + field);
    unchanged(strip(updated), strip(row), name);
  }
}
function compare(before, after) {
  for (const key of new Set([...Object.keys(before), ...Object.keys(after)])) {
    if (key === 'exported_at') continue;
    if (key === 'subscriptions') {
      compareRows(before[key], after[key], key,
        row => JSON.stringify([row.profile_id, row.source_id, row.route_type, row.route_name]), ['last_checked_at']);
    } else if (key === 'source_platform') {
      for (const name of new Set([...Object.keys(before[key]), ...Object.keys(after[key])])) {
        if (name === 'profiles') {
          compareRows(before[key][name], after[key][name], name, row => row.id, ['last_used_at']);
        } else unchanged(after[key][name], before[key][name], 'source_platform.' + name);
      }
    } else unchanged(after[key], before[key], key);
  }
}
if (require.main === module) {
  assert.ok(process.argv[2] && process.argv[3], 'Usage: node tools/v2-update-backup-check.cjs BEFORE AFTER');
  const before = load(process.argv[2]), after = load(process.argv[3]);
  compare(before.payload, after.payload);
  const platform = after.payload.source_platform;
  console.log(JSON.stringify({inPlaceV2UpdateLossless: true, beforeSha256: before.sha256,
    afterSha256: after.sha256, entries: after.payload.entries.length,
    sourceEntries: platform.source_entries.length, profiles: platform.profiles.length,
    sessions: after.payload.reading_sessions.length,
    mangaDexEntries: platform.source_entries.filter(row => row.source_id === 'mangadex').length,
    readerPositions: platform.source_reader_progress.length,
    chapterProgress: platform.source_chapter_progress.length}, null, 2));
}
module.exports = {compare};
