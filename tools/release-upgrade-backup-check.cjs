// Read exported files only. Never opens/writes a user's original database or logs titles.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {DatabaseSync} = require('node:sqlite');
const {migrate} = require('./migration-roundtrip-test.cjs');

function load(file) {
  const bytes = fs.readFileSync(file);
  const text = bytes.toString('utf8');
  assert.ok(text.startsWith('Sauce exported Date '), 'Export header missing');
  const payload = JSON.parse(text.slice(text.indexOf('{')));
  for (const key of ['entries','creators','subscriptions','subscription_seen_codes','subscription_events','daily_read_activity','reading_sessions','popular_tags']) {
    assert.ok(Array.isArray(payload[key]), `Missing ${key}`);
  }
  assert.equal(new Set(payload.entries.map(row => row.code)).size, payload.entries.length, 'Duplicate entry codes');
  return {payload, hash: crypto.createHash('sha256').update(bytes).digest('hex')};
}
function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).sort().join(',') + ']';
  if (value !== null && typeof value === 'object') return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}';
  return JSON.stringify(value);
}
function samePrivate(actual, expected, message) {
  // Do not allow assertion diagnostics to print private library content.
  assert.ok(canonical(actual) === canonical(expected), message);
}
const before = load(process.argv[2]);
assert.ok(!before.payload.source_platform, 'Expected a genuine legacy V1 export');
const p = before.payload;
const db = new DatabaseSync(':memory:');
try {
  db.exec(fs.readFileSync(path.join(__dirname, 'fixtures/sauce-tracker-1.9-schema.sql'), 'utf8'));
  db.exec('PRAGMA user_version=1; BEGIN');
  function insert(table, row) {
    const columns = Object.keys(row);
    assert.ok(columns.every(column => /^[a-z_]+$/.test(column)), 'Invalid column');
    db.prepare(`INSERT INTO ${table} (${columns.join(',')}) VALUES (${columns.map(() => '?').join(',')})`)
      .run(...columns.map(column => typeof row[column] === 'boolean' ? Number(row[column]) : row[column]));
  }
  const tagIds = new Map();
  function tag(name, type, pinned = false, url = '') {
    const key = type + ':' + name.trim().toLowerCase();
    if (!tagIds.has(key)) {
      insert('tags', {name, type, normalized_name:name.trim().toLowerCase(), pinned, source_url:url});
      tagIds.set(key, Number(db.prepare('SELECT last_insert_rowid() AS id').get().id));
    }
    return tagIds.get(key);
  }
  for (const entry of p.entries) {
    assert.ok(Number.isSafeInteger(entry.code) && entry.code > 0, 'Invalid entry code');
    assert.ok(Number.isInteger(entry.rating) && entry.rating >= 0 && entry.rating <= 5, 'Invalid rating');
    const {tags, read, ...row} = entry;
    insert('entries', {...row, read_state:read});
    assert.ok(Array.isArray(tags), 'Missing entry tags');
    for (const item of tags) db.prepare('INSERT OR IGNORE INTO entry_tags VALUES (?,?)').run(entry.code, tag(item.name, item.type));
  }
  for (const creator of p.creators) {
    const id = tag(creator.name, creator.type, creator.pinned, creator.source_url);
    db.prepare('UPDATE tags SET pinned=?,source_url=? WHERE id=?').run(Number(creator.pinned), creator.source_url, id);
  }
  for (const table of ['popular_tags','daily_read_activity','reading_sessions','entry_heatmap_cache']) {
    for (const row of p[table] || []) insert(table, row);
  }
  const subscriptionIds = new Map();
  for (const row of p.subscriptions) {
    const routeKey = row.route_type + '|' + row.route_name.trim().toLowerCase();
    insert('subscriptions', {...row, route_key:routeKey});
    subscriptionIds.set(routeKey, Number(db.prepare('SELECT last_insert_rowid() AS id').get().id));
  }
  for (const table of ['subscription_seen_codes','subscription_events']) {
    for (const row of p[table]) {
      const {route_name, route_type, ...fields} = row;
      const subscriptionId = subscriptionIds.get(route_type + '|' + route_name.trim().toLowerCase());
      assert.ok(subscriptionId, 'Subscription child has no parent');
      insert(table, {subscription_id:subscriptionId, ...fields});
    }
  }
  db.exec('COMMIT');
  const preserved = {};
  for (const table of ['entries','tags','entry_tags','popular_tags','daily_read_activity','reading_sessions','subscriptions','subscription_seen_codes','subscription_events','entry_heatmap_cache']) {
    const columns = db.prepare(`PRAGMA table_info(${table})`).all().map(row => row.name);
    // route_key deliberately acquires the profile/source prefix during migration.
    preserved[table] = {columns:columns.filter(column => !(table === 'subscriptions' && column === 'route_key'))};
    preserved[table].rows = canonical(db.prepare(`SELECT ${preserved[table].columns.join(',')} FROM ${table}`).all());
  }
  migrate(db);
  for (const [table, saved] of Object.entries(preserved)) {
    assert.ok(canonical(db.prepare(`SELECT ${saved.columns.join(',')} FROM ${table}`).all()) === saved.rows, `Lost legacy data in ${table}`);
  }
  assert.equal(db.prepare('PRAGMA integrity_check').get().integrity_check, 'ok');
  assert.equal(db.prepare('PRAGMA foreign_key_check').all().length, 0);
  const profiles = db.prepare('SELECT id,name,kind FROM profiles ORDER BY is_main DESC').all().map(row => ({...row}));
  assert.deepEqual(profiles, [{id:'main',name:'NHentai',kind:'SOURCE_LOCKED'},{id:'mangadex-default',name:'MangaDex',kind:'SOURCE_LOCKED'}]);
  assert.equal(db.prepare('SELECT COUNT(*) n FROM profile_entries WHERE profile_id=?').get('main').n, p.entries.length);
  assert.equal(db.prepare('SELECT COUNT(*) n FROM profile_entries WHERE profile_id=?').get('mangadex-default').n, 0);
  for (const entry of p.entries) {
    const state = db.prepare(`SELECT pe.rating,pe.read_state,pe.pinned,pe.read_at,pe.added_at,pe.fetched_at FROM profile_entries pe
      JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.profile_id='main' AND se.source_id='nhentai' AND se.remote_id=?`).get(String(entry.code));
    assert.deepEqual({...state}, {rating:entry.rating,read_state:Number(entry.read),pinned:Number(entry.pinned),read_at:entry.read_at,added_at:entry.added_at,fetched_at:entry.fetched_at});
  }
  console.log(JSON.stringify({backupSha256:before.hash,verifiedExportMigration:true,entries:p.entries.length,read:p.entries.filter(row=>row.read).length,pinned:p.entries.filter(row=>row.pinned).length,
    tags:db.prepare('SELECT COUNT(*) n FROM tags').get().n,creators:p.creators.length,readingSessions:p.reading_sessions.length,subscriptions:p.subscriptions.length,
    seen:p.subscription_seen_codes.length,events:p.subscription_events.length,dailyActivity:p.daily_read_activity.length,profiles},null,2));
} finally { db.close(); }

if (process.argv[3]) {
  const after = load(process.argv[3]);
  const a = after.payload;
  assert.ok(a.source_platform, 'Upgraded export lacks source platform');
  const compared = {};
  let refreshedSubscriptionChecks = 0;
  for (const table of ['entries','creators','reading_sessions','daily_read_activity','subscriptions','subscription_seen_codes','subscription_events','popular_tags','hidden_suggested_entries']) {
    const oldRows = p[table] || [];
    const keys = [...new Set(oldRows.flatMap(Object.keys))].filter(key => !(table === 'subscriptions' && key === 'last_checked_at'));
    if (table === 'subscriptions') {
      for (const row of oldRows) {
        const updated = a.subscriptions.find(item=>item.route_type===row.route_type && item.route_name===row.route_name);
        assert.ok(updated, 'Lost subscription');
        if (updated.last_checked_at !== row.last_checked_at) {
          const oldTime = row.last_checked_at ? Date.parse(row.last_checked_at) : 0;
          const newTime = Date.parse(updated.last_checked_at);
          assert.ok(Number.isFinite(newTime) && newTime >= oldTime, 'Subscription check timestamp moved backwards');
          refreshedSubscriptionChecks++;
        }
      }
    }
    const oldFingerprints = oldRows.map(row=>canonical(Object.fromEntries(keys.map(key=>[key,row[key]])))).sort();
    const newFingerprints = (a[table] || []).map(row=>canonical(Object.fromEntries(keys.map(key=>[key,row[key]])))).sort();
    samePrivate(newFingerprints, oldFingerprints, `Changed ${table} in real upgrade (before ${oldRows.length}, after ${(a[table] || []).length})`);
    compared[table] = oldRows.length;
  }
  for (const field of ['hidden_suggested_codes','suggestion_category_weights','entry_pin_priority_enabled']) {
    samePrivate(a[field], p[field], `Changed ${field}`);
  }
  const oldSettings = p.portable_preferences.values;
  const newSettings = a.portable_preferences.values;
  for (const key of Object.keys(oldSettings)) {
    samePrivate(newSettings[key], oldSettings[key], `Changed portable preference: ${key}`);
  }
  const profiles = a.source_platform.profiles.map(row => ({id:row.id,name:row.name,kind:row.kind}));
  assert.deepEqual(profiles.sort((x,y)=>x.id.localeCompare(y.id)), [{id:'main',name:'NHentai',kind:'SOURCE_LOCKED'},{id:'mangadex-default',name:'MangaDex',kind:'SOURCE_LOCKED'}]);
  assert.equal(a.source_platform.profile_entries.filter(row=>row.profile_id==='main').length, p.entries.length);
  assert.equal(a.source_platform.profile_entries.filter(row=>row.profile_id==='mangadex-default').length, 0);
  const sourceEntries = new Map(a.source_platform.source_entries.map(row=>[row.source_id+':'+row.remote_id,row]));
  const states = new Map(a.source_platform.profile_entries.filter(row=>row.profile_id==='main').map(row=>[row.source_entry_id,row]));
  for (const entry of p.entries) {
    const source = sourceEntries.get('nhentai:'+entry.code);
    assert.ok(source, 'Missing migrated source entry');
    samePrivate({title:source.title,url:source.canonical_url,pages:source.unit_count},
      {title:entry.title,url:entry.source_url,pages:entry.num_pages}, 'Changed migrated metadata');
    const state = states.get(source.id);
    assert.ok(state, 'Missing migrated personal state');
    samePrivate({read:state.read_state,rating:state.rating,pinned:state.pinned,read_at:state.read_at,added_at:state.added_at,fetched_at:state.fetched_at},
      {read:Number(entry.read),rating:entry.rating,pinned:Number(entry.pinned),read_at:entry.read_at,added_at:entry.added_at,fetched_at:entry.fetched_at}, 'Changed migrated personal state');
    const tags = a.source_platform.source_entry_tags.filter(row=>row.source_entry_id===source.id).map(row=>({name:row.name,type:row.type}));
    samePrivate(tags, entry.tags, 'Changed migrated source tags');
  }
  assert.ok(a.reading_sessions.every(row=>row.profile_id==='main' && row.source_id==='nhentai' && row.remote_id===String(row.entry_code)), 'Incorrect migrated history scope');
  assert.ok(a.subscriptions.every(row=>row.profile_id==='main' && row.source_id==='nhentai'), 'Incorrect migrated subscription scope');
  assert.equal(a.source_platform.app_state[0].active_profile_id, 'main', 'Wrong initial active profile');
  console.log(JSON.stringify({realUpgradeLossless:true,afterSha256:after.hash,compared,
    refreshedSubscriptionChecks,
    preservedPortablePreferences:Object.keys(oldSettings).length,
    addedPortablePreferenceKeys:Object.keys(newSettings).filter(key=>!(key in oldSettings))},null,2));
}
