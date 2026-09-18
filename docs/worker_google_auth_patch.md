# Worker fix - verify the Google token (replaces `handleGoogleAuth`)

## The hole

`handleGoogleAuth` currently takes `{ email, displayName, firebaseUid }` straight from the request body
and trusts all three (worker lines 1771-1803). Anyone can POST an arbitrary email and a made-up UID and
receive a valid BEAM JWT for that account - no Google account, no secret, no rate limit needed. That is a
full account takeover of any user whose email you can guess.

## The fix, in two parts

1. The client sends the **Firebase ID token**, not a UID.
2. The worker **verifies** it with Google, then takes `email` and `uid` from the *verified claims*.

## Add these two env vars (wrangler / dashboard)

```
FIREBASE_PROJECT_ID      = beam-20a1a
FIREBASE_PROJECT_NUMBER  = 683803915198
```

Both are public identifiers, not secrets - they only pin the audience so a token minted for someone
else's Firebase project cannot be replayed at us.

## Replace `handleGoogleAuth` entirely with this

```js
// POST /auth/google - register/login a Google user.
// Body: { idToken, displayName? }  -- idToken is a Firebase ID token.
async function handleGoogleAuth(request, env) {
    const body = await readBody(request);
    const idToken = body.idToken;
    if (!idToken) return errJson('idToken is required', 400, env);

    // Google validates the signature and expiry for us. We then assert the audience is
    // OUR Firebase project, so a token minted for a different app is rejected here.
    let claims;
    try {
        const res = await fetch('https://oauth2.googleapis.com/tokeninfo?id_token=' + encodeURIComponent(idToken));
        if (!res.ok) return errJson('Google token rejected', 401, env);
        claims = await res.json();
    } catch (_) {
        return errJson('could not verify Google token', 401, env);
    }

    const allowedAud = [cfg(env, 'FIREBASE_PROJECT_ID'), cfg(env, 'FIREBASE_PROJECT_NUMBER')]
        .filter(Boolean)
        .map(String);

    if (allowedAud.length && !allowedAud.includes(String(claims.aud || ''))) {
        return errJson('token audience mismatch', 401, env);
    }
    if (!claims.sub) return errJson('token has no subject', 401, env);
    if (claims.email_verified === 'false') return errJson('email not verified', 401, env);

    // Taken from the VERIFIED token - never from the body.
    const firebaseUid = String(claims.sub);
    const email = String(claims.email || '').trim().toLowerCase();
    if (!email) return errJson('token has no email', 401, env);

    const displayName = String(body.displayName || claims.name || 'User').slice(0, 60);

    let user = await tursoQueryOne('SELECT id, email, role FROM users WHERE email = ?', [email], env);

    if (!user) {
        const { lastInsertRowid: userId } = await tursoRun(
            `INSERT INTO users (email, role, password_hash, name, created_at) VALUES (?, 'user', ?, ?, ?)`,
            [email, `google:${firebaseUid}`, displayName, nowEpoch()], env
        );
        await tursoRun(`INSERT INTO user_settings (user_id) VALUES (?)`, [userId], env);
        user = await tursoQueryOne('SELECT id, email, role FROM users WHERE id = ?', [userId], env);
    } else {
        // keep the display name fresh on every sign-in
        await tursoRun('UPDATE users SET name = ? WHERE id = ?', [displayName, user.id], env);
    }

    const token = await signJWT({
        userId: user.id,
        email: user.email,
        role: user.role,
        iat: nowEpoch(),
        exp: nowEpoch() + cfg(env, 'JWT_TTL_SECONDS'),
    }, cfg(env, 'JWT_SECRET'));

    return json({
        token,
        user: { id: user.id, email: user.email, role: user.role, name: displayName },
    }, 200, env);
}
```

## Two details worth knowing

- **`users.name` exists** because schema v2 added it, so the display name now persists on the profile.
  If you ever roll the schema back, drop that column from the INSERT/UPDATE.
- **Existing Google users keep working**: the same email matches the same row, so no account is
  duplicated or lost by this change. Their `password_hash` stays `google:<uid>` as before.

## Client contract after this change

```json
POST /auth/google
{ "idToken": "<firebase id token>", "displayName": "Sravanth" }
```
The app sends the token; the worker derives everything else. Order of work: deploy this worker change
*after* the app update ships with token sending, or keep both handlers live for one release.