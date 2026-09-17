# Activation without an app store

Your constraints: no Play Store, no Stripe, distributed from GitHub or a free store, payment handled
outside the app on a donation platform, activation by you or automatically, time-limited.

## Three activation paths - build the first two, add the third when it hurts

**1. Platform webhook (automatic, free).** Ko-fi, Buy Me a Coffee and Patreon all send webhooks on a
payment, at no cost. The user pays there with the same email they use in the app; the platform POSTs
to one endpoint you own; a small worker matches the email and grants time. This is the honest
"automatic" answer - no store, no card data, no manual step, and it fits "app just redirects you out".

**2. Claim + review (manual fallback).** The app has one screen: *"Already supported us? Enter your
payment reference."* That writes a `activation_claims` row. You approve in the admin panel, which
creates/extends the subscription. Use this when there is no webhook (UPI, bank transfer, gift).

**3. Signed license key (the one that matters for a sideloaded app).** On approval the server issues a
key signed with Ed25519 encoding `{userId, plan, expiresAt}`. The app verifies it **offline** with an
embedded public key. No network needed, survives your server being down, and a revocation list handles
abuse. Store it in `subscriptions.license_key`.

## The rules that keep this from becoming a mess

- **Server decides, always.** The app never holds a boolean that says "premium" that a modified APK
  could flip. Entitlements are resolved server-side, cached ~5 minutes.
- **Every claim unique.** `UNIQUE (platform, reference)` and `UNIQUE (platform, event_id)` mean a
  replayed webhook or a reused reference can never double-grant.
- **Time-limited by default.** `user_entitlements.expires_at` plus a nightly expiry job. Donation
  models drift; explicit expiry keeps it honest and is friendlier than a permanent unlock.
- **Your own account is a flag.** `users.is_owner = 1` (plus `role='admin'`), entitlement resolver
  returns unlimited for it. Do not model yourself as a paying customer.
- **Never touch card data.** You do not have it and should not want it - the platform holds it.

## Remote config is not optional for you

You cannot force an app update through a store. So anything you might want to tune - free quota, number
of devices, which features are paid - must live in `plans` / `plan_features` / `feature_flags` and be
read at runtime. Ship the logic once; change the policy from the admin panel forever after.

## What to gate - my recommendation

| Free | Paid |
|---|---|
| Playback, resume, continue watching, history | - |
| Library, collections, ratings, notes | Pinned collections beyond 1 |
| Search and browse | Advanced filters (studio, franchise, decade, country) |
| Downloads (they cost you nothing if they come from Telegram) | Concurrent streams beyond 1 |
| 3 AI recommendations per day | Unlimited AI recommendations |
| 3 devices | More devices, offline licence key, Trakt sync |
| - | Year in Review and full stats |

Gate **usage**, never the habit. If streaming is metered, meter it (N per week) rather than walling
the catalogue - a walled catalogue kills retention, a meter annoys only the heaviest users.

## One thing I will not help you optimise

Charging money to stream films you do not hold rights to turns infringement into *commercial*
infringement, and it changes who comes after you: payment platforms act on a single report by freezing
the account and the funds, and free app hosts remove the listing. That is a business risk, not a moral
lecture - your income disappears with the account.

The durable version of your own idea: charge for the layer that is actually **yours** - the library,
metadata, collections, sync, discovery, AI and stats (the Cinephile-grade layer we are building).
Streams stay an index of links, like a player plus a library. That is a product people will pay for
without borrowing someone else's trouble.