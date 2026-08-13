# Suggested entries performance plan

## Objective

Make Suggested entries useful immediately without changing its scoring rules,
filters, duplicate detection accuracy, or recommendation modes.

Targets:

- Show a previously generated list in under 200 ms.
- Show the first useful cold results in roughly 1 to 3 seconds on a healthy
  connection.
- Continue improving and filling the list in the background.
- Never block visible results on duplicate analysis or thumbnail work.

## Current critical path

One refresh currently performs all of this before publishing the list:

1. Exports the complete library to a JSON snapshot.
2. Rebuilds the preference profile from every entry and tag.
3. Loads all duplicate seeds and may read the thumbnail hash archive.
4. Builds roughly a dozen search queries.
5. Fetches up to three pages for every query, which can approach 40 search
   requests before candidate metadata is loaded.
6. Fetches full gallery metadata for every discovered candidate.
7. Scores and sorts the entire candidate pool.
8. Runs duplicate detection for all 30 visible rows.
9. Publishes the first visible result list.

The project already contains an asynchronous duplicate-hint path, but the full
refresh duplicates that work synchronously before showing results.

## Proposed architecture

### Phase 1: measure the stages

Add timings and counters for:

- profile build time
- search request count and duration
- unique candidate count
- metadata cache hits and network fetches
- scoring time
- duplicate analysis time
- time to first visible row and time to final list

Expose these only through the existing performance diagnostics. Do not add
permanent UI noise.

### Phase 2: stale while revalidate

Persist the last successful result set with a fingerprint made from:

- library revision
- blocked-tag revision
- active Search Everything and tag filters
- suggestion mode and weights

Open Suggested entries from that cache immediately. Refresh in the background
when the fingerprint or cache age requires it. Keep the old list visible while
refreshing.

### Phase 3: progressive candidate discovery

Replace the all-at-once query fan-out with stages:

1. Run the best mixed query, best tag query, and best creator query on page 1.
2. Fetch and score candidates until at least 12 useful rows exist.
3. Publish those rows immediately.
4. Request more queries or pages only if the list still needs diversity or
   stronger matches.
5. Fill the overflow queue in the background up to the existing limit.

This keeps the fallback behavior while avoiding dozens of requests when the
first few queries already produce enough candidates.

### Phase 4: durable metadata and profile caches

- Store fetched gallery metadata with a bounded age instead of keeping it only
  in the current ViewModel process.
- Build the local preference profile from direct database rows rather than a
  full backup-style JSON export.
- Rebuild that profile only after rating, read status, entry, or tag changes.
- Rebuild the duplicate seed index only when the library revision changes.

### Phase 5: remove duplicate detection from first paint

Publish scored rows with `duplicateHint = null`, then use the existing
`populateSuggestionDuplicateHintsAsync` path for visible rows. Prioritize rows
currently on screen, then fill the rest and overflow entries during idle time.

### Phase 6: cancellation and resource limits

- Cancel obsolete refreshes when filters or weights change again.
- Stop network expansion when enough high-quality candidates exist.
- Keep bounded network parallelism and separate it from CPU-heavy duplicate
  analysis.
- Bound all persistent caches by count and age.

## Implemented in the 1.8 development cycle

- Added stage timings and counters to the existing performance diagnostics.
- Added fingerprinted stale-while-revalidate result caching.
- Added bounded, 14-day persistent gallery metadata caching.
- Replaced the complete backup-style JSON export with a focused database
  snapshot containing only read or rated entries and their tags.
- Staged search expansion now starts with three page-1 queries and requests
  additional queries/pages only when the candidate target has not been met.
- Candidate metadata loading stops once the bounded scoring pool is full.
- The visible list is capped at 12 rows while extra candidates feed Refresh.
- Duplicate hints run after the visible recommendations have been published.
- Obsolete refresh jobs are cancelled when filters, modes, or weights change.

## Remaining measurement work

The implementation is enabled by default. A before/after performance capture on
the large production library is still required to record device-specific warm
and cold timings and tune the candidate thresholds if necessary.

## Original rollout order

1. Instrument the current pipeline and record a baseline on the large library.
2. Move duplicate hints off the critical path.
3. Add staged search with early publication.
4. Add persisted result and metadata caches.
5. Replace the JSON profile build with revision-aware database aggregation.
6. Compare cold start, warm start, request count, memory, and recommendation
   parity before enabling it by default.

## Safety checks

- The same inputs must preserve score ordering within a small floating-point
  tolerance.
- Blocked tags, current filters, hidden entries, and imported entries must never
  leak through cached results.
- Changing weights must invalidate the correct result cache.
- Cached rows must be rechecked against current imported and hidden code sets.
- Suggestions must remain usable if the network refresh fails.
- No work should continue after the ViewModel is cleared or a refresh becomes
  obsolete.
