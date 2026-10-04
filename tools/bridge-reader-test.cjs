// Exercise the actual client's checkpoint function without a phone or private data.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/desktop-bridge/bridge.js'), 'utf8');
const checkpoint = source.split('\n').find(line => line.includes('function saveNow(final)')).split('function saveNow(final)')[1];
const calls = [];
const context = {
  state: {profileId: 'other-profile', reader: {
    profileId: 'reader-profile', entry: {source_id: 'mangadex', remote_id: 'series'},
    ch: {id: 'chapter'}, index: 30, count: 31, viewed: new Set([0, 1]),
    activeMs: 4500, start: 0, client: 'stable-session'
  }},
  ui: {status: {}},
  post: async (...args) => { calls.push(args); return {saved: true}; }
};
vm.createContext(context);
vm.runInContext('function saveNow(final)' + checkpoint, context);
(async () => {
  await context.saveNow(false);
  const first = calls[0][1];
  assert.equal(first.profile_id, 'reader-profile');
  assert.equal(first.seconds_elapsed, 5);
  assert.equal(first.pages_viewed, 2);
  assert.equal(first.completed, false, 'an unloaded final page must not complete the chapter');
  context.state.reader.viewed.add(30);
  await context.saveNow(true);
  assert.equal(calls[1][1].completed, true);
  assert.equal(calls[1][1].client_session_id, first.client_session_id);
  assert.equal(calls[1][2].keepalive, true);
  context.state.reader = null;
  await context.saveNow(false);
  assert.equal(calls.length, 2);
  const oldEntry = {source_id: 'mangadex', remote_id: 'old'};
  const newEntry = {source_id: 'mangadex', remote_id: 'new'};
  context.scope = () => 'mangadex';
  context.state.selected = oldEntry;
  context.state.chapters = [];
  let finishRequest;
  context.get = () => new Promise(resolve => { finishRequest = resolve; });
  vm.runInContext(source.split('\n').find(line => line.trimStart().startsWith('async function chapters(e)')), context);
  const pending = context.chapters(oldEntry);
  context.state.selected = newEntry;
  finishRequest({chapters: [{id: 'old-chapter'}], resume_chapter_id: 'old-chapter'});
  const oldResult = await pending;
  assert.equal(oldResult.resumeId, 'old-chapter');
  assert.equal(context.state.chapters.length, 0, 'stale chapters must not replace the newly selected entry');
  console.log('PASS: reader profile, active time, loaded pages, completion, stable checkpoint identity, keepalive and closed-reader handling');
  console.log('PASS: late chapter response cannot overwrite a different selected entry');
})().catch(error => { console.error(error); process.exitCode = 1; });
