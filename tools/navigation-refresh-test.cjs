// Two real SQLite connections in a temporary fixture; never reads phone/app data.
const {DatabaseSync} = require('node:sqlite');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const assert = require('node:assert/strict');
const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'sauce-navigation-'));
const filename = path.join(directory, 'navigation.db');
let dashboard, reader;
try {
  dashboard = new DatabaseSync(filename);
  dashboard.exec('CREATE TABLE library(id INTEGER PRIMARY KEY, rating INTEGER); INSERT INTO library VALUES(1,0)');
  reader = new DatabaseSync(filename);
  const token = () => [dashboard.prepare('PRAGMA data_version').get().data_version,
    Object.values(dashboard.prepare('SELECT total_changes()').get())[0]];
  const before = token();
  reader.prepare('SELECT * FROM library').all();
  assert.deepEqual(token(), before, 'opening/reading another screen does not invalidate dashboard');
  reader.exec('UPDATE library SET rating=5 WHERE id=1');
  const external = token();
  assert.notEqual(external[0], before[0], 'Reader/Browser/Bridge write detected');
  assert.equal(external[1], before[1], 'external write not counted as dashboard local change');
  dashboard.exec('UPDATE library SET rating=4 WHERE id=1');
  const own = token();
  assert.equal(own[0], external[0], 'data_version alone would miss own background writes');
  assert.notEqual(own[1], external[1], 'total_changes detects same-helper background write');
  reader.exec('BEGIN; UPDATE library SET rating=1; ROLLBACK');
  assert.deepEqual(token(), own, 'rolled back other-helper write does not discard valid UI');
  const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/data/database/SauceTrackerDatabase.kt'), 'utf8');
  assert.ok(source.includes('rawQuery("PRAGMA data_version", null)'));
  assert.ok(source.includes('rawQuery("SELECT total_changes()", null)'));
  assert.ok(!/enableWriteAheadLogging\(|setWriteAheadLoggingEnabled\(true\)/.test(source),
    'navigation tokens require the documented single-connection helper; revise if WAL is enabled');
  console.log('PASS: unchanged navigation, external/local writes and rollback detection (isolated SQLite).');
} finally {
  reader?.close(); dashboard?.close();
  // Exact mkdtemp directory above, containing only this synthetic fixture.
  assert.equal(path.dirname(path.resolve(directory)), path.resolve(os.tmpdir()));
  fs.rmSync(directory, {recursive: true, force: true});
}
