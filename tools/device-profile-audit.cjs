// Read-only device audit of the QA profile created in GitHub media mode.
// Stops debug (never stable) before reading SQLite/WAL bytes for a coherent copy.
// Private replicas exist only in a unique ignored build directory and are removed.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const {spawnSync} = require('node:child_process');
const {DatabaseSync} = require('node:sqlite');
const serial = process.argv[2];
if (!serial || !/^[A-Za-z0-9_.:-]+$/.test(serial)) throw Error('Specify the full authorized adb serial');
const adb = 'C:\\Users\\roinu\\stock-tracker\\android-app\\android-sdk\\platform-tools\\adb.exe';
const pkg = 'com.roinur.saucetracker.rewrite';
function run(args, optional = false) {
  const result = spawnSync(adb, ['-s', serial, ...args], {maxBuffer: 256 * 1024 * 1024});
  if (result.error) throw result.error;
  if (result.status !== 0 && !optional) throw Error(`ADB audit failed: ${result.stderr.toString()}`);
  return result.status === 0 ? result.stdout : null;
}
const foreground = run(['shell','dumpsys','activity','activities']).toString();
assert(/topResumedActivity=.*com\.roinur\.saucetracker\.rewrite\//.test(foreground));
assert(foreground.includes('com.roinur.saucetracker.app.GitHubMediaLauncher'), 'Audit requires an active GitHub media task');
run(['shell','am','force-stop',pkg]);
const outputRoot = path.resolve(__dirname, '../build/device-qa');
fs.mkdirSync(outputRoot, {recursive: true});
const directory = fs.mkdtempSync(path.join(outputRoot, 'profile-audit-'));
assert(path.dirname(directory) === outputRoot);
const created = [], opened = [];
try {
  function readDatabase(name) {
    for (const suffix of ['', '-wal', '-shm']) {
      const bytes = run(['exec-out','run-as',pkg,'cat',`databases/${name}${suffix}`], suffix !== '');
      if (bytes) {const file = path.join(directory, name + suffix); fs.writeFileSync(file, bytes); created.push(file);}
    }
    const db = new DatabaseSync(path.join(directory, name)); opened.push(db); return db;
  }
  const original = readDatabase('tagbook.db'), media = readDatabase('sauce_tracker_github_media.db');
  const qa = media.prepare("SELECT id FROM profiles WHERE name='QA-Device-2.0'").get();
  assert(qa, 'QA profile must exist in media');
  assert.equal(original.prepare("SELECT COUNT(*) n FROM profiles WHERE name='QA-Device-2.0'").get().n, 0, 'QA profile must not exist in production');
  const sortedRows = rows => rows.map(row => JSON.stringify(Object.fromEntries(Object.entries(row).sort(([a],[b]) => a.localeCompare(b))))).sort();
  for (const table of ['entries','tags','entry_tags','sources','source_entries','source_entry_tags','source_entry_creators']) {
    assert.deepEqual(sortedRows(media.prepare(`SELECT * FROM ${table}`).all()), sortedRows(original.prepare(`SELECT * FROM ${table}`).all()), `unchanged shared ${table}`);
  }
  const memberships = media.prepare('SELECT * FROM profile_entries WHERE profile_id=?').all(qa.id);
  assert.equal(memberships.length, 1, 'one entry was copied');
  const entry = media.prepare('SELECT source_id,remote_id FROM source_entries WHERE id=?').get(memberships[0].source_entry_id);
  const sourceProfile = original.prepare('SELECT active_profile_id FROM app_state WHERE slot_id=1').get().active_profile_id;
  const status = original.prepare('SELECT * FROM profile_entries WHERE profile_id=? AND source_entry_id=?').get(sourceProfile, memberships[0].source_entry_id);
  const without = (rows, excluded) => sortedRows(rows.map(row => Object.fromEntries(Object.entries(row).filter(([k]) => !excluded.includes(k)))));
  assert.deepEqual(without(memberships, ['profile_id']), without([status], ['profile_id']), 'copied personal state');
  let resume = 0, chapters = 0, sessions = 0;
  for (const table of ['source_reader_progress','source_chapter_progress','reading_sessions']) {
    const query = `SELECT * FROM ${table} WHERE profile_id=? AND source_id=? AND remote_id=?`;
    const from = original.prepare(query).all(sourceProfile, entry.source_id, entry.remote_id);
    const to = media.prepare(query).all(qa.id, entry.source_id, entry.remote_id);
    assert.deepEqual(without(to, ['id','profile_id']), without(from, ['id','profile_id']), `copied ${table}`);
    if (table === 'source_reader_progress') resume = to.length;
    if (table === 'source_chapter_progress') chapters = to.length;
    if (table === 'reading_sessions') sessions = to.length;
  }
  assert(resume > 0 && chapters > 0 && sessions > 0, 'chosen entry must exercise all progress/history paths');
  assert.deepEqual(media.prepare('PRAGMA foreign_key_check').all(), []);
  console.log(`PASS: on-device media profile copy preserves rating/read/pin, ${resume} resume position(s), ${chapters} chapter progress row(s), ${sessions} session(s); shared metadata matches production and no QA profile exists in production`);
} finally {
  opened.forEach(db => db.close());
  // SQLite may create its own sidecars in this uniquely generated directory.
  for (const file of fs.readdirSync(directory)) fs.unlinkSync(path.join(directory, file));
  fs.rmdirSync(directory);
}
