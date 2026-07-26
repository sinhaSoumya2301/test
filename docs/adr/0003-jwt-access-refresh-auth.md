# ADR-0003: Self-issued JWT with rotating refresh tokens over server-side sessions
Status: Accepted
Date: 2026-07-25

## Context
Auth needs to work across a Vercel-hosted SPA calling a Render-hosted API on a different origin, on free-tier infrastructure with no shared session store readily available (e.g., no free managed Redis in this topology).

## Decision
Issue short-lived (15 min) self-signed JWT access tokens plus a longer-lived refresh token. Refresh tokens are stored server-side only as a hash (never plaintext), are single-use, and rotate on every refresh call; a reused/replayed refresh token revokes the whole session.

## Consequences
- Access-token verification is stateless (signature check only), so it scales without a shared session store — important given the free-tier topology has no natural place to put one.
- Refresh-token hashing means a database compromise doesn't hand out usable credentials directly.
- Rotation-with-reuse-detection gives us a way to notice and kill a stolen refresh token, which a plain long-lived refresh token would not.
- Adds bookkeeping: a `refresh_token` table, a rotation code path, and tests for expiry/reuse/revocation (see SPEC.md → Testing Strategy).
