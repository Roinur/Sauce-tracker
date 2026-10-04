// Run the production Trends SQL against an isolated in-memory database.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {DatabaseSync} = require('node:sqlite');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/roinur/saucetracker/feature/desktopbridge/DesktopBridgeServer.kt'), 'utf8');
const method = source.slice(source.indexOf('private fun bridgeTrends('));
const sql = method.match(/"""(SELECT[\s\S]*?)"""/)[1].replace('$placeholders', '?,?');
const db = new DatabaseSync(':memory:');
db.exec(`CREATE TABLE reading_sessions(profile_id TEXT, source_id TEXT, remote_id TEXT,
  chapter_id TEXT, started_at TEXT, day_key TEXT, pages_viewed INTEGER, seconds_elapsed INTEGER)`);
const insert = db.prepare('INSERT INTO reading_sessions VALUES (?,?,?,?,?,?,?,?)');
insert.run('main','mangadex','series','one','2026-09-04 12:00:00','2026-09-04',15,120);
insert.run('main','mangadex','series','one','2026-09-04 12:05:00','2026-09-04',16,100);
insert.run('main','mangadex','series','two','2026-09-04 12:10:00','2026-09-04',10,80);
insert.run('main','nhentai','123','gallery','2026-09-04 12:15:00','2026-09-04',20,90);
insert.run('other','mangadex','series','three','2026-09-04 12:20:00','2026-09-04',100,999);
const rows = db.prepare(sql).all('main','nhentai','mangadex');
const manga = Object.values(rows.find(row => Object.values(row)[1] === 'mangadex'));
assert.equal(manga[2], 3, 'sessions remain separate');
assert.equal(manga[3], 41, 'pages are summed within the profile');
assert.equal(manga[4], 300, 'reading time is summed within the profile');
assert.equal(manga[5], 2, 'revisiting a chapter must not count it as a new chapter');
assert.equal(rows.length, 2, 'sources remain distinct');
db.close();
console.log('PASS: production Trends SQL keeps source/profile isolation and counts distinct chapters, pages and time');
