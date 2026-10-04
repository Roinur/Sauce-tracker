const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {DatabaseSync} = require('node:sqlite');
const root = path.resolve(__dirname, '../app/src/main/java/com/roinur/saucetracker');
const health = fs.readFileSync(path.join(root, 'feature/settings/LibraryHealth.kt'), 'utf8');
const query = variable => health.match(new RegExp(`val ${variable} = count\\("([^"\\n]+)"\\)`))[1];
const db = new DatabaseSync(':memory:');
try {
  db.exec(`CREATE TABLE profiles(id TEXT PRIMARY KEY); CREATE TABLE sources(id TEXT PRIMARY KEY);
    CREATE TABLE source_entries(id INTEGER PRIMARY KEY, source_id TEXT, remote_id TEXT);
    CREATE TABLE reading_sessions(profile_id TEXT,source_id TEXT,remote_id TEXT,rating INTEGER DEFAULT 0);
    INSERT INTO profiles VALUES('main'); INSERT INTO sources VALUES('nhentai'),('mangadex');`);
  const insert = db.prepare('INSERT INTO reading_sessions(profile_id,source_id,remote_id) VALUES(?,?,?)');
  insert.run('main','nhentai','123'); insert.run('main','mangadex','browser-only@en');
  assert.equal(Object.values(db.prepare(query('missingSessionEntries')).get())[0], 2);
  assert.equal(Object.values(db.prepare(query('invalidSessionOwners')).get())[0], 0, 'browser/removed entries retain valid reading history');
  assert.equal(Object.values(db.prepare(query('unratedMissingSessions')).get())[0], 2, 'unrated reads outside the library are classified without deleting history');
  db.prepare('UPDATE reading_sessions SET rating=4 WHERE remote_id=?').run('123');
  assert.equal(Object.values(db.prepare(query('unratedMissingSessions')).get())[0], 1, 'rated removed entries are not falsely labeled unrated');
  insert.run('missing','nhentai','456'); insert.run('main','missing','789'); insert.run('main','nhentai','');
  assert.equal(Object.values(db.prepare(query('invalidSessionOwners')).get())[0], 3);
  assert.equal(db.prepare('SELECT COUNT(*) n FROM reading_sessions').get().n, 5, 'health scan is read-only');
} finally { db.close(); }
const rolling = fs.readFileSync(path.join(root, 'data/backup/RollingBackupStore.kt'), 'utf8');
const read = rolling.split('internal fun readCurrentProceduralBackupTextOrNull')[1].split('internal fun writeRollingProceduralBackup')[0];
assert(read.includes('resolveExistingBackupContainerUri'));
assert(!read.includes('resolveOrCreate'), 'backup diagnostics cannot create a folder or rewrite .nomedia');
const database = fs.readFileSync(path.join(root,'data/database/SauceTrackerDatabase.kt'),'utf8').split('fun importSnapshot(')[1];
assert(database.indexOf('BackupHistoryValidation.validateOwners') < database.indexOf('db.beginTransaction()'), 'incomplete multi-source imports fail before writes');
console.log('PASS: preserved browser history, invalid owners, read-only backup lookup and import preflight placement');
