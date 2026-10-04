-- Sauce Tracker 1.9 schema: onCreate from release commit 123d577.
-- Synthetic fixtures only; never point these tests at a user database.
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS entries (
                code INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL,
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                media_id INTEGER NOT NULL DEFAULT 0,
                cover_ext TEXT NOT NULL DEFAULT '',
                rating INTEGER NOT NULL DEFAULT 0,
                read_state INTEGER NOT NULL DEFAULT 0,
                read_at TEXT NOT NULL DEFAULT '',
                pinned INTEGER NOT NULL DEFAULT 0,
                fetched_at TEXT NOT NULL,
                added_at TEXT NOT NULL
            );

CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                pinned INTEGER NOT NULL DEFAULT 0,
                source_url TEXT NOT NULL DEFAULT '',
                UNIQUE(normalized_name, type)
            );

CREATE TABLE IF NOT EXISTS entry_tags (
                entry_code INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY (entry_code, tag_id),
                FOREIGN KEY (entry_code) REFERENCES entries(code) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            );

CREATE TABLE IF NOT EXISTS popular_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                tag_count INTEGER NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0,
                UNIQUE(normalized_name, type)
            );

CREATE TABLE IF NOT EXISTS daily_read_activity (
                activity_date TEXT PRIMARY KEY,
                pages_read INTEGER NOT NULL DEFAULT 0,
                entries_read INTEGER NOT NULL DEFAULT 0
            );

CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at TEXT NOT NULL,
                ended_at TEXT NOT NULL,
                day_key TEXT NOT NULL,
                entry_code INTEGER NOT NULL,
                pages_viewed INTEGER NOT NULL DEFAULT 0,
                seconds_elapsed INTEGER NOT NULL DEFAULT 0,
                rating INTEGER NOT NULL DEFAULT 0,
                is_reread INTEGER NOT NULL DEFAULT 0
            );

CREATE TABLE IF NOT EXISTS entry_heatmap_cache (
                slot_id INTEGER PRIMARY KEY CHECK (slot_id = 1),
                cache_key TEXT NOT NULL DEFAULT '',
                payload_json TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT ''
            );

CREATE TABLE IF NOT EXISTS subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_name TEXT NOT NULL,
                route_type TEXT NOT NULL,
                route_key TEXT NOT NULL,
                notifications_enabled INTEGER NOT NULL DEFAULT 1,
                notification_dot_enabled INTEGER NOT NULL DEFAULT 1,
                initialized INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT '',
                last_checked_at TEXT NOT NULL DEFAULT '',
                UNIQUE(route_key)
            );

CREATE TABLE IF NOT EXISTS subscription_seen_codes (
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                seen_at TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            );

CREATE TABLE IF NOT EXISTS subscription_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                thumbnail_url TEXT NOT NULL DEFAULT '',
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL DEFAULT '',
                discovered_at TEXT NOT NULL DEFAULT '',
                dismissed INTEGER NOT NULL DEFAULT 0,
                pinned INTEGER NOT NULL DEFAULT 0,
                UNIQUE(subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            );

CREATE INDEX IF NOT EXISTS idx_tags_type ON tags(type);

CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);

CREATE INDEX IF NOT EXISTS idx_entry_tags_tag_id ON entry_tags(tag_id);

CREATE INDEX IF NOT EXISTS idx_popular_tags_type ON popular_tags(type);

CREATE INDEX IF NOT EXISTS idx_popular_tags_name ON popular_tags(name);

CREATE INDEX IF NOT EXISTS idx_popular_tags_count ON popular_tags(tag_count);

CREATE INDEX IF NOT EXISTS idx_reading_sessions_day_key ON reading_sessions(day_key);

CREATE INDEX IF NOT EXISTS idx_reading_sessions_entry_code ON reading_sessions(entry_code);

CREATE INDEX IF NOT EXISTS idx_subscriptions_route_type ON subscriptions(route_type);

CREATE INDEX IF NOT EXISTS idx_subscription_events_subscription_id ON subscription_events(subscription_id);

CREATE INDEX IF NOT EXISTS idx_subscription_events_dismissed ON subscription_events(dismissed, pinned, discovered_at);

