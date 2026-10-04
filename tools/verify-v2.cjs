// Local release-gate regressions. All SQLite databases and Bridge responses are synthetic.
const {spawnSync} = require('node:child_process');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const checks = [
  ['--check', 'app/src/main/assets/desktop-bridge/bridge.js'],
  ['tools/migration-integrity-test.cjs'],
  ['tools/migration-roundtrip-test.cjs'],
  ['tools/source-backup-test.cjs'],
  ['tools/backup-health-test.cjs'],
  ['tools/profile-progress-test.cjs'],
  ['tools/navigation-refresh-test.cjs'],
  ['tools/github-media-copy-test.cjs'],
  ['tools/bridge-data-test.cjs'],
  ['tools/bridge-library-test.cjs'],
  ['tools/bridge-reader-test.cjs'],
  ['tools/bridge-lifecycle-test.cjs']
];
try { require('node:sqlite'); } catch (_) {
  console.error('2.0 regression checks require Node.js 22.13 or newer with node:sqlite support.');
  process.exit(1);
}
for (const args of checks) {
  const result = spawnSync(process.execPath, args, {cwd: root, stdio: 'inherit'});
  if (result.error || result.status !== 0) {
    console.error(`FAILED: ${args.join(' ')}`, result.error?.message || '');
    process.exit(result.status || 1);
  }
}
console.log('PASS: all local 2.0 regressions. Phone/network QA and actual Verified Restore remain separate gates.');
