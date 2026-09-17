# Runbook - applying the final schema

**You run this, in the Turso dashboard. It is the only thing you need to run.**
Nothing here removes data. Audit of `schema_final.sql`: **0 DROP, 0 DELETE, 0 UPDATE,
0 TRUNCATE, 0 INSERT, 0 RENAME.** Only `ALTER TABLE ... ADD COLUMN` and
`CREATE TABLE IF NOT EXISTS`. Adding a column cannot touch the rows already there.

## Before you start

You already have the rollback point: branch **`beambot-backup-2026-09-17`** (forked 15:36 today).
If anything looks wrong afterwards, we restore from that branch. Do **not** try to undo with
`DROP COLUMN` - restore instead.

## Step 1 - open the SQL console

Turso dashboard -> database **`beambot-music`** -> SQL console / editor.

## Step 2 - paste and run, once

Open `docs/schema_final.sql`, select **everything**, paste into the console, run it.

- It is 75 `ALTER TABLE` + 19 `CREATE TABLE` + 21 `CREATE INDEX` = 115 statements.
- Run it **once**. If you run it twice you will get `duplicate column name: ...`. That error is
  harmless and means that column already exists - but if you see it, stop and tell me.

## Step 3 - verify (paste this as-is)

```sql
SELECT COUNT(*) AS tables_total FROM sqlite_master
 WHERE type='table' AND name NOT LIKE 'sqlite_%';

SELECT name FROM sqlite_master WHERE type='table' AND name IN
 ('people','credits','franchises','collections','collection_items','collection_templates',
  'collection_template_items','user_title_data','user_episode_data','devices','follows',
  'activities','user_series_follow','search_cache','provider_cache','api_rate_limits',
  'ai_recommendations','notifications','user_recent_searches') ORDER BY name;

SELECT COUNT(*) AS users_cols    FROM pragma_table_info('users');
SELECT COUNT(*) AS movies_cols   FROM pragma_table_info('movies');
SELECT COUNT(*) AS series_cols   FROM pragma_table_info('series');
SELECT COUNT(*) AS playback_cols FROM pragma_table_info('user_playback');

SELECT 'movies' AS t, COUNT(*) AS c FROM movies
UNION ALL SELECT 'series',         COUNT(*) FROM series
UNION ALL SELECT 'episodes',       COUNT(*) FROM episodes
UNION ALL SELECT 'movie_links',    COUNT(*) FROM movie_links
UNION ALL SELECT 'episode_links',  COUNT(*) FROM episode_links
UNION ALL SELECT 'download_files', COUNT(*) FROM download_files
UNION ALL SELECT 'users',          COUNT(*) FROM users;
```

## Expected results

| Check | Expected |
|---|---|
| `tables_total` | **35** (16 existing + 19 new) |
| new-table list | **19 rows**, exactly the names in the query |
| `users_cols` | **17** (was 6) |
| `movies_cols` | **40** (was 15) |
| `series_cols` | **39** (was 15) |
| `playback_cols` | **16** (was 13) |
| row counts | **movies 328, series 108, episodes 2732, movie_links 1688, episode_links 608, download_files 1705, users 4** |

**If every row count matches, nothing was lost.** If any differs, stop and send me the output.

## Step 4 - send me the result

Paste the output of Step 3 back to me. Then I start on the app: accounts + settings sync,
library and history, ratings, then discovery. Admin panel changes can wait until the app needs them.

## What is deliberately NOT in this file

- No `follows` table exists in your DB today - it is brand new here, so nothing can break.
- No changes to `download_files`, `bot_sessions`, `pending_saves`, `rate_limits`, or the
  `title_lower` indexes the worker relies on.
- No `user_library` or `user_playback` rows are rewritten; they only gain new columns.