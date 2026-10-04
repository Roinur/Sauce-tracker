// Exercise production profile progress SQL on the disposable mixed-provider fixture.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {empty, restore, exported, payload, canonical} = require('./source-backup-test.cjs');
const databaseSource = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/data/database/SauceTrackerDatabase.kt'), 'utf8');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/data/profile/ProfileStore.kt'), 'utf8');
const start = source.indexOf('private fun copyReaderProgress('), end = source.indexOf('private fun now()', start);
const sql = [...source.slice(start, end).matchAll(/db\.execSQL\("""([\s\S]*?)"""\.trimIndent\(\)/g)].map(x => x[1]);
assert.equal(sql.length, 2); assert(sql.every(x => !x.includes('DO UPDATE')), 'API 26 SQLite cannot use modern UPSERT');
const db = empty(); restore(db, payload);
const key = payload.source_entries[1].remote_id;
function copy(from, to) {
  db.prepare(sql[0]).run(to, from, 'mangadex', key);
  db.prepare(sql[1]).run(to, to, from, 'mangadex', key);
}
db.exec('BEGIN'); copy('main', 'manga'); db.exec('COMMIT');
assert.equal(db.prepare("SELECT page_index FROM source_reader_progress WHERE profile_id='manga'").get().page_index, 39, 'existing target resume position stays');
assert.equal(db.prepare("SELECT furthest_page_index FROM source_chapter_progress WHERE profile_id='manga' AND chapter_id='chapter-one'").get().furthest_page_index, 15);
db.exec("UPDATE source_chapter_progress SET furthest_page_index=30 WHERE profile_id='manga' AND chapter_id='chapter-one'");
copy('main', 'manga');
assert.equal(db.prepare("SELECT furthest_page_index FROM source_chapter_progress WHERE profile_id='manga' AND chapter_id='chapter-one'").get().furthest_page_index, 30, 'partial progress never regresses');
copy('manga', 'main');
assert.equal(db.prepare("SELECT completed FROM source_chapter_progress WHERE profile_id='main' AND chapter_id='chapter-two'").get().completed, 1);
const once = canonical(exported(db)); copy('manga', 'main'); assert.equal(canonical(exported(db)), once);
// Confirm deletion of a non-active profile removes its legacy dependents but not shared rows.
db.exec("INSERT INTO reading_sessions(started_at,ended_at,day_key,entry_code,profile_id,source_id,remote_id,session_key) VALUES('','','2026-10-03',0,'main','mangadex','series','one'),('','','2026-10-03',0,'manga','mangadex','series','two');");
db.exec("INSERT INTO subscriptions(id,route_name,route_type,route_key,profile_id,source_id) VALUES(100,'English','language','main|mangadex|language|en','main','mangadex'),(101,'English','language','manga|mangadex|language|en','manga','mangadex'); INSERT INTO subscription_seen_codes(subscription_id,code,seen_at) VALUES(100,0,''),(101,0,'');");
// Selecting one backup profile must not clear the currently active, different profile.
assert(databaseSource.includes('val restoreProfileId = RestoreProfileRouting.targetProfileId(sourcePlatform != null, sourcePlatformProfileId, targetProfileId)'));
assert.equal((databaseSource.match(/defaultProfileId = restoreProfileId,/g) || []).length, 2);
db.exec('BEGIN');
for (const name of ['replaceSubscriptionsFromSnapshot', 'replaceReadingSessionsFromSnapshot']) {
  const method = databaseSource.slice(databaseSource.indexOf(`private fun ${name}(`));
  const deletion = method.match(/else db\.delete\("(\w+)", "([^"]+)", arrayOf\(defaultProfileId\)\)/);
  assert(deletion, name); db.prepare(`DELETE FROM ${deletion[1]} WHERE ${deletion[2]}`).run('manga');
}
assert.equal(db.prepare("SELECT COUNT(*) AS n FROM reading_sessions WHERE profile_id='main'").get().n, 1);
assert.equal(db.prepare("SELECT COUNT(*) AS n FROM subscriptions WHERE profile_id='main'").get().n, 1);
db.exec('ROLLBACK');
const deletion = source.slice(source.indexOf('fun deleteProfile('), source.indexOf('private data class RawState'));
const clauses = [...deletion.matchAll(/db\.delete\("(\w+)", "([^"]+)", arrayOf\(profileId\)\)/g)];
assert.deepEqual(clauses.map(x => x[1]), ['reading_sessions','subscriptions','profiles']);
db.exec('BEGIN'); clauses.forEach(x => db.prepare(`DELETE FROM ${x[1]} WHERE ${x[2]}`).run('main')); db.exec('ROLLBACK');
assert.equal(db.prepare('SELECT COUNT(*) AS n FROM profiles').get().n, 2, 'deletion rollback retains data');
// Use non-main profile for the actual cleanup SQL; keep main as active, as the real guard requires.
db.exec("UPDATE app_state SET active_profile_id='main'");
db.exec('BEGIN'); clauses.forEach(x => db.prepare(`DELETE FROM ${x[1]} WHERE ${x[2]}`).run('manga')); db.exec('COMMIT');
for (const table of ['reading_sessions','subscriptions','source_reader_progress','source_chapter_progress','profile_entries']) assert.equal(db.prepare(`SELECT COUNT(*) AS n FROM ${table} WHERE profile_id='manga'`).get().n, 0, table);
assert.equal(db.prepare('SELECT COUNT(*) AS n FROM source_entries').get().n, 3, 'shared metadata stays');
assert.equal(db.prepare('SELECT COUNT(*) AS n FROM subscription_seen_codes').get().n, 1, 'other profile subscription relations stay');
assert.deepEqual(db.prepare('PRAGMA foreign_key_check').all(), []);
db.close();
console.log('PASS: production profile copy/merge retains resume, partial/completed chapter progress and idempotence; transactional deletion cleans only the selected profile and preserves shared data');
