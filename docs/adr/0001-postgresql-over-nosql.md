# ADR-0001: Use PostgreSQL, not a document store
Status: Accepted
Date: 2026-07-25

## Context
The core operation of this service — a manager approving a shift swap — must atomically update two `shift` rows' ownership, flip the `shift_swap_request` status, and write an `audit_log` row. The domain also has strong referential structure: employee↔manager hierarchy, shift↔employee, and a swap request referencing two employees and up to two shifts. A document database would require re-implementing multi-document transactional consistency and referential integrity in application code.

## Decision
Use PostgreSQL (Neon in production) as the sole datastore, with Flyway-managed versioned migrations, accessed via Spring Data JPA.

## Consequences
- Multi-row consistency for swap approval is a single `@Transactional` method, backed by the database's own ACID guarantees — no custom compensation logic needed.
- Foreign keys enforce the org hierarchy and swap-request relationships at the schema level, not just in application code.
- Schema evolution requires discipline (forward-only migrations, see [ADR-0002-adjacent rollback strategy in SPEC.md](../../SPEC.md#rollback-strategy)) rather than the schema-less flexibility a document store would offer.
- Neon's free tier (durable, no data expiry, branching for pre-migration snapshots) removes what would otherwise be the main practical objection to Postgres for a free-tier deployment.
