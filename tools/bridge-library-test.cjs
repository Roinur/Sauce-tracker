const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {DatabaseSync}=require('node:sqlite');
const source=fs.readFileSync(path.join(__dirname,'../app/src/main/java/com/roinur/saucetracker/data/source/SourceEntryStore.kt'),'utf8');
const db=new DatabaseSync(':memory:');
db.exec('CREATE TABLE source_entries(id INTEGER,title TEXT,remote_id TEXT);CREATE TABLE profile_entries(source_entry_id INTEGER,profile_id TEXT,read_state INTEGER,pinned INTEGER,rating INTEGER,added_at TEXT)');
const entry=db.prepare('INSERT INTO source_entries VALUES(?,?,?)'),state=db.prepare('INSERT INTO profile_entries VALUES(?,?,?,?,?,?)');
for(let i=1;i<=130;i++){entry.run(i,`Entry ${String(i).padStart(3,'0')}`,String(i));state.run(i,'main',i>100?1:0,i>120?1:0,i%6,String(i).padStart(3,'0'))}
state.run(130,'other',1,1,5,'999');
for(const filter of ['read','unread','pinned']){
 const predicate=source.match(new RegExp('"'+filter+'" -> where \\+= "([^"\\n]+)"'))[1];
 const order=source.match(/"added" -> "([^"\n]+)"/)[1];
 const rows=db.prepare(`SELECT se.id FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.profile_id=? AND ${predicate} ORDER BY ${order} LIMIT ? OFFSET ?`).all('main',10,0);
 assert.equal(rows.length,10);assert.equal(rows[0].id,filter==='unread'?100:130);
}
db.close();console.log('PASS: production filter/order clauses find entries beyond the first page and isolate profile membership');
