// Portable source rows tested with the production column contract and SQLite schema.
// This deliberately does not claim to execute Android's ContentValues/import UI.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {fixture, migrate} = require('./migration-roundtrip-test.cjs');
const root = path.resolve(__dirname, '..');
const contract = fs.readFileSync(path.join(root, 'app/src/main/java/com/roinur/saucetracker/data/backup/SourcePlatformBackup.kt'), 'utf8');
const databaseSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/roinur/saucetracker/data/database/SauceTrackerDatabase.kt'), 'utf8');
const columnsBlock = contract.slice(contract.indexOf('val columns ='), contract.indexOf('val deleteOrder ='));
const definitions = Object.fromEntries([...columnsBlock.matchAll(/"(\w+)" to listOf\(([^)]+)\)/g)].map(m => [m[1], [...m[2].matchAll(/"(\w+)"/g)].map(x => x[1])]));
const deleteOrder = [...contract.match(/val deleteOrder = listOf\(([^)]+)\)/)[1].matchAll(/"(\w+)"/g)].map(x => x[1]);
const payload = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures/mixed-v2-source-platform.json'), 'utf8'));
assert.equal(Object.keys(definitions).length, 13);
assert(databaseSource.includes('val tableColumns = SourcePlatformBackup.columns'));
assert(databaseSource.includes('val definitions = SourcePlatformBackup.columns'));
assert(databaseSource.includes('sourcePlatform?.let { SourcePlatformBackup.validate(it.toString()) }'));
const canonical = value => JSON.stringify(Object.fromEntries(Object.entries(value).map(([key, rows]) => [key, Array.isArray(rows) ? rows.map(row => JSON.stringify(row)).sort() : rows])));
function empty() {const db = fixture(0); migrate(db); return db;}
function restore(db, data, failAfter = Infinity) {
  db.exec('BEGIN');
  try {
    deleteOrder.forEach(table => db.exec(`DELETE FROM ${table}`));
    let count = 0;
    for (const [table, columns] of Object.entries(definitions)) {
      const insert = db.prepare(`INSERT INTO ${table}(${columns.join(',')}) VALUES(${columns.map(() => '?').join(',')})`);
      for (const row of data[table] || []) {
        if (count++ === failAfter) throw Error('interrupted restore');
        insert.run(...columns.map(c => c === 'completed_at' && !(c in row) ? '' : row[c] ?? null));
      }
    }
    db.exec('COMMIT');
  } catch (e) {db.exec('ROLLBACK'); throw e;}
}
function exported(db) {
  return {schema_version: 5, ...Object.fromEntries(Object.entries(definitions).map(([table, columns]) => [table, db.prepare(`SELECT ${columns.join(',')} FROM ${table}`).all().map(row => Object.fromEntries(Object.entries(row)))]))};
}
const db = empty();
for (const [table, columns] of Object.entries(definitions)) {
  const schema = db.prepare(`PRAGMA table_info(${table})`).all().map(x => x.name);
  assert.deepEqual([...columns].sort(), [...schema].sort(), `all ${table} columns must be backed up`);
}
restore(db, payload); const first = exported(db);
assert.equal(canonical(first), canonical(payload), 'mixed providers, language variants, profile status, preferences, covers, counts and progress round trip');
restore(db, JSON.parse(JSON.stringify(first))); assert.equal(canonical(exported(db)), canonical(first), 'second restore is idempotent');
assert.throws(() => restore(db, payload, 5), /interrupted restore/);
assert.equal(canonical(exported(db)), canonical(first), 'failed restore rolls back existing data');
assert.deepEqual(db.prepare('PRAGMA foreign_key_check').all(), []);
assert.equal(db.prepare('PRAGMA integrity_check').get().integrity_check, 'ok');
const earlier = structuredClone(payload); earlier.source_chapter_progress.forEach(row => delete row.completed_at);
restore(db, earlier); assert.equal(db.prepare("SELECT completed_at FROM source_chapter_progress WHERE completed=1").get().completed_at, '');
db.close();
module.exports = {empty, restore, exported, payload, canonical};
console.log('PASS: production source backup columns match all persisted fields; mixed-language/profile source rows round trip twice; interrupted restore rolls back; earlier V5 completion timestamps remain compatible');
