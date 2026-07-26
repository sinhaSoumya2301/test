# ADR-0004: Render + Neon + Vercel as the free-tier hosting topology
Status: Accepted
Date: 2026-07-25

## Context
The requirement is a live, working deployment at zero cost. Candidates considered: Railway (all-in-one, but free tier is a limited trial credit rather than indefinitely free), Fly.io (reduced free allowances), and a split topology of best-in-class free tiers per component.

## Decision
Backend (Docker image) on Render's free web-service tier; PostgreSQL on Neon's free tier; frontend static build on Vercel's free tier.

## Consequences
- Each piece runs on the platform best suited to it for free, rather than compromising on an all-in-one platform's weaker free tier.
- Accepted trade-off: Render's free web service spins down after 15 minutes idle, so the first request after idle incurs a 30–50s JVM cold start. Documented in SPEC.md → Non-Functional Requirements and Deployment Strategy rather than hidden.
- Neon's free tier doesn't expire/pause data (unlike some alternatives), and its branching feature doubles as a pre-migration safety net (see SPEC.md → Rollback Strategy).
- Three separate platforms means three sets of credentials/dashboards to manage, versus one for an all-in-one platform.
