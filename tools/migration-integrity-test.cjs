// Execute production migration-integrity SQL against isolated synthetic 1.9 data.
// No phone, production database or user backup is opened.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {DatabaseSync} = require('node:sqlite');

const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/data/database/SauceTrackerDatabase.kt'), 'utf8');
// SQL-only tests cannot execute Kotlin constructor ordering. Guard the runtime
// first-open path too: onUpgrade needs this list before the init block opens DB.
assert.ok(source.indexOf('private val preMigrationTables') < source.indexOf('    init {'),
  'Pre-migration snapshot tables must be initialized before the database is opened');
function validationSql(name) {
  const match = source.match(new RegExp(`val ${name} = db\\.rawQuery\\("""([\\s\\S]*?)"""`));
  assert.ok(match, `production query ${name} exists`);
  return match[1];
}

const checks = [
  'stateMismatchCount', 'missingTagCount', 'missingCreatorCount',
  'sessionMismatchCount', 'subscriptionMismatchCount', 'eventMismatchCount'
];
const db = new DatabaseSync(':memory:');
db.exec(`
  CREATE TABLE entries(code INTEGER,rating INTEGER,read_state INTEGER,pinned INTEGER,added_at TEXT,read_at TEXT);
  CREATE TABLE source_entries(id INTEGER,source_id TEXT,remote_id TEXT);
  CREATE TABLE profile_entries(source_entry_id INTEGER,profile_id TEXT,rating INTEGER,read_state INTEGER,pinned INTEGER,added_at TEXT,read_at TEXT);
  CREATE TABLE tags(id INTEGER,name TEXT,type TEXT,normalized_name TEXT);
  CREATE TABLE entry_tags(entry_code INTEGER,tag_id INTEGER);
  CREATE TABLE source_entry_tags(source_entry_id INTEGER,type TEXT,normalized_name TEXT);
  CREATE TABLE source_entry_creators(source_entry_id INTEGER,type TEXT,normalized_name TEXT);
  CREATE TABLE reading_sessions(entry_code INTEGER,profile_id TEXT,source_id TEXT,remote_id TEXT);
  CREATE TABLE subscriptions(profile_id TEXT,source_id TEXT,route_key TEXT);
  CREATE TABLE subscription_events(code INTEGER,source_id TEXT,remote_id TEXT);
  INSERT INTO entries VALUES(123,5,1,1,'2026-01-01','2026-02-01');
  INSERT INTO source_entries VALUES(1,'nhentai','123'),(2,'mangadex','123');
  INSERT INTO profile_entries VALUES(1,'main',5,1,1,'2026-01-01','2026-02-01'),(2,'other',1,0,0,'','');
  INSERT INTO tags VALUES(1,'Adventure','tag','adventure'),(2,'Creator','artist','creator');
  INSERT INTO entry_tags VALUES(123,1),(123,2);
  INSERT INTO source_entry_tags VALUES(1,'tag','adventure'),(1,'artist','creator');
  INSERT INTO source_entry_creators VALUES(1,'artist','creator');
  INSERT INTO reading_sessions VALUES(123,'main','nhentai','123');
  INSERT INTO subscriptions VALUES('main','nhentai','main|nhentai|artist|creator');
  INSERT INTO subscription_events VALUES(123,'nhentai','123');
`);

function mismatch(name) { return Object.values(db.prepare(validationSql(name)).get())[0]; }
for (const check of checks) assert.equal(mismatch(check), 0, `${check} clean fixture`);
function rejectChange(check, sql) {
  db.exec('BEGIN');
  db.exec(sql);
  assert.ok(mismatch(check) > 0, `${check} detects loss`);
  db.exec('ROLLBACK');
  assert.equal(mismatch(check), 0, `${check} rollback`);
}
for (const field of ['rating','read_state','pinned','added_at','read_at']) {
  rejectChange('stateMismatchCount', `UPDATE profile_entries SET ${field}=${field.endsWith('_at') ? "'changed'" : '0'} WHERE profile_id='main'`);
}
rejectChange('stateMismatchCount', "DELETE FROM profile_entries WHERE profile_id='main'");
rejectChange('missingTagCount', "DELETE FROM source_entry_tags WHERE type='tag'");
rejectChange('missingCreatorCount', 'DELETE FROM source_entry_creators');
rejectChange('sessionMismatchCount', "UPDATE reading_sessions SET remote_id='wrong'");
rejectChange('subscriptionMismatchCount', "UPDATE subscriptions SET profile_id='other'");
rejectChange('eventMismatchCount', "UPDATE subscription_events SET remote_id='wrong'");
db.close();
console.log('PASS: production migration SQL rejects lost entry state, tags, creators, sessions, subscriptions and events; source collisions stay separate');
