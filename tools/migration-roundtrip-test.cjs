// Run the production schema/migration SQL on disposable 1.9 SQLite fixtures.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {DatabaseSync} = require('node:sqlite');
const root = path.join(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'app/src/main/java/com/roinur/saucetracker/data/database/SauceTrackerDatabase.kt'), 'utf8');
const legacySchema = fs.readFileSync(path.join(__dirname, 'fixtures/sauce-tracker-1.9-schema.sql'), 'utf8');

function method(name) {
  const start = source.indexOf(`private fun ${name}(`);
  assert.ok(start >= 0, `production method ${name}`);
  const end = source.indexOf('\n    private fun ', start + 1);
  return source.slice(start, end < 0 ? source.length : end);
}
function statements(body) {
  return [...body.matchAll(/db\.execSQL\(\s*(?:"""([\s\S]*?)"""\.trimIndent\(\)|"((?:\\.|[^"\\])*)")/g)]
    .map(match => match[1] || JSON.parse('"' + match[2] + '"'));
}
const migrationSql = [2, 3, 4, 5, 6, 7].flatMap(version => statements(method(`ensureV${version}Schema`)));
const currentVersion = Number(fs.readFileSync(path.join(root, 'app/src/main/java/com/roinur/saucetracker/data/database/DatabaseSchema.kt'), 'utf8').match(/VERSION = (\d+)/)[1]);
assert.equal(currentVersion, 8, 'new schema versions require coverage here');
const subtitleQuery = method('migrateLegacyAlternateTitles').match(/db\.rawQuery\("""([\s\S]*?)"""/)[1];
assert.ok(migrationSql.length > 40, 'all migration steps extracted');
// This is the bound JSON update step; the production Kotlin serializer has its
// own JVM tests, including control characters and preserving provider variants.
function repairSubtitles(db, repairInvalid) {
  const candidates = db.prepare(subtitleQuery).all(repairInvalid ? '1' : '0');
  for (const row of candidates) {
    let titles;
    try { titles = JSON.parse(row.alternate_titles); } catch (_) {}
    const valid = Array.isArray(titles) && titles.every(title => typeof title === 'string');
    if (valid && (titles.length || !row.subtitle)) continue;
    db.prepare('UPDATE source_entries SET alternate_titles=? WHERE id=?').run(JSON.stringify(row.subtitle ? [row.subtitle] : []), row.id);
  }
}
function runSql(db, sql) {
  const alter = sql.match(/^ALTER TABLE (\w+) ADD COLUMN (\w+)/i);
  if (alter && db.prepare(`PRAGMA table_info(${alter[1]})`).all().some(column => column.name === alter[2])) return;
  assert.equal(sql.includes('$'), false, 'unexpected Kotlin interpolation requires explicit test support');
  const parameterCount = (sql.match(/\?/g) || []).length;
  if (parameterCount) db.prepare(sql).run(...Array(parameterCount).fill('2026-10-03 12:00:00'));
  else db.exec(sql);
}
function migrate(db, stopAt = Infinity) {
  db.exec('BEGIN');
  try {
    migrationSql.forEach((sql, index) => {
      if (index === stopAt) throw Error('simulated interruption');
      runSql(db, sql);
      if (/INSERT OR IGNORE INTO source_entries\(/.test(sql)) repairSubtitles(db, false);
    });
    repairSubtitles(db, true);
    db.exec(`PRAGMA user_version=${currentVersion}; COMMIT`);
  } catch (error) { db.exec('ROLLBACK'); throw error; }
}
function fixture(size) {
  const db = new DatabaseSync(':memory:');
  db.exec(legacySchema + '\nPRAGMA user_version=1;');
  const insert = db.prepare('INSERT INTO entries VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)');
  const link = db.prepare('INSERT INTO entry_tags VALUES(?,?)');
  db.exec("INSERT INTO tags VALUES(1,'Adventure','tag','adventure',1,'https://nhentai.net/tag/adventure/'),(2,'Creator','artist','creator',0,'https://nhentai.net/artist/creator/');");
  db.exec('BEGIN');
  for (let index = 1; index <= size; index++) {
    const subtitle = index === 1 ? 'Alternate "title" \\ path\nsecond line\twith tab' : `Alternate ${index}`;
    insert.run(index, `Gallery ${index}`, subtitle, `https://nhentai.net/g/${index}/`, 31, '2026-01-01', index * 10, 'jpg', index % 6, index % 2, index % 2 ? '2026-01-02 12:00:00' : '', index % 3 === 0 ? 1 : 0, '2026-01-01 12:00:00', '2026-01-01 11:00:00');
    link.run(index, 1); link.run(index, 2);
  }
  if (size) {
    db.exec("INSERT INTO reading_sessions VALUES(1,'2026-01-02 12:00:00','2026-01-02 12:01:00','2026-01-02',1,31,60,5,0),(2,'2026-01-03 12:00:00','2026-01-03 12:02:00','2026-01-03',1,31,120,4,1);");
    db.exec("INSERT INTO subscriptions VALUES(1,'Creator','artist','artist|creator',1,1,1,'2026-01-01','2026-01-02'); INSERT INTO subscription_seen_codes VALUES(1,1,'2026-01-02'); INSERT INTO subscription_events VALUES(1,1,1,'Gallery 1','cover',31,'2026-01-01','url','2026-01-02',0,1);");
    db.exec("INSERT INTO daily_read_activity VALUES('2026-01-02',31,1); INSERT INTO popular_tags VALUES(1,'Adventure','tag','adventure',100,1); INSERT INTO entry_heatmap_cache VALUES(1,'old-layout','{}','2026-01-02');");
  }
  db.exec('COMMIT');
  return db;
}
function snapshot(db) {
  const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").all();
  return Object.fromEntries(tables.map(({name}) => [name, db.prepare(`SELECT * FROM ${name}`).all().map(row => Object.fromEntries(Object.entries(row)))]));
}
function checkMigration() {
for (const size of [0, 1, 2500]) {
  const db = fixture(size);
  const before = snapshot(db);
  assert.throws(() => migrate(db, Math.floor(migrationSql.length / 2)), /simulated interruption/);
  assert.deepEqual(snapshot(db), before, 'interruption rolls back schema and rows');
  assert.equal(db.prepare('PRAGMA user_version').get().user_version, 1);
  migrate(db);
  assert.deepEqual(db.prepare('SELECT id,name,kind,is_main FROM profiles ORDER BY is_main DESC').all().map(row => ({...row})), [
    {id: 'main', name: 'NHentai', kind: 'SOURCE_LOCKED', is_main: 1},
    {id: 'mangadex-default', name: 'MangaDex', kind: 'SOURCE_LOCKED', is_main: 0}
  ], 'fresh installs / 1.9 upgrades create two source-locked profiles, not a visible Main');
  assert.deepEqual(db.prepare('SELECT profile_id,source_id FROM profile_sources ORDER BY profile_id').all().map(row => ({...row})), [
    {profile_id: 'main', source_id: 'nhentai'},
    {profile_id: 'mangadex-default', source_id: 'mangadex'}
  ]);
  assert.equal(db.prepare('SELECT COUNT(*) AS n FROM source_entries').get().n, size);
  assert.equal(db.prepare('SELECT COUNT(*) AS n FROM profile_entries').get().n, size);
  assert.equal(db.prepare('SELECT active_profile_id FROM app_state').get().active_profile_id, 'main');
  assert.equal(db.prepare('PRAGMA integrity_check').get().integrity_check, 'ok');
  assert.deepEqual(db.prepare('PRAGMA foreign_key_check').all(), []);
  for (const entry of before.entries) {
    const restored = db.prepare("SELECT se.*,pe.rating,pe.read_state,pe.pinned,pe.added_at,pe.read_at FROM source_entries se JOIN profile_entries pe ON pe.source_entry_id=se.id AND pe.profile_id='main' WHERE se.source_id='nhentai' AND se.remote_id=?").get(String(entry.code));
    for (const field of ['title', 'rating', 'read_state', 'pinned', 'added_at', 'read_at']) assert.equal(restored[field], entry[field], field);
    assert.deepEqual(JSON.parse(restored.alternate_titles), [entry.subtitle], 'alternate titles stay valid and lossless');
    assert.equal(restored.canonical_url, entry.source_url);
    assert.equal(restored.unit_count, entry.num_pages);
    assert.equal(restored.thumbnail_url, `https://t.nhentai.net/galleries/${entry.media_id}/cover.${entry.cover_ext}`);
    assert.equal(restored.fetched_at, entry.fetched_at);
  }
  for (const [table, rows] of Object.entries(before)) {
    const after = db.prepare(`SELECT * FROM ${table}`).all().map(row => Object.fromEntries(Object.keys(rows[0] || {}).map(key => [key, key === 'route_key' ? row[key].replace(/^main\|nhentai\|/, '') : row[key]])));
    assert.deepEqual(after, rows, `legacy ${table} values stay intact`);
  }
  assert.equal(db.prepare('SELECT COUNT(*) AS n FROM source_entry_tags').get().n, size * 2);
  assert.equal(db.prepare('SELECT COUNT(*) AS n FROM source_entry_creators').get().n, size);
  assert.equal(db.prepare("SELECT COUNT(*) AS n FROM profile_entries WHERE profile_id='mangadex-default'").get().n, 0, 'old data belongs exclusively to NHentai');
  assert.equal(db.prepare("SELECT COUNT(*) AS n FROM reading_sessions WHERE profile_id<>'main' OR source_id<>'nhentai'").get().n, 0);
  assert.equal(db.prepare("SELECT COUNT(*) AS n FROM subscriptions WHERE profile_id<>'main' OR source_id<>'nhentai'").get().n, 0);
  const once = snapshot(db);
  migrate(db);
  assert.deepEqual(snapshot(db), once, 'repeated schema setup is idempotent');
  db.close();
  console.log(`PASS: production migration SQL, ${size} entries, interruption/rollback/retry and repeated startup`);
}

const repair = fixture(1);
migrate(repair);
repair.exec(`UPDATE source_entries SET alternate_titles='[broken';`);
repairSubtitles(repair, true);
assert.deepEqual(JSON.parse(repair.prepare('SELECT alternate_titles FROM source_entries').get().alternate_titles), ['Alternate "title" \\ path\nsecond line\twith tab']);
repair.exec(`UPDATE source_entries SET alternate_titles='["Original","Fetched variant"]';`);
repairSubtitles(repair, true);
assert.deepEqual(JSON.parse(repair.prepare('SELECT alternate_titles FROM source_entries').get().alternate_titles), ['Original', 'Fetched variant']);
repair.close();
console.log('PASS: schema 8 subtitle repair preserves valid fetched title variants');

// Existing 2.0 debug/user-created profiles are neither renamed nor rebuilt on startup.
// Deleting a default MangaDex profile must not make it reappear at the next launch.
const development = fixture(1);
migrate(development);
development.exec("DELETE FROM profiles WHERE id='mangadex-default'; UPDATE profiles SET name='My combined profile',kind='COMBINED' WHERE id='main'; INSERT INTO profile_sources VALUES('main','mangadex'); UPDATE app_state SET active_profile_id='main';");
const developmentBefore = snapshot(development);
migrate(development);
assert.deepEqual(snapshot(development), developmentBefore, 'existing development profiles and memberships remain unchanged');
development.close();
console.log('PASS: two locked defaults with old data in NHentai; existing 2.0 profiles and deleted defaults are preserved on restart');
}
module.exports = {fixture, migrate, snapshot};
if (require.main === module) checkMigration();
