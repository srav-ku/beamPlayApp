# Worker - the last three edits (verified against your pasted file)

Your file is 2539 lines. The premium block pasted correctly - `getFlags`,
`getUserEntitlements`, `premiumGate`, `handleUserEntitlements`, `handleAdminSetFlag`,
`handleGetMovieLinksGated`, `handleGetEpisodeLinksGated` are all present.

But **the gate is not wired yet**, and the admin route is missing. Three edits left.

## Edit 1 - add the missing admin route

At line 2413 you have the entitlements route (fine, two array entries on one line is valid JS).
It is missing its partner. Make that region read:

```js
        ['GET', '/auth/me', handleMe, 'auth'],
        ['GET',  '/user/entitlements', handleUserEntitlements, false],
        ['POST', '/admin/flags',       handleAdminSetFlag,      'admin'],
```

Without the second line you cannot flip the model from the admin portal - you would have to run SQL.

## Edit 2 - point the movie link route at the gated handler

Line 2427 currently reads:

```js
        ['GET', '/movies/:id/links', handleGetMovieLinks, false],
```

Change it to:

```js
        ['GET', '/movies/:id/links', handleGetMovieLinksGated, false],
```

## Edit 3 - the same for episodes

Find the `/series/:id/...episodes.../links` entry pointing at `handleGetEpisodeLinks` and change it to
`handleGetEpisodeLinksGated`.

That is all. The two original handlers can stay in the file untouched - nothing calls them after this.

## Why this matters

Right now: the block is pasted, the entitlements endpoint answers, but `/movies/:id/links` still calls
the old handler, so flipping `stream_gate_enabled` would change nothing. After these three edits the
gate is live and still does nothing until you flip the flag - which is exactly what you want.

## Downloads - the honest state of it

You said: stream **and** in-app direct download are premium; only getting the file through Telegram is
free. I checked the worker for that path:

- `handleGetDownloadOptions` (line 1450) reads `download_files` and returns the Telegram options
  (`channel_msg_id`, quality, audio, subtitles). **That is the free path** - leave it alone.
- There is **no in-app direct download endpoint in the worker yet**. The app has no way to pull the
  file itself; everything goes through Telegram today.

So there is nothing to gate yet on that side, and no code will pretend otherwise. When we build the
direct download (worker streams the file to the app instead of sending the user to Telegram), it gets
one line at its top:

```js
    const gate = await premiumGate(user, 'download_full_speed', env);
    if (gate) return gate;
```

Recorded here so it is not forgotten, and so `download_full_speed` is not silently unused.

## After these three edits

Deploy the worker. It will behave **identically** to today - the gate is off, downloads are untouched,
the new endpoint just answers. Then the flag flip happens only after the app version that reads
entitlements ships.