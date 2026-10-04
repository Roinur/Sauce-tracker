// Run the real web client against deferred synthetic responses, never a user's phone.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/desktop-bridge/bridge.js'), 'utf8');

class Element {
  constructor(tag = 'div') {
    this.tagName = tag; this.children = []; this.dataset = {}; this.value = ''; this.textContent = '';
    this.style = {setProperty() {}};
    const classes = new Set();
    this.classList = {add: (...xs) => xs.forEach(x => classes.add(x)), remove: (...xs) => xs.forEach(x => classes.delete(x)), contains: x => classes.has(x), toggle: (x, on) => { if (on ?? !classes.has(x)) classes.add(x); else classes.delete(x); }};
  }
  append(...xs) { this.children.push(...xs); }
  replaceChildren(...xs) { this.children = xs; }
  querySelector() { return new Element(); }
  querySelectorAll() { return []; }
  setAttribute() {}
  addEventListener() {}
  scrollIntoView() {}
}
function harness() {
  const elements = new Map(), pending = [], intervals = [], document = {
    hidden: false, body: new Element(), documentElement: new Element(),
    getElementById: id => { if (!elements.has(id)) elements.set(id, new Element()); return elements.get(id); },
    createElement: tag => new Element(tag), querySelectorAll: () => [], addEventListener() {}
  };
  const context = {document, window: {addEventListener() {}}, localStorage: {getItem() {return null}, setItem() {}},
    URLSearchParams, AbortController, TextDecoder, TextEncoder, Uint8Array, Set, Map, Date, console,
    isSecureContext: false, crypto: {randomUUID: () => 'synthetic-session'}, Image: Element,
    requestAnimationFrame: f => f(), setInterval: f => intervals.push(f), setTimeout: () => 0, clearTimeout() {},
    fetch: (url, options) => new Promise((resolve, reject) => pending.push({url, options, resolve: value => resolve({ok: true, json: async () => value}), reject}))
  };
  // Test-only export; the shipped client exposes none of its private state.
  let client = source.replace(/\}check\(\);\r?\n/, '}\n');
  assert.notEqual(client, source, 'disable only the initial automatic connection check');
  const end = client.lastIndexOf('})();');
  client = client.slice(0, end) + 'window.testBridge={state,ui,api,get,openReader,closeReader,privateClear,pageImage,show,mode,move,saveNow,pollDevice};\n' + client.slice(end);
  vm.createContext(context); vm.runInContext(client, context);
  const bridge = context.window.testBridge;
  Object.assign(bridge.state, {unlocked: true, profileId: 'original-profile', profiles: [{id: 'original-profile', name: 'Private name', sources: ['mangadex']}], scope: new Set(['mangadex'])});
  return {bridge, pending, document};
}
const tick = () => new Promise(resolve => setImmediate(resolve));
const entry = {source_id: 'mangadex', remote_id: 'synthetic-series', title: 'Test manga', in_library: true};
const chapters = [{id: 'one', label: 'Chapter 1'}, {id: 'two', label: 'Chapter 2'}];
const manifest = id => ({reader_session_id: id, page_count: 40, start_page_index: 14});

(async () => {
  // Done/Escape must cancel a chapter whose manifest has not arrived yet.
  {
    const {bridge: b, pending} = harness(); b.state.chapters = chapters;
    const launch = b.openReader(entry, chapters[0]); await tick();
    assert.equal(pending.length, 1);
    await b.closeReader(false); pending[0].resolve(manifest('late')); await launch;
    assert.equal(b.state.reader, null); assert(b.ui.reader.classList.contains('hidden'));
  }
  // Responses may arrive in either order. Keep the opening profile, not a later profile.
  {
    const {bridge: b, pending} = harness(); b.state.chapters = chapters;
    const first = b.openReader(entry, chapters[0]); await tick();
    const second = b.openReader(entry, chapters[1]); await tick();
    b.state.profileId = 'different-profile';
    pending[1].resolve(manifest('second')); await second;
    pending[0].resolve(manifest('first')); await first;
    assert.equal(b.state.reader.server, 'second');
    assert.equal(b.state.reader.profileId, 'original-profile');
    assert(pending[1].url.includes('profile_id=original-profile'));
    const oldImage = b.pageImage(0);
    const next = b.openReader(entry, chapters[0], b.state.reader); await tick();
    pending[2].resolve({saved: true}); await tick();
    pending[3].resolve(manifest('third')); await next;
    assert.equal(b.state.reader.profileId, 'original-profile');
    const loading = b.ui.readerLoading.textContent; oldImage.onerror();
    assert.equal(b.ui.readerLoading.textContent, loading, 'old image errors must not cover the new chapter');
  }
  // Final progress must be sent, but its delayed acknowledgment cannot close a newer reader.
  {
    const {bridge: b, pending} = harness(); b.state.chapters = chapters;
    const launch = b.openReader(entry, chapters[0]); await tick(); pending[0].resolve(manifest('one')); await launch;
    const close = b.closeReader(true);
    assert.equal(b.state.reader, null, 'detach immediately, before the progress POST completes');
    const next = b.openReader(entry, chapters[1]); await tick(); pending[2].resolve(manifest('two')); await next;
    pending[1].resolve({saved: true}); await close;
    assert.equal(b.state.reader.server, 'two');
    assert.equal(JSON.parse(pending[1].options.body).profile_id, 'original-profile');
    assert.equal(pending[1].options.keepalive, true);
  }
  // Prefetch and display reuse the same in-memory image, including a loaded neighbor.
  {
    const {bridge: b, pending} = harness(); b.state.chapters = chapters;
    const launch = b.openReader(entry, chapters[0]); await tick(); pending[0].resolve(manifest('cache')); await launch;
    const neighbor = b.pageImage(15); neighbor.onload();
    assert.equal(b.state.reader.viewed.has(15), false, 'prefetch is not a read');
    b.show(15);
    assert.equal(b.ui.readerPages.children[0], neighbor, 'do not re-download a prefetched no-store image');
    assert.equal(b.state.reader.viewed.has(15), true);
    assert(b.ui.readerLoading.classList.contains('hidden'));
    for (let i = 16; i < 35; i++) {b.show(i); assert(b.state.reader.images.size <= 7);}
    b.mode('vertical');
    assert.equal(b.ui.readerPages.children[0].loading, 'lazy', 'distant vertical pages must not all load eagerly');
    assert.equal(b.ui.readerPages.children[34].loading, 'eager');
    b.privateClear(); assert.equal(b.state.reader, null);
  }
  // Clearing privacy state invalidates and aborts in-flight data, even if transport ignores abort.
  {
    const {bridge: b, pending} = harness();
    const load = b.get('/api/v2/library', {profile_id: 'original-profile'});
    const rejected = assert.rejects(load, e => e.name === 'StaleResponse');
    b.state.items = [{title: 'Private'}]; b.ui.search.value = 'Private query';
    b.privateClear('Offline');
    assert.equal(pending[0].options.signal.aborted, true);
    pending[0].resolve({items: [{title: 'Late private data'}]}); await rejected;
    assert.equal(b.state.items.length, 0); assert.equal(b.state.profiles.length, 0);
    assert.equal(b.ui.profileAvatar.textContent, '?'); assert.equal(b.ui.search.value, '');
  }
  {
    const {bridge: b, pending, document} = harness();
    const launch = b.openReader(entry, chapters[0]); await tick();
    document.hidden = true; b.privateClear('Background');
    pending[0].resolve(manifest('private')); await launch;
    assert.equal(b.state.reader, null); assert(b.ui.reader.classList.contains('hidden'));
  }
  // Profile/source/view changes cannot put the previous list into the new view.
  {
    const {bridge: b, pending} = harness();
    const load = b.get('/api/v2/library', {profile_id: 'original-profile'});
    const rejected = assert.rejects(load, e => e.name === 'StaleResponse');
    b.state.profileId = 'new-profile'; pending[0].resolve({items: [{title: 'Old profile'}]}); await rejected;
  }
  // A LAN server disappearing does not necessarily emit the browser's 'offline' event.
  {
    const {bridge: b, pending} = harness(); b.state.items = [{title: 'Private'}];
    const poll = b.pollDevice(); pending[0].reject(new TypeError('Failed to fetch')); await poll;
    assert.equal(b.state.unlocked, false); assert.equal(b.state.items.length, 0);
    assert.equal(b.ui.library.children.length, 0); assert.match(b.ui.status.textContent, /connection unavailable/);
  }
  // Provider failure leaves a dismissible error, never an unhandled rejected click handler.
  {
    const {bridge: b, pending} = harness();
    const launch = b.openReader(entry, chapters[0]); await tick(); pending[0].reject(new Error('Provider unavailable')); await launch;
    assert.equal(b.state.reader, null); assert.equal(b.ui.readerLoading.textContent, 'Provider unavailable');
    await b.closeReader(); assert(b.ui.reader.classList.contains('hidden'));
  }
  console.log('PASS: Bridge reader cancellation, response ordering, original profile, final checkpoint race, bounded page reuse, lazy vertical images, stale image errors, privacy clearing, stale lists, LAN disconnection and provider error handling');
})().catch(e => {console.error(e); process.exitCode = 1;});
