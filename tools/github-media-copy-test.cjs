// Disposable SQLite reproduction of the media-mode REPLACE/RESTRICT crash.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {empty, restore, payload} = require('./source-backup-test.cjs');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/core/diagnostics/GitHubMediaSession.kt'), 'utf8');
assert(source.includes('DatabaseCopyOrder.parentFirst(parents)'));
assert(source.includes('copyOrder.asReversed().forEach'));
assert(source.includes('INSERT INTO main.'));
assert(!source.includes('INSERT OR REPLACE INTO main.'));
assert(!source.includes('DELETE FROM production.'));
const db = empty(); restore(db, payload);
const tables = db.prepare("SELECT name,sql FROM main.sqlite_master WHERE type='table' AND name<>'android_metadata' AND name NOT LIKE 'sqlite_%' ORDER BY rowid").all();
const contents = schema => Object.fromEntries(tables.map(({name}) => [name, db.prepare(`SELECT * FROM ${schema}."${name}" ORDER BY rowid`).all()]));
const original = contents('main');
db.exec("ATTACH DATABASE ':memory:' AS media");
for (const {name, sql} of tables) db.exec(sql.replace(/^CREATE TABLE\s+(?:IF NOT EXISTS\s+)?(?:"[^"]+"|\w+)/i, `CREATE TABLE media."${name}"`));
db.exec("INSERT INTO media.sources(id,display_name) VALUES('nhentai','NHentai'); INSERT INTO media.profiles(id,name) VALUES('main','Main'); INSERT INTO media.profile_sources(profile_id,source_id) VALUES('main','nhentai')");
assert.throws(() => db.exec("INSERT OR REPLACE INTO media.sources SELECT * FROM main.sources"), /FOREIGN KEY/, 'old copy crashes before the UI opens');
const parents = new Map(tables.map(({name}) => [name, db.prepare(`PRAGMA media.foreign_key_list("${name}")`).all().map(row => row.table)]));
const pending = new Set(parents.keys()), order = [];
while (pending.size) {
  const next = [...pending].find(name => parents.get(name).every(parent => parent === name || !pending.has(parent)));
  assert(next, 'acyclic production schema'); order.push(next); pending.delete(next);
}
function copy(failAt = Infinity) {
  db.exec('BEGIN');
  try {
    [...order].reverse().forEach(name => db.exec(`DELETE FROM media."${name}"`));
    order.forEach((name, i) => {if (i === failAt) throw Error('interrupted copy'); db.exec(`INSERT INTO media."${name}" SELECT * FROM main."${name}"`);});
    assert.deepEqual(db.prepare('PRAGMA media.foreign_key_check').all(), []);
    db.exec('COMMIT');
  } catch (error) {db.exec('ROLLBACK'); throw error;}
}
copy(); assert.deepEqual(contents('media'), original, 'all copied tables retain every row');
copy(); assert.deepEqual(contents('media'), original, 'repeat copy is idempotent');
assert.throws(() => copy(3), /interrupted/);
assert.deepEqual(contents('media'), original, 'failed copy restores previous media rows');
assert.deepEqual(contents('main'), original, 'production rows remain unchanged');
db.close();
console.log('PASS: GitHub media REPLACE/RESTRICT crash reproduced; parent-first copy, rollback, idempotence and read-only production verified in isolated SQLite');
