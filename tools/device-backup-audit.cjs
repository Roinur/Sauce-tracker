// Private, read-only diagnosis: only debug is stopped; existing backups are never written.
const fs = require('node:fs'), path = require('node:path');
const {spawnSync} = require('node:child_process');
const {DatabaseSync} = require('node:sqlite');
const {createHash} = require('node:crypto');
const serial = process.argv[2];
if (!serial || !/^[A-Za-z0-9_.:-]+$/.test(serial)) throw Error('Specify an authorized adb serial');
const adb = 'C:/Users/roinu/stock-tracker/android-app/android-sdk/platform-tools/adb.exe';
const pkg = 'com.roinur.saucetracker.rewrite';
function run(args, optional = false) {
  const result = spawnSync(adb, ['-s', serial, ...args], {maxBuffer: 256 * 1024 * 1024});
  if (result.error) throw result.error;
  if (result.status !== 0 && !optional) throw Error('Read-only ADB audit failed');
  return result.status === 0 ? result.stdout : null;
}
const prefs = run(['exec-out','run-as',pkg,'cat','shared_prefs/nhtagbook_prefs.xml']).toString();
const uri = prefs.match(/<string name="auto_backup_tree_uri">(.*?)<\/string>/)?.[1];
if (!uri?.startsWith('content://com.android.externalstorage.documents/tree/primary%3A')) throw Error('Unsupported backup location; do not guess its path');
const folder = '/sdcard/' + decodeURIComponent(uri.split('/tree/')[1]).slice('primary:'.length);
if (!folder.startsWith('/sdcard/') || folder.split('/').includes('..') || /["\n\r]/.test(folder)) throw Error('Invalid backup path');
run(['shell','am','force-stop',pkg]);
const outputRoot = path.resolve(__dirname, '../build/device-qa');
fs.mkdirSync(outputRoot, {recursive: true});
const directory = fs.mkdtempSync(path.join(outputRoot, 'backup-audit-'));
let db;
try {
  for (const suffix of ['', '-wal', '-shm']) {
    const bytes = run(['exec-out','run-as',pkg,'cat',`databases/tagbook.db${suffix}`], suffix !== '');
    if (bytes) fs.writeFileSync(path.join(directory, 'tagbook.db' + suffix), bytes);
  }
  db = new DatabaseSync(path.join(directory, 'tagbook.db'));
  const missing = db.prepare(`SELECT rs.* FROM reading_sessions rs LEFT JOIN source_entries se
    ON se.source_id=rs.source_id AND se.remote_id=rs.remote_id WHERE se.id IS NULL`).all();
  const groups = {};
  for (const row of missing) {
    const legacy = db.prepare('SELECT 1 FROM entries WHERE code=?').get(row.entry_code);
    const alternate = db.prepare("SELECT COUNT(*) n FROM source_entries WHERE source_id=? AND (remote_id=? OR remote_id LIKE ?)").get(row.source_id, row.remote_id.split('@')[0], row.remote_id.split('@')[0] + '@%').n;
    const profile = db.prepare('SELECT 1 FROM profiles WHERE id=?').get(row.profile_id);
    const category = JSON.stringify({source: row.source_id, legacyEntry: !!legacy, alternateIdentity: alternate > 0, profileExists: !!profile, blankRemote: !row.remote_id, legacyKey: row.session_key.startsWith('legacy:')});
    groups[category] = (groups[category] || 0) + 1;
  }
  console.log(JSON.stringify({missingSessions: missing.length, groups, integrity: db.prepare('PRAGMA quick_check').get()}, null, 2));
  for (const name of ['procedural_backup.txt', 'procedural_backup_previous_1.txt']) {
    const bytes = run(['exec-out',`cat "${folder}/${name}"`], true);
    if (!bytes) {console.log(name + ': unreadable'); continue;}
    const raw = bytes.toString();
    const backup = JSON.parse(raw.slice(raw.indexOf('{')));
    const canonical = rows => JSON.stringify(rows.map(row => JSON.stringify(Object.fromEntries(Object.entries(row).sort(([a], [b]) => a.localeCompare(b))))).sort());
    const comparedTables = {};
    const historyTables = {};
    if (backup.source_platform) {
      const contract = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/data/backup/SourcePlatformBackup.kt'), 'utf8').split('val deleteOrder')[0];
      for (const match of contract.matchAll(/"([a-z_]+)" to listOf\(([^\n]+)\)/g)) {
        const table = match[1], columns = [...match[2].matchAll(/"([a-z_]+)"/g)].map(m => m[1]);
        if (!Array.isArray(backup.source_platform[table])) continue;
        comparedTables[table] = canonical(db.prepare(`SELECT ${columns.join(',')} FROM ${table}`).all()) === canonical(backup.source_platform[table]);
      }
    }
    const arrays = {};
    for (const key of ['entries','reading_sessions','subscriptions','subscription_seen_codes','subscription_events']) {
      const rows = backup[key] || [];
      arrays[key] = {count: rows.length, fieldShapes: [...new Set(rows.map(row => Object.keys(row).sort().join(',')))], mangaDex: rows.filter(row => row.source_id === 'mangadex').length};
    }
    if (backup.source_platform) {
      for (const table of ['reading_sessions', 'subscriptions', 'subscription_seen_codes', 'subscription_events']) {
        const rows = backup[table] || [];
        if (!rows.length) {historyTables[table] = db.prepare(`SELECT COUNT(*) n FROM ${table}`).get().n === 0; continue;}
        const columns = Object.keys(rows[0]);
        if (!columns.every(column => /^[a-z_]+$/.test(column))) throw Error('Invalid export column');
        let sql;
        if (table === 'reading_sessions' || table === 'subscriptions') sql = `SELECT ${columns.join(',')} FROM ${table}`;
        else sql = `SELECT ${columns.map(column => `${['route_name','route_type','profile_id','source_id'].includes(column) ? 's' : 'r'}.${column} AS ${column}`).join(',')}
          FROM ${table} r JOIN subscriptions s ON s.id=r.subscription_id`;
        historyTables[table] = canonical(db.prepare(sql).all()) === canonical(rows);
      }
    }
    const identities = new Set((backup.source_platform?.source_entries || []).map(row => row.source_id + ':' + row.remote_id));
    const after = run(['exec-out',`cat "${folder}/${name}"`]);
    const unchanged = createHash('sha256').update(bytes).digest('hex') === createHash('sha256').update(after).digest('hex');
    if (!unchanged) throw Error('Backup changed externally during read-only audit; retry with debug stopped');
    console.log(JSON.stringify({file: name, unchangedDuringAudit: unchanged, version: backup.version, format: backup.format, exportedAt: backup.exported_at, sourceSchema: backup.source_platform?.schema_version, sourceEntries: identities.size, sourceTablesMatchDevice: comparedTables, historyTablesMatchDevice: historyTables, orphanSourceSessions: (backup.reading_sessions || []).filter(row => row.source_id && !identities.has(row.source_id + ':' + row.remote_id)).length, arrays}, null, 2));
  }
} finally {
  db?.close();
  // Delete only replicas in our freshly created and validated private directory.
  if (path.dirname(directory) !== outputRoot) throw Error('Refuse unsafe cleanup');
  for (const file of fs.readdirSync(directory)) fs.unlinkSync(path.join(directory, file));
  fs.rmdirSync(directory);
  // Leave debug stopped so diagnosis cannot trigger automatic backup rotation.
}
