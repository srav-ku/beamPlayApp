# Plans, limits and premium - the design decision

## Short answer

Add the tables now, enforce later. The schema is nearly free to put in place; the *enforcement*
is the real work and needs none of it today.

Honest correction to the worry in your question: retrofitting premium is **not** very hard, because
it is almost entirely **new tables referencing `users`**. What is genuinely expensive to change
later is the shape of data you already collect - e.g. a 5-star rating that should have been 1-10.
So: premium hooks are low-urgency, low-risk, but I have written them now because you asked and
because the cost today is one paste.

## What stays free, and what earns money

Rule: **never paywall the habit.** Playback, resume, continue watching, library, collections,
history, ratings and search must stay free forever - that is what makes people stay. Charge for
things that either cost you money or feel like a superpower:

| Gate | Why it is a fair ask |
|---|---|
| Devices / concurrent streams | It is the one thing that genuinely costs you bandwidth and abuse risk |
| Offline downloads | Real storage and real load |
| AI recommendations (quota) | Every call costs you money |
| Pinned collections (1 vs 3) | Small, harmless, honest pressure |
| Advanced discovery filters | Studio / franchise / decade / country - cinephile-grade tools |
| Year in Review + stats | Emotional payoff, zero marginal cost |
| Trakt sync | For the serious users who will pay |
| Priority requests | Fair queue-jumping, no cost to you |

## Device limits - the part people get wrong

- Table `device_sessions`, one row per (user, device), `last_seen_at` refreshed on every launch.
- A device counts as "in use" if `last_seen_at` is within the last 30 days and `revoked_at` is null.
- If a new device pushes you over `plans.max_devices`, **revoke the least recently used one** and
  tell that user plainly: *"You were signed out on this device because your plan covers N devices."*
- **Never fail mid-playback.** Sign-out happens at next launch, not while someone is watching.
- Concurrent *streams* are a separate counter: if a second play starts while `max_streams` is
  reached, show "Playing on another device - watch here anyway?" rather than a hard error.

## Security, without buying anything

- **Never trust a client flag.** The app can be modified; entitlements are resolved server-side on
  each API call.
- Cache the resolved entitlements per user (in-memory or edge KV, 5-minute TTL). One row read, not
  a join per request.
- Verify payments server-side: Play Billing purchase token -> Google Play Developer API; Stripe and
  Razorpay via signed webhooks. Store every event in `payment_events` keyed by
  `(provider, event_id)` so a retried webhook can never double-grant.
- For offline grace, hand the client a short-lived signed entitlement token (e.g. 7 days) instead of
  trusting local state.
- Rate-limit device registration, or the device limit becomes a joke.

## What it costs

- **Building it: free.** No new services; Play Billing and Stripe/Razorpay are SDKs and webhooks.
- **Selling it: not free.** Google Play 15-30%, Stripe ~2.9% + $0.30, Razorpay ~2%. Play Console is
  a one-time $25. Subscriptions also require the app to be published.
- Because of those cuts, free tier should be generous: you want volume before you want revenue.

## Order of work (unchanged)

1. Finish the free product: accounts, library, history, ratings, discovery, per-episode state
2. Cross-device resume (uses `devices` + `user_playback`, no premium code)
3. Only then wire plans, quotas and the paywall